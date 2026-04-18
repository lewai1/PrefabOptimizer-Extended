package dev.lewai.prefaboptimizerextended.optimization;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PrefabBackupService {
    static final String BACKUP_ROOT_NAME = "__PrefabOptimizerBackup__";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss", Locale.ROOT);

    private PrefabBackupService() {
    }

    @Nonnull
    static String newTimestamp() {
        return TIMESTAMP.format(LocalDateTime.now());
    }

    @Nonnull
    static Path backupRootFor(@Nonnull Path baseDir, @Nonnull String timestamp) {
        return baseDir.resolve(BACKUP_ROOT_NAME).resolve(timestamp);
    }

    static void backupIfExists(
        @Nonnull Path baseDir,
        @Nonnull Path backupRoot,
        @Nonnull String targetKey,
        @Nonnull Path existingFile
    ) throws IOException {
        if (!Files.exists(existingFile)) {
            return;
        }
        Path backupPath = backupRoot.resolve(targetKey);
        Path parent = backupPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(existingFile, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    @Nullable
    public static RestoreResult restoreLatest(@Nonnull AssetPack pack) throws IOException {
        if (!AssetModule.get().validatePackExistsOnDisk(pack)) {
            throw new IOException("Asset pack '" + pack.getName() + "' no longer exists on disk");
        }
        Path baseDir = PrefabStore.get().getPrefabsPathForPack(pack);
        Path rootDir = baseDir.resolve(BACKUP_ROOT_NAME);
        if (!Files.isDirectory(rootDir)) {
            return null;
        }
        List<Path> timestampDirs;
        try (Stream<Path> stream = Files.list(rootDir)) {
            timestampDirs = stream
                .filter(Files::isDirectory)
                .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                .toList();
        }
        if (timestampDirs.isEmpty()) {
            return null;
        }
        Path latest = timestampDirs.get(0);
        List<String> restoredKeys = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(latest)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                Path relative = latest.relativize(file);
                Path dest = baseDir.resolve(relative);
                Path parent = dest.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                restoredKeys.add(relative.toString().replace('\\', '/'));
            }
        }
        return new RestoreResult(latest.getFileName().toString(), List.copyOf(restoredKeys));
    }

    public record RestoreResult(@Nonnull String timestamp, @Nonnull List<String> restoredKeys) {
    }
}
