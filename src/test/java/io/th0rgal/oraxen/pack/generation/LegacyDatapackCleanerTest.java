package io.th0rgal.oraxen.pack.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyDatapackCleanerTest {

    @Test
    void removesReplacedOraxenDatapacksFromCustomWorld(@TempDir Path serverRoot) throws Exception {
        Path dataDirectory = Files.createDirectories(serverRoot.resolve("plugins/Oraxen"));
        Files.writeString(serverRoot.resolve("server.properties"), "level-name=custom-world\n");

        Path datapacks = Files.createDirectories(serverRoot.resolve("custom-world/datapacks"));
        Path nestedDatapacks = Files.createDirectories(
                serverRoot.resolve("custom-world/dimensions/minecraft/overworld/datapacks"));
        Files.createDirectories(datapacks.resolve("oraxen_paintings"));
        Files.createDirectories(datapacks.resolve("oraxen_jukebox"));
        Files.createDirectories(nestedDatapacks.resolve("oraxen_paintings"));
        Files.createDirectories(nestedDatapacks.resolve("oraxen_jukebox"));
        Path unrelated = Files.createDirectories(datapacks.resolve("unrelated_pack"));

        List<String> warnings = new ArrayList<>();
        LegacyDatapackCleaner.clearReplacedDatapacks(dataDirectory, warnings::add);

        assertFalse(Files.exists(datapacks.resolve("oraxen_paintings")));
        assertFalse(Files.exists(datapacks.resolve("oraxen_jukebox")));
        assertFalse(Files.exists(nestedDatapacks.resolve("oraxen_paintings")));
        assertFalse(Files.exists(nestedDatapacks.resolve("oraxen_jukebox")));
        assertTrue(Files.isDirectory(unrelated));
        assertTrue(warnings.isEmpty());
    }
}
