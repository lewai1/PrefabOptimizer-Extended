package dev.ninesliced.prefaboptimizer.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.ninesliced.prefaboptimizer.commands.CommandPages;
import javax.annotation.Nonnull;

public final class PrefabBatchOptimizerSubCommand extends AbstractPlayerCommand {
    public PrefabBatchOptimizerSubCommand() {
        super("prefabs", "Open the prefab batch optimizer.");
        this.addAliases("prefab", "batch");
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
        CommandPages.openPrefabBatchOptimizer(ref, store, playerRef);
    }
}
