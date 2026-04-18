package dev.lewai.prefaboptimizerextended.commands.subcommands;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.ui.SelectionOptimizerPage;
import javax.annotation.Nonnull;

public final class SelectionOptimizerSubCommand extends AbstractPlayerCommand {
    public SelectionOptimizerSubCommand() {
        super("selection", "Open the selection optimizer.");
        this.addAliases("sel");
        this.setPermissionGroup(GameMode.Creative);
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        BlockSelection selection = BuilderToolsPlugin.getState(player, playerRef).getSelection();
        if (selection == null || !selection.hasSelectionBounds()) {
            context.sendMessage(Message.raw("PrefabOptimizer-Extended: make a BuilderTools selection first, then run /prefaboptimizer selection."));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new SelectionOptimizerPage(playerRef));
    }
}
