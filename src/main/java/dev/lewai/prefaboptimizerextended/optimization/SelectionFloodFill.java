package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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
    static LongSet computeReachableAir(
        @Nonnull ChunkAccessor accessor,
        @Nonnull BlockBounds bounds,
        @Nonnull OccludedBlockMask mask
    ) {
        int xMin = bounds.xMin(), xMax = bounds.xMax();
        int yMin = bounds.yMin(), yMax = bounds.yMax();
        int zMin = bounds.zMin(), zMax = bounds.zMax();

        LongOpenHashSet reached = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        seedBoundaryFaces(accessor, mask, xMin, xMax, yMin, yMax, zMin, zMax, reached, queue);

        while (!queue.isEmpty()) {
            long key = queue.dequeueLong();
            int x = unpackX(key), y = unpackY(key), z = unpackZ(key);
            for (int[] d : NEIGHBORS) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                if (nx < xMin || nx > xMax || ny < yMin || ny > yMax || nz < zMin || nz > zMax) {
                    continue;
                }
                long nkey = packPos(nx, ny, nz);
                if (reached.contains(nkey)) {
                    continue;
                }
                if (isOccluder(accessor, nx, ny, nz, mask)) {
                    continue;
                }
                reached.add(nkey);
                queue.enqueue(nkey);
            }
        }
        return reached;
    }

    private static void seedBoundaryFaces(
        @Nonnull ChunkAccessor accessor,
        @Nonnull OccludedBlockMask mask,
        int xMin, int xMax,
        int yMin, int yMax,
        int zMin, int zMax,
        @Nonnull LongOpenHashSet reached,
        @Nonnull LongArrayFIFOQueue queue
    ) {
        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                trySeed(accessor, mask, x, y, zMin, reached, queue);
                trySeed(accessor, mask, x, y, zMax, reached, queue);
            }
        }
        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                trySeed(accessor, mask, x, yMin, z, reached, queue);
                trySeed(accessor, mask, x, yMax, z, reached, queue);
            }
        }
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
        @Nonnull LongOpenHashSet reached,
        @Nonnull LongArrayFIFOQueue queue
    ) {
        long key = packPos(x, y, z);
        if (reached.contains(key)) {
            return;
        }
        if (isOccluder(accessor, x, y, z, mask)) {
            return;
        }
        reached.add(key);
        queue.enqueue(key);
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

    static long packPos(int x, int y, int z) {
        long lx = ((long) x) & 0x1FFFFFL;
        long ly = ((long) y) & 0x1FFFFFL;
        long lz = ((long) z) & 0x1FFFFFL;
        return (lx << 42) | (ly << 21) | lz;
    }

    private static int unpackX(long key) {
        return (int) ((key << 1) >> 43);
    }

    private static int unpackY(long key) {
        return (int) ((key << 22) >> 43);
    }

    private static int unpackZ(long key) {
        return (int) ((key << 43) >> 43);
    }
}
