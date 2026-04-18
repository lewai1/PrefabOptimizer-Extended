package dev.lewai.prefaboptimizerextended.optimization;

import javax.annotation.Nonnull;

import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class BlockClassifier {
    public boolean isOptimizableFullCube(int blockId, @Nonnull OptimizerSettings settings) {
        if (blockId <= 0) {
            return false;
        }

        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null || blockType.isUnknown()) {
            return false;
        }
        if (settings.excludes(blockType.getId())) {
            return false;
        }
        if (blockType.getMaterial() != BlockMaterial.Solid) {
            return false;
        }
        if (!isAllowedDrawType(blockType.getDrawType(), settings.strictCubeOnly())) {
            return false;
        }
        return !settings.preserveTransparentBlocks() || blockType.getOpacity() == Opacity.Solid;
    }

    private static boolean isAllowedDrawType(@Nonnull DrawType drawType, boolean strictCubeOnly) {
        if (strictCubeOnly) {
            return drawType == DrawType.Cube;
        }
        return drawType == DrawType.Cube || drawType == DrawType.CubeWithModel;
    }
}
