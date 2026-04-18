package dev.lewai.prefaboptimizerextended.commands.subcommands;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.optimization.PrefabBackupService;
import dev.lewai.prefaboptimizerextended.util.ThrowableMessages;
import javax.annotation.Nonnull;

public final class UndoSubCommand extends AbstractPlayerCommand {
    public UndoSubCommand() {
        super("undo", "Restore the most recent batch backup for a given asset pack.");
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
        String packName = extractPackName(context.getInputString());
        if (packName.isEmpty()) {
            playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: usage /prefaboptimizer undo <pack>"));
            return;
        }
        AssetPack pack = AssetModule.get().getAssetPack(packName);
        if (pack == null) {
            playerRef.sendMessage(Message.raw("PrefabOptimizer-Extended: asset pack '" + packName + "' not found."));
            return;
        }

        try {
            PrefabBackupService.RestoreResult result = PrefabBackupService.restoreLatest(pack);
            if (result == null) {
                playerRef.sendMessage(Message.raw(
                    "PrefabOptimizer-Extended: no backups found for pack '" + pack.getName()
                        + "'. Backups are created automatically when a prefab batch overwrites existing files."
                ));
                return;
            }
            playerRef.sendMessage(Message.raw(
                "PrefabOptimizer-Extended: restored " + result.restoredKeys().size()
                    + " prefab(s) from backup " + result.timestamp() + " in pack '" + pack.getName() + "'."
            ));
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw(
                "PrefabOptimizer-Extended: undo failed: " + ThrowableMessages.readableMessage(e)
            ));
        }
    }

    // Hytale's CommandContext.getInputString() can include the root command name,
    // its aliases, and the subcommand name. Drop every known command token so
    // whatever remains is the pack name.
    private static final java.util.Set<String> COMMAND_TOKENS = java.util.Set.of(
        "prefaboptimizer", "popt", "prefabopt", "undo"
    );

    @Nonnull
    private static String extractPackName(@Nonnull String rawInput) {
        String trimmed = rawInput.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder remaining = new StringBuilder();
        for (String part : trimmed.split("\\s+")) {
            if (COMMAND_TOKENS.contains(part.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            if (remaining.length() > 0) {
                remaining.append(' ');
            }
            remaining.append(part);
        }
        return remaining.toString().trim();
    }
}
