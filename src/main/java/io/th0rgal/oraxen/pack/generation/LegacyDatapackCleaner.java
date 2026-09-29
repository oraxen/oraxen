package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.platform.BukkitWrapper;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

public final class LegacyDatapackCleaner {

    private static final List<String> REPLACED_DATAPACKS = List.of(
            "oraxen_paintings",
            "oraxen_jukebox"
    );
    private static final String FILE_PACK_PREFIX = "file/";
    private static final Path OVERWORLD_PATH = Path.of("dimensions", "minecraft", "overworld");

    private LegacyDatapackCleaner() {
        throw new IllegalStateException("Utility class");
    }

    public static void clearReplacedDatapacks() {
        Path worldFolder = overworldFolder();
        List<Path> datapackRoots = resolveDatapackRoots(worldFolder);

        for (String name : REPLACED_DATAPACKS) {
            try {
                // Paper reports world datapacks as "file/<folder>", match either form
                BukkitWrapper.get().setDatapackEnabled(name, false);
                BukkitWrapper.get().setDatapackEnabled(FILE_PACK_PREFIX + name, false);
            } catch (RuntimeException exception) {
                Logs.logWarning("Failed to disable legacy Oraxen datapack '" + name + "': " + exception.getMessage());
                Logs.debug(exception);
            }
            clear(name, datapackRoots, Logs::logWarning);
        }
    }

    public static void clearReplacedDatapacks(Path dataDirectory, Consumer<String> warningLogger) {
        Path serverRoot = dataDirectory.toAbsolutePath().normalize().resolve("../..").normalize();
        Path worldFolder = serverRoot.resolve(levelName(serverRoot, warningLogger)).normalize();
        List<Path> datapackRoots = resolveDatapackRoots(worldFolder);

        for (String name : REPLACED_DATAPACKS)
            clear(name, datapackRoots, warningLogger);
    }

    private static List<Path> resolveDatapackRoots(Path worldFolder) {
        Path normalized = worldFolder.normalize();
        Path datapacks = normalized.resolve("datapacks");

        if (!normalized.endsWith(OVERWORLD_PATH))
            return List.of(datapacks, normalized.resolve(OVERWORLD_PATH).resolve("datapacks"));

        return List.of(normalized.getParent().getParent().getParent().resolve("datapacks"), datapacks);
    }

    private static void clear(String name, List<Path> datapackRoots, Consumer<String> warningLogger) {
        for (Path datapackRoot : datapackRoots) {
            Path datapackFolder = datapackRoot.resolve(name);
            if (!Files.exists(datapackFolder)) continue;

            try {
                deleteDirectory(datapackFolder);
            } catch (IOException | RuntimeException exception) {
                warningLogger.accept("Failed to remove legacy Oraxen datapack '" + name + "' at "
                        + datapackFolder + ": " + exception.getMessage());
                Logs.debug(exception);
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception != null) throw exception;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path overworldFolder() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL)
                return world.getWorldFolder().toPath();
        }
        return Bukkit.getWorlds().get(0).getWorldFolder().toPath();
    }

    private static String levelName(Path serverRoot, Consumer<String> warningLogger) {
        Path serverProperties = serverRoot.resolve("server.properties");
        if (!Files.isRegularFile(serverProperties)) return "world";

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(serverProperties)) {
            properties.load(input);
            return properties.getProperty("level-name", "world").trim();
        } catch (IOException exception) {
            warningLogger.accept("Failed to read server.properties while removing legacy Oraxen datapacks: "
                    + exception.getMessage());
            return "world";
        }
    }
}
