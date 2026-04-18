package dev.lewai.prefaboptimizerextended.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import dev.lewai.prefaboptimizerextended.optimization.OptimizerSettings;

public final class OptimizerEventData {
    static final String KEY_ACTION = "Action";
    static final String KEY_FILE = "File";
    static final String KEY_SEARCH_RESULT = "SearchResult";
    static final String KEY_SEARCH_QUERY = "@SearchQuery";
    static final String KEY_SOURCE = "Source";
    static final String KEY_PRESERVE_TRANSPARENT = "@PreserveTransparent";
    static final String KEY_STRICT_CUBE_ONLY = "@StrictCubeOnly";
    static final String KEY_EXCLUDED_BLOCKS = "@ExcludedBlocks";
    static final String KEY_RECURSIVE_FOLDERS = "@RecursiveFolders";
    static final String KEY_TARGET_PACK = "@TargetPack";
    static final String KEY_TARGET_FOLDER = "@TargetFolder";

    public String action;
    public String file;
    public String searchResult;
    public String searchQuery;
    public String source;
    public Boolean preserveTransparent;
    public Boolean strictCubeOnly;
    public String excludedBlocks;
    public Boolean recursiveFolders;
    public String targetPack;
    public String targetFolder;

    public OptimizerSettings toSettings() {
        return OptimizerSettings.create(this.preserveTransparent, this.strictCubeOnly, this.excludedBlocks);
    }

    public static final BuilderCodec<OptimizerEventData> CODEC = BuilderCodec.<OptimizerEventData>builder(OptimizerEventData.class, OptimizerEventData::new)
        .append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (data, value) -> data.action = value, data -> data.action).add()
        .append(new KeyedCodec<>(KEY_FILE, Codec.STRING), (data, value) -> data.file = value, data -> data.file).add()
        .append(new KeyedCodec<>(KEY_SEARCH_RESULT, Codec.STRING), (data, value) -> data.searchResult = value, data -> data.searchResult).add()
        .append(new KeyedCodec<>(KEY_SEARCH_QUERY, Codec.STRING), (data, value) -> data.searchQuery = value, data -> data.searchQuery).add()
        .append(new KeyedCodec<>(KEY_SOURCE, Codec.STRING), (data, value) -> data.source = value, data -> data.source).add()
        .append(new KeyedCodec<>(KEY_PRESERVE_TRANSPARENT, Codec.BOOLEAN), (data, value) -> data.preserveTransparent = value, data -> data.preserveTransparent).add()
        .append(new KeyedCodec<>(KEY_STRICT_CUBE_ONLY, Codec.BOOLEAN), (data, value) -> data.strictCubeOnly = value, data -> data.strictCubeOnly).add()
        .append(new KeyedCodec<>(KEY_EXCLUDED_BLOCKS, Codec.STRING), (data, value) -> data.excludedBlocks = value, data -> data.excludedBlocks).add()
        .append(new KeyedCodec<>(KEY_RECURSIVE_FOLDERS, Codec.BOOLEAN), (data, value) -> data.recursiveFolders = value, data -> data.recursiveFolders).add()
        .append(new KeyedCodec<>(KEY_TARGET_PACK, Codec.STRING), (data, value) -> data.targetPack = value, data -> data.targetPack).add()
        .append(new KeyedCodec<>(KEY_TARGET_FOLDER, Codec.STRING), (data, value) -> data.targetFolder = value, data -> data.targetFolder).add()
        .build();
}
