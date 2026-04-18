package dev.lewai.prefaboptimizerextended.optimization;

import java.util.List;
import javax.annotation.Nonnull;

public record PrefabOptimizationResult(
    int sourceCount,
    int savedCount,
    int removedBlocks,
    int processedBlocks,
    @Nonnull List<String> savedPaths,
    @Nonnull List<String> warnings
) {
    public PrefabOptimizationResult {
        savedPaths = List.copyOf(savedPaths);
        warnings = List.copyOf(warnings);
    }

    public double removedBlockPercentage() {
        if (this.processedBlocks <= 0) {
            return 0.0;
        }
        return (double) this.removedBlocks * 100.0 / this.processedBlocks;
    }
}
