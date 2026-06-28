package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MigrationBackups {

    private static final String MIGRATED_FOLDER = ".migrated";

    private MigrationBackups() {
    }

    public static boolean moveToMigrated(@NotNull File dataFolder, @NotNull File file) {
        if (!file.exists())
            return false;

        try {
            Path destination = uniqueMigratedPath(dataFolder.toPath(), file.toPath());
            Files.createDirectories(destination.getParent());
            Files.move(file.toPath(), destination);
            Logs.logSuccess("Moved migrated config <blue>"
                    + dataFolder.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                    + "</blue> to <blue>" + dataFolder.toPath().relativize(destination).toString().replace('\\', '/')
                    + "</blue>");
            return true;
        } catch (IOException | IllegalArgumentException e) {
            Logs.logError("Failed to move migrated config: " + file.getName());
            Logs.debug(e);
            return false;
        }
    }

    @NotNull
    private static Path uniqueMigratedPath(@NotNull Path dataFolder, @NotNull Path source) {
        Path relativeSource;
        try {
            relativeSource = dataFolder.relativize(source);
        } catch (IllegalArgumentException e) {
            relativeSource = source.getFileName();
        }

        Path requested = dataFolder.resolve(MIGRATED_FOLDER).resolve(relativeSource);
        if (!Files.exists(requested))
            return requested;

        Path parent = requested.getParent();
        String fileName = requested.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
        String extension = dotIndex == -1 ? "" : fileName.substring(dotIndex);

        int counter = 1;
        Path candidate;
        do {
            candidate = parent.resolve(baseName + "-" + counter + extension);
            counter++;
        } while (Files.exists(candidate));
        return candidate;
    }
}
