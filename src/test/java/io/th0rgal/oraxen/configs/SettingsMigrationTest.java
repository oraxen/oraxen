package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SettingsMigrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesEveryLeafOfCustomized12201Settings() throws Exception {
        YamlConfiguration settings = resource("/configs/settings-1.220.1.yml");
        Map<String, Object> oldLeaves = leaves(settings);
        Map<String, String> paths = migrationPaths();

        assertTrue(SettingsMigration.migrate(settings));

        for (var entry : oldLeaves.entrySet()) {
            String oldPath = entry.getKey();
            String newPath = mappedPath(paths, oldPath);
            assertTrue(oldPath.equals("debug") || !oldPath.equals(newPath),
                    "Unaccounted old setting " + oldPath);
            Object expected = oldPath.equals("Pack.generation.disable_mcmeta_generation")
                    ? !((Boolean) entry.getValue()) : entry.getValue();
            assertEquals(expected, settings.get(newPath), oldPath + " -> " + newPath);
            if (!oldPath.equals(newPath)) assertFalse(settings.contains(oldPath, true), oldPath);
        }
        assertEquals(oldLeaves.size(), leaves(settings).size());
        for (String oldRoot : List.of("Plugin", "ConfigsTools", "Glyphs", "CustomArmor", "ItemUpdater",
                "FurnitureUpdater", "Pack", "Misc", "inventory-menu", "Studio", "update-checker"))
            assertFalse(settings.contains(oldRoot, true), oldRoot);
        assertEquals(List.of(1001, "1010-1020"), settings.getList("configuration.items.model-data.skipped-numbers"));
        assertEquals(0.35, settings.getDouble("pack.receive.loaded.actions.sound.volume"));
        assertEquals("custom_icon", settings.getString("inventory.layout.custom_category_ID.icon"));
    }

    @Test
    void invertsMcmetaDisableFlagInBothDirections() {
        for (boolean disabled : List.of(false, true)) {
            YamlConfiguration settings = new YamlConfiguration();
            settings.set("Pack.generation.disable_mcmeta_generation", disabled);
            assertTrue(SettingsMigration.migrate(settings));
            assertEquals(!disabled, settings.getBoolean("pack.generation.mcmeta"));
            assertFalse(settings.contains("Pack", true));
        }
    }

    @Test
    void explicitNewLeavesWinWhilePartialSectionsMerge() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("Plugin.language", "german");
        settings.set("general.language", "french");
        settings.set("Pack.generation.disable_mcmeta_generation", false);
        settings.set("pack.generation.mcmeta", false);
        settings.set("Pack.receive.loaded.actions.sound.volume", 0.25);
        settings.set("pack.receive.loaded.actions.sound.pitch", 1.75);
        settings.set("inventory-menu.layout.My_Custom_ID.icon", "old_icon");
        settings.set("inventory-menu.layout.My_Custom_ID.name", "Old name");
        settings.set("inventory.layout.My_Custom_ID.icon", "new_icon");
        settings.set("inventory.layout.other_ID.slot", 21);
        settings.set("Plugin.custom_extension.enabled", false);

        assertTrue(SettingsMigration.migrate(settings));

        assertEquals("french", settings.getString("general.language"));
        assertFalse(settings.getBoolean("pack.generation.mcmeta"));
        assertEquals(0.25, settings.getDouble("pack.receive.loaded.actions.sound.volume"));
        assertEquals(1.75, settings.getDouble("pack.receive.loaded.actions.sound.pitch"));
        assertEquals("new_icon", settings.getString("inventory.layout.My_Custom_ID.icon"));
        assertEquals("Old name", settings.getString("inventory.layout.My_Custom_ID.name"));
        assertEquals(21, settings.getInt("inventory.layout.other_ID.slot"));
        assertFalse(settings.getBoolean("Plugin.custom_extension.enabled"));
        assertTrue(settings.contains("Plugin.custom_extension.enabled", true));
        assertFalse(settings.contains("Plugin.language", true));
        assertFalse(settings.contains("Pack", true));
        assertFalse(settings.contains("inventory-menu", true));
    }

    @Test
    void copiesCommentsAndRetainsExplicitNewComments() {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("Plugin.language", "german");
        settings.setComments("Plugin.language", List.of("Custom language"));
        settings.setInlineComments("Plugin.language", List.of("Keep this comment"));
        settings.set("Plugin.formatting.chat", false);
        settings.setComments("Plugin.formatting.chat", List.of("Old comment"));
        settings.set("general.formatting.chat", true);
        settings.setComments("general.formatting.chat", List.of("New comment"));

        SettingsMigration.migrate(settings);

        assertEquals(List.of("Custom language"), settings.getComments("general.language"));
        assertEquals(List.of("Keep this comment"), settings.getInlineComments("general.language"));
        assertEquals(List.of("New comment"), settings.getComments("general.formatting.chat"));
    }

    @Test
    void defaultsFillMissingSettingsAfterMigrationWithoutReplacingCustomValues() throws Exception {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("Plugin.language", "german");
        settings.set("Pack.generation.disable_mcmeta_generation", true);
        settings.set("inventory-menu.layout.custom_ID.icon", "custom_icon");
        SettingsMigration.migrate(settings);
        YamlConfiguration defaults = resource("/settings.yml");
        for (var entry : leaves(defaults).entrySet()) {
            if (!settings.contains(entry.getKey(), true)) settings.set(entry.getKey(), entry.getValue());
        }

        assertEquals("german", settings.getString("general.language"));
        assertFalse(settings.getBoolean("pack.generation.mcmeta"));
        assertEquals("custom_icon", settings.getString("inventory.layout.custom_ID.icon"));
        for (String path : leaves(defaults).keySet()) assertTrue(settings.contains(path, true), path);
        assertEquals(defaults.get("pack.upload.s3.presigned-url-expiry"),
                settings.get("pack.upload.s3.presigned-url-expiry"));
        assertFalse(SettingsMigration.migrate(settings));
    }

    @Test
    void emptyRenamedLayoutSurvivesDefaultMerge() throws Exception {
        YamlConfiguration settings = new YamlConfiguration();
        settings.loadFromString("inventory-menu: {layout: {}}");

        assertTrue(SettingsUpdater.migrateBeforeDefaults(settings));

        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getResource(anyString())).thenAnswer(invocation ->
                SettingsMigrationTest.class.getResourceAsStream("/" + invocation.getArgument(0)));
        ConfigsManager manager = new ConfigsManager(plugin);
        when(plugin.getConfigsManager()).thenReturn(manager);
        var updateDefaults = ConfigsManager.class.getDeclaredMethod("updateDefaultKeys",
                String.class, YamlConfiguration.class, YamlConfiguration.class);
        updateDefaults.setAccessible(true);
        try (var bukkit = mockStatic(Bukkit.class);
             var plugins = mockStatic(OraxenPlugin.class)) {
            plugins.when(OraxenPlugin::get).thenReturn(plugin);
            updateDefaults.invoke(manager, "settings.yml", settings, resource("/settings.yml"));
        }

        YamlConfiguration reloaded = new YamlConfiguration();
        reloaded.loadFromString(settings.saveToString());
        assertFalse(reloaded.contains("inventory-menu", true));
        ConfigurationSection layout = reloaded.getConfigurationSection("inventory.layout");
        assertNotNull(layout);
        assertTrue(layout.getKeys(false).isEmpty());
        assertEquals(resource("/settings.yml").get("inventory.rows"), reloaded.get("inventory.rows"));
        assertFalse(SettingsUpdater.migrateBeforeDefaults(reloaded));
    }

    @Test
    void emptyRenamedSectionsPreserveCommentsAndExplicitNewValues() throws Exception {
        YamlConfiguration settings = new YamlConfiguration();
        settings.loadFromString("inventory-menu: {layout: {}}");
        settings.setComments("inventory-menu.layout", List.of("No categories"));

        assertTrue(SettingsMigration.migrate(settings));
        assertEquals(List.of("No categories"), settings.getComments("inventory.layout"));

        settings.createSection("inventory-menu.layout");
        settings.set("inventory.layout.custom.icon", "custom_icon");
        settings.setComments("inventory.layout", List.of("Current layout"));
        assertTrue(SettingsMigration.migrate(settings));
        assertEquals("custom_icon", settings.getString("inventory.layout.custom.icon"));
        assertEquals(List.of("Current layout"), settings.getComments("inventory.layout"));
        assertFalse(settings.contains("inventory-menu", true));
    }

    @Test
    void secondMigrationLeavesSerializedContentAndSavedFileBytesUnchanged() throws Exception {
        YamlConfiguration settings = resource("/configs/settings-1.220.1.yml");
        assertTrue(SettingsMigration.migrate(settings));
        Path file = temporaryDirectory.resolve("settings.yml");
        settings.save(file.toFile());
        byte[] firstBytes = Files.readAllBytes(file);
        YamlConfiguration reloaded = YamlConfiguration.loadConfiguration(file.toFile());
        String firstSerialized = reloaded.saveToString();

        boolean changed = SettingsMigration.migrate(reloaded);
        assertFalse(changed);
        assertEquals(firstSerialized, reloaded.saveToString());
        reloaded.save(file.toFile());
        assertArrayEquals(firstBytes, Files.readAllBytes(file));
    }

    @Test
    void migratesProtectionSettingsWithNewestPathPrecedence() {
        for (boolean intermediateFirst : List.of(false, true)) {
            YamlConfiguration settings = new YamlConfiguration();
            if (intermediateFirst) settings.set("pack.obfuscation.type", "SIMPLE");
            settings.set("Pack.generation.obfuscation.type", "FULL");
            settings.set("Pack.generation.obfuscation.custom-option", "preserved");
            if (!intermediateFirst) settings.set("pack.obfuscation.type", "SIMPLE");
            settings.setComments("pack.obfuscation.type", List.of("Intermediate choice"));

            assertTrue(SettingsMigration.migrate(settings));
            assertEquals("SIMPLE", settings.getString("pack.protection.obfuscation.type"));
            assertEquals("preserved", settings.getString("pack.protection.obfuscation.custom-option"));
            assertEquals(List.of("Intermediate choice"), settings.getComments("pack.protection.obfuscation.type"));
            assertFalse(settings.contains("Pack", true));
            assertFalse(settings.contains("pack.obfuscation", true));

            settings.set("Pack.generation.obfuscation.type", "FULL");
            settings.set("pack.obfuscation.type", "SIMPLE");
            settings.set("pack.protection.obfuscation.type", "NONE");
            settings.set("pack.protection.size-spoofing", true);
            assertTrue(SettingsMigration.migrate(settings));
            assertEquals("NONE", settings.getString("pack.protection.obfuscation.type"));
            assertTrue(settings.getBoolean("pack.protection.size-spoofing"));
            assertFalse(SettingsMigration.migrate(settings));
        }
    }

    @Test
    void protectionDefaultsDisableSizeSpoofingAndObfuscation() throws Exception {
        YamlConfiguration defaults = resource("/settings.yml");
        assertEquals(false, defaults.get(Settings.PACK_SIZE_SPOOFING.getPath()));
        assertEquals("NONE", defaults.get(Settings.PACK_OBFUSCATION_TYPE.getPath()));
        assertFalse(defaults.contains("pack.obfuscation", true));
    }
    private static YamlConfiguration resource(String path) throws Exception {
        try (var input = Objects.requireNonNull(SettingsMigrationTest.class.getResourceAsStream(path));
             var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private static Map<String, Object> leaves(YamlConfiguration settings) {
        Map<String, Object> values = new LinkedHashMap<>();
        settings.getValues(true).forEach((path, value) -> {
            if (!(value instanceof ConfigurationSection)) values.put(path, value);
        });
        return values;
    }

    private static Map<String, String> migrationPaths() throws Exception {
        Map<String, String> paths = new LinkedHashMap<>();
        try (var input = Objects.requireNonNull(SettingsMigrationTest.class.getResourceAsStream("/settings-migration.tsv"));
             var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line : reader.lines().filter(line -> !line.startsWith("#")).toList()) {
                String[] columns = line.split("\t", 2);
                paths.put(columns[0], columns[1]);
            }
        }
        return paths;
    }

    private static String mappedPath(Map<String, String> paths, String path) {
        return paths.entrySet().stream()
                .filter(entry -> path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "."))
                .max(java.util.Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(entry -> entry.getValue() + path.substring(entry.getKey().length()))
                .orElse(path);
    }
}
