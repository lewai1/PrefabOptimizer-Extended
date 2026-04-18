package dev.lewai.prefaboptimizerextended.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.optimization.PrefabOptimizerService;
import javax.annotation.Nonnull;

public final class SelectionOptimizerPage extends InteractiveCustomUIPage<SelectionEventData> {
    private static final String LAYOUT_PATH = "Pages/PrefabOptimizer/SelectionOptimizer.ui";

    private String status = "Adjust the optimizer settings, then optimize the current BuilderTools selection.";

    public SelectionOptimizerPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, SelectionEventData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder ui,
        @Nonnull UIEventBuilder events,
        @Nonnull Store<EntityStore> store
    ) {
        ui.append(LAYOUT_PATH);
        ui.set("#StatusLabel.Text", this.status);
        this.bindActions(events);
    }

    private void bindActions(@Nonnull UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", actionData(Action.CLOSE), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#OptimizeSelectionButton", settingsActionData(), false);
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SelectionEventData data
    ) {
        super.handleDataEvent(ref, store, data);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Action action = UiActions.parse(Action.class, data.action);
        if (action == null) {
            return;
        }

        switch (action) {
            case CLOSE -> player.getPageManager().setPage(ref, store, Page.None);
            case OPTIMIZE_SELECTION -> {
                if (PrefabOptimizerService.optimizeSelection(ref, store, player, this.playerRef, data.toSettings())) {
                    this.playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: selection optimization is in process. You will be notified when it finishes."));
                    player.getPageManager().setPage(ref, store, Page.None);
                } else {
                    this.status = "Could not queue the selection optimization.";
                    UICommandBuilder ui = new UICommandBuilder();
                    UIEventBuilder events = new UIEventBuilder();
                    ui.set("#StatusLabel.Text", this.status);
                    this.bindActions(events);
                    this.sendUpdate(ui, events, false);
                }
            }
        }
    }

    @Nonnull
    private static EventData actionData(@Nonnull Action action) {
        return new EventData().put(SelectionEventData.KEY_ACTION, action.name());
    }

    @Nonnull
    private static EventData settingsActionData() {
        return actionData(Action.OPTIMIZE_SELECTION)
            .put(SelectionEventData.KEY_PRESERVE_TRANSPARENT, "#PreserveTransparent.Value")
            .put(SelectionEventData.KEY_STRICT_CUBE_ONLY, "#StrictCubeOnly.Value")
            .put(SelectionEventData.KEY_PRESERVE_FLUID_ADJACENT, "#PreserveFluidAdjacent.Value")
            .put(SelectionEventData.KEY_FLOOD_FILL_INTERIOR, "#FloodFillInterior.Value")
            .put(SelectionEventData.KEY_EXCLUDED_BLOCKS, "#ExcludedBlocks.Value");
    }

    private enum Action {
        CLOSE,
        OPTIMIZE_SELECTION
    }
}
