package io.th0rgal.oraxen;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OraxenPluginBootstrapTest {

    @Test
    void gatesRegistryFeaturesByMinecraftVersion() {
        assertFalse(OraxenPluginBootstrap.atOrAbove("1.21.2", 1, 21, 3));
        assertTrue(OraxenPluginBootstrap.atOrAbove("1.21.3", 1, 21, 3));
        assertTrue(OraxenPluginBootstrap.atOrAbove("1.21.6-pre1", 1, 21, 6));
        assertTrue(OraxenPluginBootstrap.atOrAbove("26.1.2", 1, 21, 6));
    }

    @Test
    void removesMineableTagOnlyWhenConfiguredMechanicIsEnabled() throws Exception {
        YamlConfiguration disabled = config("""
                block:
                  enabled: false
                  remove-mineable-tag: true
                """);
        YamlConfiguration retained = config("""
                block:
                  enabled: true
                  remove-mineable-tag: false
                """);
        YamlConfiguration removed = config("""
                block:
                  enabled: true
                  remove-mineable-tag: true
                """);

        assertFalse(OraxenPluginBootstrap.shouldRemoveNoteBlockMineableTag(disabled));
        assertFalse(OraxenPluginBootstrap.shouldRemoveNoteBlockMineableTag(retained));
        assertTrue(OraxenPluginBootstrap.shouldRemoveNoteBlockMineableTag(removed));
    }

    @Test
    void supportsLegacyNoteblockConfig() throws Exception {
        YamlConfiguration config = config("""
                noteblock:
                  enabled: true
                  remove_mineable_tag: true
                """);

        assertTrue(OraxenPluginBootstrap.shouldRemoveNoteBlockMineableTag(config));
    }

    @Test
    void migratesLegacySoundFileWhenSoundsFileIsMissing(@TempDir Path dataDirectory) throws Exception {
        Files.writeString(dataDirectory.resolve("sound.yml"), """
                sounds:
                  custom_song:
                    sound: oraxen.custom_song
                    jukebox_song:
                      length_in_seconds: 42
                """);

        YamlConfiguration migrated = OraxenPluginBootstrap.loadSoundsConfigWithLegacyMigration(dataDirectory);

        assertNotNull(migrated);
        List<?> sounds = migrated.getMapList("sounds");
        assertEquals(1, sounds.size());
        assertEquals("custom_song", ((java.util.Map<?, ?>) sounds.get(0)).get("id"));
    }

    @Test
    void mergesLegacySoundFileIntoExistingSoundsFile(@TempDir Path dataDirectory) throws Exception {
        Files.writeString(dataDirectory.resolve("sound.yml"), """
                sounds:
                  legacy_song:
                    sound: oraxen.legacy_song
                """);
        Files.writeString(dataDirectory.resolve("sounds.yml"), """
                sounds:
                - id: new_song
                  sound: oraxen.new_song
                """);

        YamlConfiguration merged = OraxenPluginBootstrap.loadSoundsConfigWithLegacyMigration(dataDirectory);

        assertNotNull(merged);
        List<String> ids = merged.getMapList("sounds").stream()
                .map(sound -> String.valueOf(sound.get("id")))
                .toList();
        assertTrue(ids.contains("new_song"));
        assertTrue(ids.contains("legacy_song"));
    }

    @Test
    void skipsLegacyMigrationWhenNoLegacySoundFileExists(@TempDir Path dataDirectory) {
        assertNull(OraxenPluginBootstrap.loadSoundsConfigWithLegacyMigration(dataDirectory));
    }

    private static YamlConfiguration config(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}
