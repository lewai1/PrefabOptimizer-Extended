package dev.ninesliced.prefaboptimizer.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.ninesliced.prefaboptimizer.optimization.OptimizerSettings;

public final class SelectionEventData {
    static final String KEY_ACTION = "Action";
    static final String KEY_PRESERVE_TRANSPARENT = "@PreserveTransparent";
    static final String KEY_STRICT_CUBE_ONLY = "@StrictCubeOnly";
    static final String KEY_EXCLUDED_BLOCKS = "@ExcludedBlocks";

    public String action;
    public Boolean preserveTransparent;
    public Boolean strictCubeOnly;
    public String excludedBlocks;

    public OptimizerSettings toSettings() {
        return OptimizerSettings.create(this.preserveTransparent, this.strictCubeOnly, this.excludedBlocks);
    }

    public static final BuilderCodec<SelectionEventData> CODEC = BuilderCodec.<SelectionEventData>builder(SelectionEventData.class, SelectionEventData::new)
        .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
        .append(new KeyedCodec<>(KEY_PRESERVE_TRANSPARENT, Codec.BOOLEAN), (data, value) -> data.preserveTransparent = value, data -> data.preserveTransparent).add()
        .append(new KeyedCodec<>(KEY_STRICT_CUBE_ONLY, Codec.BOOLEAN), (data, value) -> data.strictCubeOnly = value, data -> data.strictCubeOnly).add()
        .append(new KeyedCodec<>(KEY_EXCLUDED_BLOCKS, Codec.STRING), (data, value) -> data.excludedBlocks = value, data -> data.excludedBlocks).add()
        .build();
}
