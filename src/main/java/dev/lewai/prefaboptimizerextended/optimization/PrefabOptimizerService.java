package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;

public final class PrefabOptimizerService {
    private static final BlockClassifier CLASSIFIER = new BlockClassifier();
    private static final AssetPrefabFileProvider SOURCE_PROVIDER = new AssetPrefabFileProvider();
    private static final ExecutorService PREFAB_BATCH_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "PrefabOptimizerExt-Batch");
        thread.setDaemon(true);
        return thread;
    });
    private static final PrefabSourceCollector SOURCE_COLLECTOR = new PrefabSourceCollector(SOURCE_PROVIDER);
    private static final SelectionOptimizationService SELECTION_OPTIMIZER = new SelectionOptimizationService(CLASSIFIER);
    private static final PrefabBatchOptimizer PREFAB_BATCH_OPTIMIZER = new PrefabBatchOptimizer(
        CLASSIFIER,
        SOURCE_COLLECTOR,
        PREFAB_BATCH_EXECUTOR
    );

    private PrefabOptimizerService() {
    }

    public static boolean optimizeSelection(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull Player player,
        @Nonnull PlayerRef playerRef,
        @Nonnull OptimizerSettings settings
    ) {
        return SELECTION_OPTIMIZER.optimize(ref, store, player, playerRef, settings);
    }

    @Nonnull
    public static PrefabOptimizationResult optimizePrefabSources(
        @Nonnull Collection<String> sourceVirtualPaths,
        @Nonnull AssetPack targetPack,
        @Nonnull String targetFolder,
        @Nonnull OptimizerSettings settings,
        boolean recursiveFolders
    ) {
        return PREFAB_BATCH_OPTIMIZER.optimize(sourceVirtualPaths, targetPack, targetFolder, settings, recursiveFolders);
    }

    @Nonnull
    public static CompletableFuture<PrefabOptimizationResult> optimizePrefabSourcesAsync(
        @Nonnull Collection<String> sourceVirtualPaths,
        @Nonnull AssetPack targetPack,
        @Nonnull String targetFolder,
        @Nonnull OptimizerSettings settings,
        boolean recursiveFolders
    ) {
        return PREFAB_BATCH_OPTIMIZER.optimizeAsync(sourceVirtualPaths, targetPack, targetFolder, settings, recursiveFolders);
    }

    @Nonnull
    public static AssetPrefabFileProvider sourceProvider() {
        return SOURCE_PROVIDER;
    }

    public static void shutdown() {
        PREFAB_BATCH_EXECUTOR.shutdownNow();
    }
}
