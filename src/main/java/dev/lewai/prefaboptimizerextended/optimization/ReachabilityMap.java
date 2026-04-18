package dev.lewai.prefaboptimizerextended.optimization;

import java.util.BitSet;
import javax.annotation.Nonnull;

/**
 * Bounds-relative bitmap that stores one bit per position inside a known
 * volume. Uses O(V bits) memory where V is the volume of the bounding box,
 * which is orders of magnitude smaller than a LongOpenHashSet for dense
 * fills on large prefabs.
 *
 * Positions outside the bounds are treated as "not contained" and ignored
 * by add().
 */
final class ReachabilityMap {
    private final int xMin;
    private final int yMin;
    private final int zMin;
    private final int dx;
    private final int dy;
    private final int dz;
    private final BitSet bits;

    ReachabilityMap(int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.zMin = zMin;
        this.dx = xMax - xMin + 1;
        this.dy = yMax - yMin + 1;
        this.dz = zMax - zMin + 1;
        if (this.dx <= 0 || this.dy <= 0 || this.dz <= 0) {
            throw new IllegalArgumentException("Invalid bounds");
        }
        this.bits = new BitSet(this.dx * this.dy * this.dz);
    }

    @Nonnull
    static ReachabilityMap empty() {
        return new ReachabilityMap(0, 0, 0, 0, 0, 0);
    }

    boolean contains(int x, int y, int z) {
        int i = this.index(x, y, z);
        return i >= 0 && this.bits.get(i);
    }

    boolean add(int x, int y, int z) {
        int i = this.index(x, y, z);
        if (i < 0 || this.bits.get(i)) {
            return false;
        }
        this.bits.set(i);
        return true;
    }

    void forEachPosition(@Nonnull PositionConsumer consumer) {
        for (int i = this.bits.nextSetBit(0); i >= 0; i = this.bits.nextSetBit(i + 1)) {
            int iz = i % this.dz;
            int iy = (i / this.dz) % this.dy;
            int ix = i / this.dz / this.dy;
            consumer.accept(this.xMin + ix, this.yMin + iy, this.zMin + iz);
        }
    }

    @FunctionalInterface
    interface PositionConsumer {
        void accept(int x, int y, int z);
    }

    private int index(int x, int y, int z) {
        int ix = x - this.xMin;
        int iy = y - this.yMin;
        int iz = z - this.zMin;
        if (ix < 0 || ix >= this.dx || iy < 0 || iy >= this.dy || iz < 0 || iz >= this.dz) {
            return -1;
        }
        return (ix * this.dy + iy) * this.dz + iz;
    }
}
