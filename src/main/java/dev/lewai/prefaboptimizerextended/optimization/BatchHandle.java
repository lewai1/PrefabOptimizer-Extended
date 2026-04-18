package dev.lewai.prefaboptimizerextended.optimization;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

public final class BatchHandle {
    private final CompletableFuture<PrefabOptimizationResult> future;
    private final AtomicBoolean cancelled;

    BatchHandle(@Nonnull CompletableFuture<PrefabOptimizationResult> future, @Nonnull AtomicBoolean cancelled) {
        this.future = future;
        this.cancelled = cancelled;
    }

    @Nonnull
    public CompletableFuture<PrefabOptimizationResult> future() {
        return this.future;
    }

    public void cancel() {
        this.cancelled.set(true);
        this.future.cancel(true);
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }
}
