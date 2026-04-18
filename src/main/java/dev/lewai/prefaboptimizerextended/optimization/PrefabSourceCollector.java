package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.builtin.buildertools.prefablist.AssetPrefabFileProvider;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import dev.lewai.prefaboptimizerextended.util.ThrowableMessages;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

final class PrefabSourceCollector {
    private final AssetPrefabFileProvider sourceProvider;

    PrefabSourceCollector(@Nonnull AssetPrefabFileProvider sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Nonnull
    PrefabSourceCollection collect(@Nonnull Collection<String> sourceVirtualPaths, boolean recursiveFolders) {
        Map<Path, PrefabSource> sources = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String virtualPath : sourceVirtualPaths) {
            this.collectSource(sources, warnings, virtualPath, recursiveFolders);
        }
        return new PrefabSourceCollection(sources, warnings);
    }

    private void collectSource(
        @Nonnull Map<Path, PrefabSource> sources,
        @Nonnull List<String> warnings,
        String virtualPath,
        boolean recursiveFolders
    ) {
        if (virtualPath == null || virtualPath.isBlank()) {
            return;
        }

        Path resolved = this.sourceProvider.resolveVirtualPath(virtualPath);
        if (resolved == null || !Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) {
            warnings.add("Source not found: " + virtualPath);
            return;
        }
        if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            this.collectDirectorySources(sources, warnings, resolved, recursiveFolders);
        } else if (PrefabStore.PREFAB_FILTER.test(resolved)) {
            sources.put(resolved.toAbsolutePath().normalize(), new PrefabSource(resolved, resolved.getFileName()));
        } else {
            warnings.add("Source is not a prefab: " + virtualPath);
        }
    }

    private void collectDirectorySources(
        @Nonnull Map<Path, PrefabSource> sources,
        @Nonnull List<String> warnings,
        @Nonnull Path directory,
        boolean recursiveFolders
    ) {
        try (Stream<Path> stream = recursiveFolders ? Files.walk(directory) : Files.list(directory)) {
            stream
                .filter(PrefabStore.PREFAB_FILTER)
                .sorted()
                .forEach(path -> sources.put(
                    path.toAbsolutePath().normalize(),
                    new PrefabSource(path, directory.relativize(path))
                ));
        } catch (IOException e) {
            warnings.add("Could not read folder " + directory + ": " + ThrowableMessages.readableMessage(e));
        }
    }
}
