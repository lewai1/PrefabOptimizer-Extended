package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockFilter;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import javax.annotation.Nonnull;

@SuppressWarnings({"deprecation", "rawtypes", "removal"})
public final class OccludedBlockMask extends BlockMask {
    private static final int[][] NEIGHBORS = {
        {1, 0, 0},
        {-1, 0, 0},
        {0, 1, 0},
        {0, -1, 0},
        {0, 0, 1},
        {0, 0, -1}
    };

    private final BlockClassifier classifier;
    private final OptimizerSettings settings;

    public OccludedBlockMask(@Nonnull BlockClassifier classifier, @Nonnull OptimizerSettings settings) {
        super(BlockFilter.EMPTY_ARRAY);
        this.classifier = classifier;
        this.settings = settings;
    }

    @Override
    public boolean isExcluded(
        @Nonnull ChunkAccessor accessor,
        int x,
        int y,
        int z,
        com.hypixel.hytale.math.vector.Vector3i min,
        com.hypixel.hytale.math.vector.Vector3i max,
        int blockId,
        int fluidId
    ) {
        return !this.isRemovable(accessor, x, y, z, toBounds(min, max), blockId);
    }

    public boolean isExcluded(
        @Nonnull ChunkAccessor accessor,
        int x,
        int y,
        int z,
        org.joml.Vector3i min,
        org.joml.Vector3i max,
        int blockId,
        int fluidId
    ) {
        return !this.isRemovable(accessor, x, y, z, toBounds(min, max), blockId);
    }

    public boolean isRemovable(
        @Nonnull ChunkAccessor accessor,
        int x,
        int y,
        int z,
        @Nonnull BlockBounds bounds,
        int blockId
    ) {
        if (!this.classifier.isOptimizableFullCube(blockId, this.settings)) {
            return false;
        }

        for (int[] neighbor : NEIGHBORS) {
            int nx = x + neighbor[0];
            int ny = y + neighbor[1];
            int nz = z + neighbor[2];
            if (!bounds.contains(nx, ny, nz) || !this.isSolidNeighbor(accessor, nx, ny, nz)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSolidNeighbor(@Nonnull ChunkAccessor accessor, int x, int y, int z) {
        BlockAccessor chunk = (BlockAccessor) accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null || chunk.getFiller(x, y, z) != 0) {
            return false;
        }
        return this.classifier.isOptimizableFullCube(chunk.getBlock(x, y, z), this.settings);
    }

    private static BlockBounds toBounds(
        @Nonnull com.hypixel.hytale.math.vector.Vector3i min,
        @Nonnull com.hypixel.hytale.math.vector.Vector3i max
    ) {
        return BlockBounds.of(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    private static BlockBounds toBounds(@Nonnull org.joml.Vector3i min, @Nonnull org.joml.Vector3i max) {
        return BlockBounds.of(min.x, min.y, min.z, max.x, max.y, max.z);
    }
}
