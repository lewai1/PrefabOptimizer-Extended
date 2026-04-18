package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.builtin.buildertools.PrototypePlayerBuilderToolSettings;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockPattern;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.LocalCachedChunkAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class SelectionOptimizationService {
    private final BlockClassifier classifier;

    SelectionOptimizationService(@Nonnull BlockClassifier classifier) {
        this.classifier = classifier;
    }

    boolean optimize(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull Player player,
        @Nonnull PlayerRef playerRef,
        @Nonnull OptimizerSettings settings
    ) {
        if (!this.isOkayToOptimizeSelection(ref, store, player, playerRef)) {
            return false;
        }

        if (settings.excludedBlocksRegexError() != null) {
            playerRef.sendMessage(Message.raw(
                "PrefabOptimizer-Extended warning: exclusion regex invalid, fell back to token matching: "
                    + settings.excludedBlocksRegexError()
            ));
        }

        BuilderToolsPlugin.addToQueue(player, playerRef, (queueRef, state, componentAccessor) -> {
            BlockSelection selection = state.getSelection();
            if (selection == null || !selection.hasSelectionBounds()) {
                playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: select a region first."));
                return;
            }

            BlockBounds bounds = SelectionBoundsReader.read(selection);
            World world = componentAccessor.getExternalData().getWorld();
            LocalCachedChunkAccessor accessor = LocalCachedChunkAccessor.atWorldCoords(
                world,
                bounds.centerX(),
                bounds.centerZ(),
                Math.max(bounds.width(), bounds.depth()) + 2
            );

            OccludedBlockMask primingMask = new OccludedBlockMask(this.classifier, settings);
            LongSet reachableAir = settings.floodFillInterior()
                ? SelectionFloodFill.computeReachableAir(accessor, bounds, primingMask)
                : null;
            OccludedBlockMask mask = reachableAir == null
                ? primingMask
                : new OccludedBlockMask(this.classifier, settings, reachableAir);

            SelectionStats stats = countSelectionStats(selection, mask, accessor, bounds);
            if (stats.removedBlocks() <= 0) {
                playerRef.sendMessage(Message.raw(
                    "PrefabOptimizer-Extended finished: removed 0/" + stats.processedBlocks()
                        + " blocks (" + formatPercentage(stats.removedBlockPercentage()) + " optimized)."
                ));
                return;
            }

            state.replace(queueRef, mask, BlockPattern.EMPTY, componentAccessor);
            playerRef.sendMessage(Message.raw(
                "PrefabOptimizer-Extended finished: removed " + stats.removedBlocks() + "/" + stats.processedBlocks()
                    + " blocks (" + formatPercentage(stats.removedBlockPercentage())
                    + " optimized). Use /undo to revert."
            ));
        });
        return true;
    }

    private boolean isOkayToOptimizeSelection(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull Player player,
        @Nonnull PlayerRef playerRef
    ) {
        Boolean playerRefResult = invokeBuilderToolsSelectionCheck(ref, store, playerRef, PlayerRef.class, playerRef);
        if (playerRefResult != null) {
            return playerRefResult;
        }

        Boolean playerResult = invokeBuilderToolsSelectionCheck(ref, store, playerRef, Player.class, player);
        if (playerResult != null) {
            return playerResult;
        }

        // Hytale builds disagree on the second parameter type. If neither
        // method exists, avoid crashing and let BuilderTools queueing decide.
        return true;
    }

    @Nullable
    private static Boolean invokeBuilderToolsSelectionCheck(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef playerRef,
        @Nonnull Class<?> secondParameterType,
        @Nonnull Object secondParameter
    ) {
        try {
            Method method = PrototypePlayerBuilderToolSettings.class.getMethod(
                "isOkayToDoCommandsOnSelection",
                Ref.class,
                secondParameterType,
                ComponentAccessor.class
            );
            Object result = method.invoke(null, ref, secondParameter, store);
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: could not verify BuilderTools selection state (" + e.getClass().getSimpleName() + ")."));
            return false;
        }
    }

    @SuppressWarnings("removal")
    private static SelectionStats countSelectionStats(
        @Nonnull BlockSelection selection,
        @Nonnull OccludedBlockMask mask,
        @Nonnull LocalCachedChunkAccessor accessor,
        @Nonnull BlockBounds bounds
    ) {
        int processedBlocks = 0;
        int removedBlocks = 0;
        for (int x = bounds.xMin(); x <= bounds.xMax(); x++) {
            for (int z = bounds.zMin(); z <= bounds.zMax(); z++) {
                WorldChunk chunk = accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                if (chunk == null) {
                    continue;
                }
                for (int y = bounds.yMin(); y <= bounds.yMax(); y++) {
                    int blockId = chunk.getBlock(x, y, z);
                    if (blockId <= 0 || chunk.getFiller(x, y, z) != 0) {
                        continue;
                    }
                    processedBlocks++;
                    if (mask.isRemovable(accessor, x, y, z, bounds, blockId)) {
                        removedBlocks++;
                    }
                }
            }
        }
        return new SelectionStats(processedBlocks, removedBlocks);
    }

    @Nonnull
    private static String formatPercentage(double percentage) {
        return String.format(Locale.ROOT, "%.2f%%", percentage);
    }

    private record SelectionStats(int processedBlocks, int removedBlocks) {
        double removedBlockPercentage() {
            if (this.processedBlocks <= 0) {
                return 0.0;
            }
            return (double) this.removedBlocks * 100.0 / this.processedBlocks;
        }
    }
}
