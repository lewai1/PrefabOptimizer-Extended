package dev.lewai.prefaboptimizerextended.optimization;

import java.nio.file.Path;
import javax.annotation.Nonnull;

record PrefabSource(@Nonnull Path path, @Nonnull Path relativeOutputPath) {
}
