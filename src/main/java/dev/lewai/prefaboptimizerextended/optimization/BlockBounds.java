package dev.lewai.prefaboptimizerextended.optimization;

public record BlockBounds(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
    public static BlockBounds of(int ax, int ay, int az, int bx, int by, int bz) {
        return new BlockBounds(
            Math.min(ax, bx),
            Math.min(ay, by),
            Math.min(az, bz),
            Math.max(ax, bx),
            Math.max(ay, by),
            Math.max(az, bz)
        );
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.xMin && x <= this.xMax
            && y >= this.yMin && y <= this.yMax
            && z >= this.zMin && z <= this.zMax;
    }

    public int width() {
        return this.xMax - this.xMin;
    }

    public int depth() {
        return this.zMax - this.zMin;
    }

    public int centerX() {
        return this.xMin + this.width() / 2;
    }

    public int centerZ() {
        return this.zMin + this.depth() / 2;
    }
}
