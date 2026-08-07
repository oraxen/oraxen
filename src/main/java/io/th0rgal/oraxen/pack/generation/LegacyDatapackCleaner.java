package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public final class LegacyDatapackCleaner {

    private LegacyDatapackCleaner() {
        throw new IllegalStateException("Utility class");
    }

    public static void clear(String name) {
        try {
            Path worldFolder = Bukkit.getWorlds().get(0).getWorldFolder().toPath();
            File datapackFolder = getDatapackRoot(worldFolder).resolve("datapacks/" + name).toFile();
            File legacyDatapackFolder = worldFolder.resolve("datapacks/" + name).toFile();

            FileUtils.deleteDirectory(datapackFolder);
            if (!datapackFolder.equals(legacyDatapackFolder)) {
                FileUtils.deleteDirectory(legacyDatapackFolder);
            }
        } catch (IOException | RuntimeException exception) {
            Logs.debug(exception);
        }
    }

    private static Path getDatapackRoot(Path worldFolder) {
        if (!VersionUtil.atOrAbove("26.1")) {
            return worldFolder;
        }

        Path normalizedWorldFolder = worldFolder.normalize();
        if (normalizedWorldFolder.endsWith(Path.of("dimensions", "minecraft", "overworld"))) {
            Path minecraftFolder = normalizedWorldFolder.getParent();
            if (minecraftFolder != null && minecraftFolder.getParent() != null
                    && minecraftFolder.getParent().getParent() != null) {
                return minecraftFolder.getParent().getParent();
            }
        }

        return worldFolder;
    }
}
