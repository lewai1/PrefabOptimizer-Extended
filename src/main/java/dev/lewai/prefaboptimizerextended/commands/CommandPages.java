package dev.lewai.prefaboptimizerextended.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.lewai.prefaboptimizerextended.ui.PrefabOptimizerPage;
import javax.annotation.Nonnull;

public final class CommandPages {
    private CommandPages() {
    }

    public static void openPrefabBatchOptimizer(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull PlayerRef playerRef
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().openCustomPage(ref, store, new PrefabOptimizerPage(playerRef));
        }
    }
}
