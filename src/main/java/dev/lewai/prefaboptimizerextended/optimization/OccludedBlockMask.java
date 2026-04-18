package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockFilter;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
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
    private final Int2BooleanMap classifierCache = new Int2BooleanOpenHashMap();

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
        if (!this.classify(blockId)) {
            return false;
        }

        boolean preserveFluidAdjacent = this.settings.preserveFluidAdjacentBlocks();
        for (int[] neighbor : NEIGHBORS) {
            int nx = x + neighbor[0];
            int ny = y + neighbor[1];
            int nz = z + neighbor[2];
            if (!bounds.contains(nx, ny, nz)) {
                return false;
            }
            BlockAccessor chunk = (BlockAccessor) accessor.getChunk(ChunkUtil.indexChunkFromBlock(nx, nz));
            if (chunk == null || chunk.getFiller(nx, ny, nz) != 0) {
                return false;
            }
            if (preserveFluidAdjacent && chunk.getFluidId(nx, ny, nz) != 0) {
                return false;
            }
            if (!this.classify(chunk.getBlock(nx, ny, nz))) {
                return false;
            }
        }
        return true;
    }

    private boolean classify(int blockId) {
        synchronized (this.classifierCache) {
            if (this.classifierCache.containsKey(blockId)) {
                return this.classifierCache.get(blockId);
            }
            boolean result = this.classifier.isOptimizableFullCube(blockId, this.settings);
            this.classifierCache.put(blockId, result);
            return result;
        }
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
