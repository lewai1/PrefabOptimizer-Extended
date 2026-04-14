package dev.ninesliced.prefaboptimizer.optimization;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

record PrefabSourceCollection(
    @Nonnull Map<Path, PrefabSource> sources,
    @Nonnull List<String> warnings
) {
    public PrefabSourceCollection {
        sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
        warnings = List.copyOf(warnings);
    }
}
