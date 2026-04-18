package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import javax.annotation.Nonnull;

@SuppressWarnings({"deprecation", "rawtypes", "removal"})
final class SelectionFloodFill {
    private static final int[][] NEIGHBORS = {
        {1, 0, 0}, {-1, 0, 0},
        {0, 1, 0}, {0, -1, 0},
        {0, 0, 1}, {0, 0, -1}
    };

    private SelectionFloodFill() {
    }

    @Nonnull
    static ReachabilityMap computeReachableAir(
        @Nonnull ChunkAccessor accessor,
        @Nonnull BlockBounds bounds,
        @Nonnull OccludedBlockMask mask,
        @Nonnull OptimizerSettings settings
    ) {
        int xMin = bounds.xMin(), xMax = bounds.xMax();
        int yMin = bounds.yMin(), yMax = bounds.yMax();
        int zMin = bounds.zMin(), zMax = bounds.zMax();
        boolean skipBottom = settings.skipBottomFace();

        ReachabilityMap reached = new ReachabilityMap(xMin, xMax, yMin, yMax, zMin, zMax);
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        seedBoundaryFaces(accessor, mask, xMin, xMax, yMin, yMax, zMin, zMax, skipBottom, reached, queue);

        while (!queue.isEmpty()) {
            long key = queue.dequeueLong();
            int x = PackedPos.unpackX(key), y = PackedPos.unpackY(key), z = PackedPos.unpackZ(key);
            for (int[] d : NEIGHBORS) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                if (nx < xMin || nx > xMax || ny < yMin || ny > yMax || nz < zMin || nz > zMax) {
                    continue;
                }
                if (reached.contains(nx, ny, nz)) {
                    continue;
                }
                if (isOccluder(accessor, nx, ny, nz, mask)) {
                    continue;
                }
                if (reached.add(nx, ny, nz)) {
                    queue.enqueue(PackedPos.pack(nx, ny, nz));
                }
            }
        }

        int shell = settings.shellThickness();
        if (shell > 1) {
            expandShell(reached, xMin, xMax, yMin, yMax, zMin, zMax, shell - 1);
        }
        return reached;
    }

    private static void seedBoundaryFaces(
        @Nonnull ChunkAccessor accessor,
        @Nonnull OccludedBlockMask mask,
        int xMin, int xMax,
        int yMin, int yMax,
        int zMin, int zMax,
        boolean skipBottom,
        @Nonnull ReachabilityMap reached,
        @Nonnull LongArrayFIFOQueue queue
    ) {
        // Z faces
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                trySeed(accessor, mask, x, y, zMin, reached, queue);
                trySeed(accessor, mask, x, y, zMax, reached, queue);
            }
        }
        // Y faces (skip the minY face if skipBottom)
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                if (!skipBottom) {
                    trySeed(accessor, mask, x, yMin, z, reached, queue);
                }
                trySeed(accessor, mask, x, yMax, z, reached, queue);
            }
        }
        // X faces
        for (int y = yMin; y <= yMax; y++) {
            for (int z = zMin; z <= zMax; z++) {
                trySeed(accessor, mask, xMin, y, z, reached, queue);
                trySeed(accessor, mask, xMax, y, z, reached, queue);
            }
        }
    }

    private static void trySeed(
        @Nonnull ChunkAccessor accessor,
        @Nonnull OccludedBlockMask mask,
        int x, int y, int z,
        @Nonnull ReachabilityMap reached,
        @Nonnull LongArrayFIFOQueue queue
    ) {
        if (reached.contains(x, y, z)) {
            return;
        }
        if (isOccluder(accessor, x, y, z, mask)) {
            return;
        }
        if (reached.add(x, y, z)) {
            queue.enqueue(PackedPos.pack(x, y, z));
        }
    }

    private static boolean isOccluder(
        @Nonnull ChunkAccessor accessor,
        int x, int y, int z,
        @Nonnull OccludedBlockMask mask
    ) {
        BlockAccessor chunk = (BlockAccessor) accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null) {
            return false;
        }
        if (chunk.getFiller(x, y, z) != 0) {
            return true;
        }
        int blockId = chunk.getBlock(x, y, z);
        if (blockId <= 0) {
            return false;
        }
        return mask.classify(blockId);
    }

    private static void expandShell(
        @Nonnull ReachabilityMap reached,
        int xMin, int xMax,
        int yMin, int yMax,
        int zMin, int zMax,
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
                    if (nx < xMin || nx > xMax || ny < yMin || ny > yMax || nz < zMin || nz > zMax) {
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
}
