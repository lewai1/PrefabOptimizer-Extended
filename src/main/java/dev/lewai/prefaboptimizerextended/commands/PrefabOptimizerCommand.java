package dev.lewai.prefaboptimizerextended.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.commands.subcommands.PrefabBatchOptimizerSubCommand;
import dev.lewai.prefaboptimizerextended.commands.subcommands.SelectionOptimizerSubCommand;
import javax.annotation.Nonnull;

public final class PrefabOptimizerCommand extends AbstractPlayerCommand {
    public PrefabOptimizerCommand() {
        super("prefaboptimizer", "Open the prefab batch optimizer.");
        this.addAliases("popt", "prefabopt");
        this.setPermissionGroup(GameMode.Creative);
        this.addSubCommand(new SelectionOptimizerSubCommand());
        this.addSubCommand(new PrefabBatchOptimizerSubCommand());
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        CommandPages.openPrefabBatchOptimizer(ref, store, playerRef);
    }
}
