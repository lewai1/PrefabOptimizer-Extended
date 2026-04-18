package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import dev.lewai.prefaboptimizerextended.util.ThrowableMessages;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

final class PrefabBatchOptimizer {
    private final BlockClassifier classifier;
    private final PrefabSourceCollector sourceCollector;
    private final Executor executor;

    PrefabBatchOptimizer(
        @Nonnull BlockClassifier classifier,
        @Nonnull PrefabSourceCollector sourceCollector,
        @Nonnull Executor executor
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
        List<String> savedPaths = new ArrayList<>();
        List<String> warnings = new ArrayList<>(sourceCollection.warnings());
        int removedBlocks = 0;
        int processedBlocks = 0;

        for (PrefabSource source : sourceCollection.sources().values()) {
            try {
                BlockSelection original = prefabStore.getPrefab(source.path());
                OptimizedPrefab optimized = this.optimizePrefab(original, settings);
                String targetKey = buildTargetKey(targetFolder, source.relativeOutputPath());
                prefabStore.savePrefabToPack(targetPack, targetKey, optimized.selection(), true);
                savedPaths.add(targetKey);
                removedBlocks += optimized.removedBlocks();
                processedBlocks += optimized.processedBlocks();
            } catch (Exception | LinkageError e) {
                warnings.add(source.path().getFileName() + ": " + ThrowableMessages.readableMessage(e));
            }
        }

        return new PrefabOptimizationResult(
            sourceCollection.sources().size(),
            savedPaths.size(),
            removedBlocks,
            processedBlocks,
            savedPaths,
            warnings
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
        AtomicInteger removedBlocks = new AtomicInteger();
        AtomicInteger processedBlocks = new AtomicInteger();
        optimized.copyPropertiesFrom(original);

        Set<Long> fluidNeighborhood = settings.preserveFluidAdjacentBlocks()
            ? collectFluidNeighborhood(original)
            : Set.of();

        original.forEachBlock((x, y, z, block) -> {
            if (block.blockId() > 0 && block.filler() == 0) {
                processedBlocks.incrementAndGet();
            }
            if (this.isPrefabBlockRemovable(original, block, x, y, z, settings, fluidNeighborhood)) {
                removedBlocks.incrementAndGet();
                return;
            }

            Holder<ChunkStore> holder = block.holder() == null ? null : block.holder().clone();
            optimized.addBlockAtLocalPos(x, y, z, block.blockId(), block.rotation(), block.filler(), block.supportValue(), holder);
        });
        original.forEachFluid(optimized::addFluidAtLocalPos);
        original.forEachTint(optimized::addTintAtLocalPos);
        original.forEachEntity(holder -> optimized.addEntityHolderRaw(holder.clone()));

        return new OptimizedPrefab(optimized, removedBlocks.get(), processedBlocks.get());
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
        @Nonnull BlockSelection selection,
        @Nonnull BlockSelection.BlockHolder block,
        int x,
        int y,
        int z,
        @Nonnull OptimizerSettings settings,
        @Nonnull Set<Long> fluidNeighborhood
    ) {
        if (block.filler() != 0 || !this.classifier.isOptimizableFullCube(block.blockId(), settings)) {
            return false;
        }
        if (settings.preserveFluidAdjacentBlocks() && fluidNeighborhood.contains(packPos(x, y, z))) {
            return false;
        }

        return this.isPrefabNeighborSolid(selection, x + 1, y, z, settings)
            && this.isPrefabNeighborSolid(selection, x - 1, y, z, settings)
            && this.isPrefabNeighborSolid(selection, x, y + 1, z, settings)
            && this.isPrefabNeighborSolid(selection, x, y - 1, z, settings)
            && this.isPrefabNeighborSolid(selection, x, y, z + 1, settings)
            && this.isPrefabNeighborSolid(selection, x, y, z - 1, settings);
    }

    private boolean isPrefabNeighborSolid(
        @Nonnull BlockSelection selection,
        int x,
        int y,
        int z,
        @Nonnull OptimizerSettings settings
    ) {
        BlockSelection.BlockHolder block = selection.getBlockHolderAtWorldPos(
            selection.getX() + x,
            selection.getY() + y,
            selection.getZ() + z
        );
        return block != null
            && block.filler() == 0
            && this.classifier.isOptimizableFullCube(block.blockId(), settings);
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
}
