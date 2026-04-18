package dev.lewai.prefaboptimizerextended.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.lewai.prefaboptimizerextended.optimization.OptimizerSettings;

public final class SelectionEventData {
    static final String KEY_ACTION = "Action";
    static final String KEY_PRESERVE_TRANSPARENT = "@PreserveTransparent";
    static final String KEY_STRICT_CUBE_ONLY = "@StrictCubeOnly";
    static final String KEY_PRESERVE_FLUID_ADJACENT = "@PreserveFluidAdjacent";
    static final String KEY_FLOOD_FILL_INTERIOR = "@FloodFillInterior";
    static final String KEY_SHELL_THICKNESS = "@ShellThickness";
    static final String KEY_SKIP_BOTTOM_FACE = "@SkipBottomFace";
    static final String KEY_PREVIEW_ONLY = "@PreviewOnly";
    static final String KEY_EXCLUDED_BLOCKS = "@ExcludedBlocks";

    public String action;
    public Boolean preserveTransparent;
    public Boolean strictCubeOnly;
    public Boolean preserveFluidAdjacent;
    public Boolean floodFillInterior;
    public String shellThickness;
    public Boolean skipBottomFace;
    public Boolean previewOnly;
    public String excludedBlocks;

    public OptimizerSettings toSettings() {
        return OptimizerSettings.create(
            this.preserveTransparent,
            this.strictCubeOnly,
            this.preserveFluidAdjacent,
            this.floodFillInterior,
            this.shellThickness,
            this.skipBottomFace,
            this.previewOnly,
            this.excludedBlocks
        );
    }

    public static final BuilderCodec<SelectionEventData> CODEC = BuilderCodec.<SelectionEventData>builder(SelectionEventData.class, SelectionEventData::new)
        .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
        .append(new KeyedCodec<>(KEY_PRESERVE_TRANSPARENT, Codec.BOOLEAN), (data, value) -> data.preserveTransparent = value, data -> data.preserveTransparent).add()
        .append(new KeyedCodec<>(KEY_STRICT_CUBE_ONLY, Codec.BOOLEAN), (data, value) -> data.strictCubeOnly = value, data -> data.strictCubeOnly).add()
        .append(new KeyedCodec<>(KEY_PRESERVE_FLUID_ADJACENT, Codec.BOOLEAN), (data, value) -> data.preserveFluidAdjacent = value, data -> data.preserveFluidAdjacent).add()
        .append(new KeyedCodec<>(KEY_FLOOD_FILL_INTERIOR, Codec.BOOLEAN), (data, value) -> data.floodFillInterior = value, data -> data.floodFillInterior).add()
        .append(new KeyedCodec<>(KEY_SHELL_THICKNESS, Codec.STRING), (data, value) -> data.shellThickness = value, data -> data.shellThickness).add()
        .append(new KeyedCodec<>(KEY_SKIP_BOTTOM_FACE, Codec.BOOLEAN), (data, value) -> data.skipBottomFace = value, data -> data.skipBottomFace).add()
        .append(new KeyedCodec<>(KEY_PREVIEW_ONLY, Codec.BOOLEAN), (data, value) -> data.previewOnly = value, data -> data.previewOnly).add()
        .append(new KeyedCodec<>(KEY_EXCLUDED_BLOCKS, Codec.STRING), (data, value) -> data.excludedBlocks = value, data -> data.excludedBlocks).add()
        .build();
}
