package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.lewai.prefaboptimizerextended.util.ThrowableMessages;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

final class PrefabBatchOptimizer {
    private final BlockClassifier classifier;
    private final PrefabSourceCollector sourceCollector;
    private final ForkJoinPool executor;

    PrefabBatchOptimizer(
        @Nonnull BlockClassifier classifier,
        @Nonnull PrefabSourceCollector sourceCollector,
        @Nonnull ForkJoinPool executor
    ) {
        this.classifier = classifier;
        this.sourceCollector = sourceCollector;
        this.executor = executor;
    }

    @Nonnull
    PrefabOptimizationResult optimize(
        @Nonnull Collection<String> sourceVirtualPaths,
        @Nonnull AssetPack targetPack,
        @Nonnull String targetFolder,
        @Nonnull OptimizerSettings settings,
        boolean recursiveFolders
    ) {
        PrefabStore prefabStore = PrefabStore.get();
        PrefabSourceCollection sourceCollection = this.sourceCollector.collect(sourceVirtualPaths, recursiveFolders);
        List<String> savedPaths = Collections.synchronizedList(new ArrayList<>());
        List<String> warnings = Collections.synchronizedList(new ArrayList<>(sourceCollection.warnings()));
        AtomicInteger removedBlocks = new AtomicInteger();
        AtomicInteger processedBlocks = new AtomicInteger();

        if (settings.excludedBlocksRegexError() != null) {
            warnings.add("Exclusion regex invalid, fell back to token matching: " + settings.excludedBlocksRegexError());
        }

        Collection<PrefabSource> sources = sourceCollection.sources().values();
        try {
            this.executor.submit(() ->
                sources.parallelStream().forEach(source -> {
                    try {
                        BlockSelection original = prefabStore.getPrefab(source.path());
                        OptimizedPrefab optimized = this.optimizePrefab(original, settings);
                        String targetKey = buildTargetKey(targetFolder, source.relativeOutputPath());
                        prefabStore.savePrefabToPack(targetPack, targetKey, optimized.selection(), true);
                        savedPaths.add(targetKey);
                        removedBlocks.addAndGet(optimized.removedBlocks());
                        processedBlocks.addAndGet(optimized.processedBlocks());
                    } catch (Exception | LinkageError e) {
                        warnings.add(source.path().getFileName() + ": " + ThrowableMessages.readableMessage(e));
                    }
                })
            ).join();
        } catch (RuntimeException e) {
            warnings.add("Batch execution error: " + ThrowableMessages.readableMessage(ThrowableMessages.rootCause(e)));
        }

        return new PrefabOptimizationResult(
            sourceCollection.sources().size(),
            savedPaths.size(),
            removedBlocks.get(),
            processedBlocks.get(),
            List.copyOf(savedPaths),
            List.copyOf(warnings)
        );
    }

    @Nonnull
    CompletableFuture<PrefabOptimizationResult> optimizeAsync(
        @Nonnull Collection<String> sourceVirtualPaths,
        @Nonnull AssetPack targetPack,
        @Nonnull String targetFolder,
        @Nonnull OptimizerSettings settings,
        boolean recursiveFolders
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.optimize(sourceVirtualPaths, targetPack, targetFolder, settings, recursiveFolders),
            this.executor
        );
    }

    @Nonnull
    private OptimizedPrefab optimizePrefab(@Nonnull BlockSelection original, @Nonnull OptimizerSettings settings) {
        BlockSelection optimized = new BlockSelection(original.getBlockCount(), original.getEntityCount());
        optimized.copyPropertiesFrom(original);

        PrefabBlockIndex index = buildBlockIndex(original);
        Set<Long> fluidNeighborhood = settings.preserveFluidAdjacentBlocks()
            ? collectFluidNeighborhood(original)
            : Set.of();
        Int2BooleanMap classifierCache = new Int2BooleanOpenHashMap();

        int[] removedBlocks = {0};
        int[] processedBlocks = {0};

        original.forEachBlock((x, y, z, block) -> {
            if (block.blockId() > 0 && block.filler() == 0) {
                processedBlocks[0]++;
            }
            if (this.isPrefabBlockRemovable(block, x, y, z, settings, fluidNeighborhood, index, classifierCache)) {
                removedBlocks[0]++;
                return;
            }
            Holder<ChunkStore> holder = block.holder() == null ? null : block.holder().clone();
            optimized.addBlockAtLocalPos(x, y, z, block.blockId(), block.rotation(), block.filler(), block.supportValue(), holder);
        });
        original.forEachFluid(optimized::addFluidAtLocalPos);
        original.forEachTint(optimized::addTintAtLocalPos);
        original.forEachEntity(holder -> optimized.addEntityHolderRaw(holder.clone()));

        return new OptimizedPrefab(optimized, removedBlocks[0], processedBlocks[0]);
    }

    @Nonnull
    private static PrefabBlockIndex buildBlockIndex(@Nonnull BlockSelection selection) {
        Long2IntOpenHashMap blockIds = new Long2IntOpenHashMap(selection.getBlockCount());
        blockIds.defaultReturnValue(0);
        LongOpenHashSet nonZeroFillers = new LongOpenHashSet();
        selection.forEachBlock((x, y, z, block) -> {
            long key = packPos(x, y, z);
            if (block.filler() != 0) {
                nonZeroFillers.add(key);
            } else if (block.blockId() > 0) {
                blockIds.put(key, block.blockId());
            }
        });
        return new PrefabBlockIndex(blockIds, nonZeroFillers);
    }

    @Nonnull
    private static Set<Long> collectFluidNeighborhood(@Nonnull BlockSelection selection) {
        Set<Long> positions = new HashSet<>();
        selection.forEachFluid((x, y, z, fluidId, fluidLevel) -> {
            positions.add(packPos(x, y, z));
            positions.add(packPos(x + 1, y, z));
            positions.add(packPos(x - 1, y, z));
            positions.add(packPos(x, y + 1, z));
            positions.add(packPos(x, y - 1, z));
            positions.add(packPos(x, y, z + 1));
            positions.add(packPos(x, y, z - 1));
        });
        return positions;
    }

    private boolean isPrefabBlockRemovable(
        @Nonnull BlockSelection.BlockHolder block,
        int x,
        int y,
        int z,
        @Nonnull OptimizerSettings settings,
        @Nonnull Set<Long> fluidNeighborhood,
        @Nonnull PrefabBlockIndex index,
        @Nonnull Int2BooleanMap classifierCache
    ) {
        if (block.filler() != 0 || !this.classify(block.blockId(), settings, classifierCache)) {
            return false;
        }
        if (settings.preserveFluidAdjacentBlocks() && fluidNeighborhood.contains(packPos(x, y, z))) {
            return false;
        }

        return this.isNeighborSolid(x + 1, y, z, settings, index, classifierCache)
            && this.isNeighborSolid(x - 1, y, z, settings, index, classifierCache)
            && this.isNeighborSolid(x, y + 1, z, settings, index, classifierCache)
            && this.isNeighborSolid(x, y - 1, z, settings, index, classifierCache)
            && this.isNeighborSolid(x, y, z + 1, settings, index, classifierCache)
            && this.isNeighborSolid(x, y, z - 1, settings, index, classifierCache);
    }

    private boolean isNeighborSolid(
        int x,
        int y,
        int z,
        @Nonnull OptimizerSettings settings,
        @Nonnull PrefabBlockIndex index,
        @Nonnull Int2BooleanMap classifierCache
    ) {
        long key = packPos(x, y, z);
        if (index.nonZeroFillers().contains(key)) {
            return false;
        }
        int blockId = index.blockIds().get(key);
        if (blockId <= 0) {
            return false;
        }
        return this.classify(blockId, settings, classifierCache);
    }

    private boolean classify(int blockId, @Nonnull OptimizerSettings settings, @Nonnull Int2BooleanMap cache) {
        if (cache.containsKey(blockId)) {
            return cache.get(blockId);
        }
        boolean result = this.classifier.isOptimizableFullCube(blockId, settings);
        cache.put(blockId, result);
        return result;
    }

    private static long packPos(int x, int y, int z) {
        long lx = ((long) x) & 0x1FFFFFL;
        long ly = ((long) y) & 0x1FFFFFL;
        long lz = ((long) z) & 0x1FFFFFL;
        return (lx << 42) | (ly << 21) | lz;
    }

    @Nonnull
    private static String buildTargetKey(@Nonnull String targetFolder, @Nonnull Path relativePath) {
        String folder = targetFolder.trim().replace('\\', '/');
        while (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        while (folder.endsWith("/")) {
            folder = folder.substring(0, folder.length() - 1);
        }
        String relative = relativePath.toString().replace('\\', '/');
        return folder.isBlank() ? relative : folder + "/" + relative;
    }

    private record OptimizedPrefab(@Nonnull BlockSelection selection, int removedBlocks, int processedBlocks) {
    }

    private record PrefabBlockIndex(@Nonnull Long2IntMap blockIds, @Nonnull LongSet nonZeroFillers) {
    }
}
