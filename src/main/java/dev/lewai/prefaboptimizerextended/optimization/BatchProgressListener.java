package dev.lewai.prefaboptimizerextended.optimization;

@FunctionalInterface
public interface BatchProgressListener {
    void onProgress(int completedSources, int totalSources);

    BatchProgressListener NOOP = (c, t) -> {};
}
