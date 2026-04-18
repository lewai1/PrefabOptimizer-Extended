package dev.lewai.prefaboptimizerextended.optimization;

/**
 * Packs a signed 3D voxel position (21 bits per axis) into a single long, used
 * as a hash key for neighbor-lookup maps and BFS queues. Each axis is
 * constrained to the inclusive range [-1_048_576, 1_048_575], which comfortably
 * covers any reasonable prefab or selection bounds.
 *
 * Kept free of Hytale API dependencies so test code can link against it.
 */
final class PackedPos {
    private static final long MASK_21 = 0x1FFFFFL;

    private PackedPos() {
    }

    static long pack(int x, int y, int z) {
        long lx = ((long) x) & MASK_21;
        long ly = ((long) y) & MASK_21;
        long lz = ((long) z) & MASK_21;
        return (lx << 42) | (ly << 21) | lz;
    }

    static int unpackX(long key) {
        return (int) ((key << 1) >> 43);
    }

    static int unpackY(long key) {
        return (int) ((key << 22) >> 43);
    }

    static int unpackZ(long key) {
        return (int) ((key << 43) >> 43);
    }
}
