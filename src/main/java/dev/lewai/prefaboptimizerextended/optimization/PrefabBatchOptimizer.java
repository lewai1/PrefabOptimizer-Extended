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
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class PrefabBatchOptimizer {
    private static final int[][] NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0},
        {0, 1, 0}, {0, -1, 0},
        {0, 0, 1}, {0, 0, -1}
    };

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
        boolean recursiveFolders,
        @Nonnull AtomicBoolean cancelled,
        @Nonnull BatchProgressListener progressListener
    ) {
        PrefabStore prefabStore = PrefabStore.get();
        PrefabSourceCollection sourceCollection = this.sourceCollector.collect(sourceVirtualPaths, recursiveFolders);
        List<String> savedPaths = Collections.synchronizedList(new ArrayList<>());
        List<String> warnings = Collections.synchronizedList(new ArrayList<>(sourceCollection.warnings()));
        AtomicInteger removedBlocks = new AtomicInteger();
        AtomicInteger processedBlocks = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();

        if (settings.excludedBlocksRegexError() != null) {
            warnings.add("Exclusion regex invalid, fell back to token matching: " + settings.excludedBlocksRegexError());
        }
        if (settings.previewOnly()) {
            warnings.add("Preview mode: no files were written. Uncheck 'Preview only' to apply.");
        }

        Collection<PrefabSource> sources = sourceCollection.sources().values();
        int total = sources.size();
        Path baseDir = settings.previewOnly() ? null : prefabStore.getPrefabsPathForPack(targetPack);
        String batchTimestamp = settings.previewOnly() ? null : PrefabBackupService.newTimestamp();
        Path backupRoot = (baseDir == null || batchTimestamp == null)
            ? null
            : PrefabBackupService.backupRootFor(baseDir, batchTimestamp);

        int reportEvery = Math.max(1, total / 100);

        try {
            this.executor.submit(() ->
                sources.parallelStream().forEach(source -> {
                    if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    try {
                        BlockSelection original = prefabStore.getPrefab(source.path());
                        OptimizedPrefab optimized = this.optimizePrefab(original, settings);
                        String targetKey = buildTargetKey(targetFolder, source.relativeOutputPath());

                        if (!settings.previewOnly()) {
                            saveAtomically(prefabStore, targetPack, baseDir, targetKey, optimized.selection(), backupRoot);
                        }

                        savedPaths.add(targetKey);
                        removedBlocks.addAndGet(optimized.removedBlocks());
                        processedBlocks.addAndGet(optimized.processedBlocks());
                    } catch (Exception | LinkageError e) {
                        warnings.add(source.path().getFileName() + ": " + ThrowableMessages.readableMessage(e));
                    } finally {
                        int done = completed.incrementAndGet();
                        if (done == total || done % reportEvery == 0) {
                            progressListener.onProgress(done, total);
                        }
                    }
                })
            ).join();
        } catch (RuntimeException e) {
            warnings.add("Batch execution error: " + ThrowableMessages.readableMessage(ThrowableMessages.rootCause(e)));
        }

        if (cancelled.get()) {
            warnings.add("Batch cancelled by user. Partial results above reflect the prefabs that completed before the stop.");
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
    BatchHandle optimizeAsync(
        @Nonnull Collection<String> sourceVirtualPaths,
        @Nonnull AssetPack targetPack,
        @Nonnull String targetFolder,
        @Nonnull OptimizerSettings settings,
        boolean recursiveFolders,
        @Nonnull BatchProgressListener progressListener
    ) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        CompletableFuture<PrefabOptimizationResult> future = CompletableFuture.supplyAsync(
            () -> this.optimize(sourceVirtualPaths, targetPack, targetFolder, settings, recursiveFolders, cancelled, progressListener),
            this.executor
        );
        return new BatchHandle(future, cancelled);
    }

    private static void saveAtomically(
        @Nonnull PrefabStore prefabStore,
        @Nonnull AssetPack targetPack,
        @Nonnull Path baseDir,
        @Nonnull String targetKey,
        @Nonnull BlockSelection prefab,
        @Nullable Path backupRoot
    ) throws IOException {
        String tempKey = targetKey + ".tmp." + Long.toHexString(System.nanoTime());
        prefabStore.savePrefabToPack(targetPack, tempKey, prefab, true);

        Path tempPath = baseDir.resolve(tempKey);
        Path finalPath = baseDir.resolve(targetKey);
        Path parent = finalPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (backupRoot != null) {
            try {
                PrefabBackupService.backupIfExists(baseDir, backupRoot, targetKey, finalPath);
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException ignored) {
                }
                throw e;
            }
        }

        try {
            Files.move(tempPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
            throw e;
        }
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
        ReachabilityMap reachableAir = settings.floodFillInterior()
            ? this.computeReachableAir(index, settings, classifierCache)
            : null;

        int[] removedBlocks = {0};
        int[] processedBlocks = {0};

        original.forEachBlock((x, y, z, block) -> {
            if (block.blockId() > 0 && block.filler() == 0) {
                processedBlocks[0]++;
            }
            if (this.isPrefabBlockRemovable(block, x, y, z, settings, fluidNeighborhood, index, classifierCache, reachableAir)) {
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
            long key = PackedPos.pack(x, y, z);
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
            positions.add(PackedPos.pack(x, y, z));
            positions.add(PackedPos.pack(x + 1, y, z));
            positions.add(PackedPos.pack(x - 1, y, z));
            positions.add(PackedPos.pack(x, y + 1, z));
            positions.add(PackedPos.pack(x, y - 1, z));
            positions.add(PackedPos.pack(x, y, z + 1));
            positions.add(PackedPos.pack(x, y, z - 1));
        });
        return positions;
    }

    @Nonnull
    private ReachabilityMap computeReachableAir(
        @Nonnull PrefabBlockIndex index,
        @Nonnull OptimizerSettings settings,
        @Nonnull Int2BooleanMap classifierCache
    ) {
        int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE, zMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE, yMax = Integer.MIN_VALUE, zMax = Integer.MIN_VALUE;
        LongIterator keys = index.blockIds().keySet().iterator();
        while (keys.hasNext()) {
            long key = keys.nextLong();
            int x = PackedPos.unpackX(key), y = PackedPos.unpackY(key), z = PackedPos.unpackZ(key);
            if (x < xMin) xMin = x; if (x > xMax) xMax = x;
            if (y < yMin) yMin = y; if (y > yMax) yMax = y;
            if (z < zMin) zMin = z; if (z > zMax) zMax = z;
        }
        LongIterator fillers = index.nonZeroFillers().iterator();
        while (fillers.hasNext()) {
            long key = fillers.nextLong();
            int x = PackedPos.unpackX(key), y = PackedPos.unpackY(key), z = PackedPos.unpackZ(key);
            if (x < xMin) xMin = x; if (x > xMax) xMax = x;
            if (y < yMin) yMin = y; if (y > yMax) yMax = y;
            if (z < zMin) zMin = z; if (z > zMax) zMax = z;
        }
        if (xMin > xMax) {
            return ReachabilityMap.empty();
        }

        int minX = xMin - 1, maxX = xMax + 1;
        int minY = settings.skipBottomFace() ? yMin : yMin - 1;
        int maxY = yMax + 1;
        int minZ = zMin - 1, maxZ = zMax + 1;

        ReachabilityMap reached = new ReachabilityMap(minX, maxX, minY, maxY, minZ, maxZ);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        // Seed at the top-exterior corner so it's always outside the block mass
        // (important when skipBottomFace tightens minY to yMin where blocks exist).
        long seed = PackedPos.pack(minX, maxY, minZ);
        if (reached.add(minX, maxY, minZ)) {
            queue.enqueue(seed);
        }

        while (!queue.isEmpty()) {
            long key = queue.dequeueLong();
            int x = PackedPos.unpackX(key), y = PackedPos.unpackY(key), z = PackedPos.unpackZ(key);
            for (int[] d : NEIGHBORS) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) {
                    continue;
                }
                if (reached.contains(nx, ny, nz)) {
                    continue;
                }
                if (this.isPrefabOccluder(nx, ny, nz, index, settings, classifierCache)) {
                    continue;
                }
                reached.add(nx, ny, nz);
                queue.enqueue(PackedPos.pack(nx, ny, nz));
            }
        }

        int shell = settings.shellThickness();
        if (shell > 1) {
            expandShell(reached, minX, maxX, minY, maxY, minZ, maxZ, shell - 1);
        }
        return reached;
    }

    private static void expandShell(
        @Nonnull ReachabilityMap reached,
        int minX, int maxX,
        int minY, int maxY,
        int minZ, int maxZ,
        int extraSteps
    ) {
        LongArrayFIFOQueue frontier = collectPositions(reached);

        for (int step = 0; step < extraSteps; step++) {
            LongArrayFIFOQueue nextFrontier = new LongArrayFIFOQueue();
            while (!frontier.isEmpty()) {
                long key = frontier.dequeueLong();
                int x = PackedPos.unpackX(key), y = PackedPos.unpackY(key), z = PackedPos.unpackZ(key);
                for (int[] d : NEIGHBORS) {
                    int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                    if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) {
                        continue;
                    }
                    if (!reached.add(nx, ny, nz)) {
                        continue;
                    }
                    nextFrontier.enqueue(PackedPos.pack(nx, ny, nz));
                }
            }
            frontier = nextFrontier;
            if (frontier.isEmpty()) {
                break;
            }
        }
    }

    @Nonnull
    private static LongArrayFIFOQueue collectPositions(@Nonnull ReachabilityMap reached) {
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        reached.forEachPosition((x, y, z) -> queue.enqueue(PackedPos.pack(x, y, z)));
        return queue;
    }

    private boolean isPrefabOccluder(
        int x, int y, int z,
        @Nonnull PrefabBlockIndex index,
        @Nonnull OptimizerSettings settings,
        @Nonnull Int2BooleanMap classifierCache
    ) {
        long key = PackedPos.pack(x, y, z);
        if (index.nonZeroFillers().contains(key)) {
            return true;
        }
        int blockId = index.blockIds().get(key);
        if (blockId <= 0) {
            return false;
        }
        return this.classify(blockId, settings, classifierCache);
    }

    private boolean isPrefabBlockRemovable(
        @Nonnull BlockSelection.BlockHolder block,
        int x,
        int y,
        int z,
        @Nonnull OptimizerSettings settings,
        @Nonnull Set<Long> fluidNeighborhood,
        @Nonnull PrefabBlockIndex index,
        @Nonnull Int2BooleanMap classifierCache,
        @Nullable ReachabilityMap reachableAir
    ) {
        if (block.filler() != 0 || !this.classify(block.blockId(), settings, classifierCache)) {
            return false;
        }
        if (settings.preserveFluidAdjacentBlocks() && fluidNeighborhood.contains(PackedPos.pack(x, y, z))) {
            return false;
        }

        if (reachableAir != null) {
            for (int[] d : NEIGHBORS) {
                if (reachableAir.contains(x + d[0], y + d[1], z + d[2])) {
                    return false;
                }
            }
            return true;
        }

        boolean bottomOkay = settings.skipBottomFace()
            || this.isNeighborSolid(x, y - 1, z, settings, index, classifierCache);
        return bottomOkay
            && this.isNeighborSolid(x + 1, y, z, settings, index, classifierCache)
            && this.isNeighborSolid(x - 1, y, z, settings, index, classifierCache)
            && this.isNeighborSolid(x, y + 1, z, settings, index, classifierCache)
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
        long key = PackedPos.pack(x, y, z);
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
