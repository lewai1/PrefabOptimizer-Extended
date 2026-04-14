package dev.ninesliced.prefaboptimizer;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.ninesliced.prefaboptimizer.commands.PrefabOptimizerCommand;
import dev.ninesliced.prefaboptimizer.optimization.PrefabOptimizerService;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public final class PrefabOptimizer extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger(PrefabOptimizer.class.getName());

    public PrefabOptimizer(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new PrefabOptimizerCommand());
        LOGGER.info("PrefabOptimizer registered. Use /prefaboptimizer for prefab batches or /prefaboptimizer selection for selections.");
    }

    @Override
    protected void shutdown() {
        PrefabOptimizerService.shutdown();
    }
}
