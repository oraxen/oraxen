package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.configs.ResourcesManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class RemoveDefaultsCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void keepsGuiItemsWhileRemovingBundledSampleItems() throws IOException {
        Path itemsFolder = Files.createDirectories(tempDir.resolve("items"));
        Path guiItems = Files.writeString(itemsFolder.resolve("guis.yml"), "customized GUI items");
        Path sampleItems = Files.writeString(itemsFolder.resolve("armors.yml"), "sample items");
        Path customItems = Files.writeString(itemsFolder.resolve("custom.yml"), "custom items");
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();
        Set<Path> defaultItemFiles;

        try (var resources = mockStatic(ResourcesManager.class)) {
            resources.when(() -> ResourcesManager.browseJar(any())).thenAnswer(invocation -> {
                Consumer<ZipEntry> consumer = invocation.getArgument(0);
                consumer.accept(new ZipEntry("items/"));
                consumer.accept(new ZipEntry("items/guis.yml"));
                consumer.accept(new ZipEntry("items/armors.yml"));
                consumer.accept(new ZipEntry("recipes/armors.yml"));
                return null;
            });
            defaultItemFiles = new RemoveDefaultsCommand().deleteKnownDefaultFiles(tempDir, "items",
                    Set.of(guiItems), deletedFiles, failedFiles);
        }

        assertEquals("customized GUI items", Files.readString(guiItems));
        assertEquals("custom items", Files.readString(customItems));
        assertFalse(Files.exists(sampleItems));
        assertEquals(Set.of(sampleItems), defaultItemFiles);
        assertEquals(1, deletedFiles.get());
        assertEquals(0, failedFiles.get());
    }

    @Test
    void removesDefaultInventoryCategoriesAndSavesCustomSettings() throws IOException {
        YamlConfiguration settings = new YamlConfiguration();
        settings.set("inventory-menu.layout.armors.icon", "customized_default_icon");
        settings.set("inventory-menu.layout.weapons.slot", 11);
        settings.set("inventory-menu.layout.custom.icon", "custom_icon");
        settings.set("inventory-menu.title", "Custom title");
        settings.set("inventory-menu.rows", 4);
        Path settingsFile = tempDir.resolve("settings.yml");
        settings.save(settingsFile.toFile());
        AtomicInteger failedFiles = new AtomicInteger();
        Set<Path> defaultItemFiles = Set.of(tempDir.resolve("items/armors.yml"),
                tempDir.resolve("items/weapons.yml"), tempDir.resolve("items/blocks.yml"));

        RemoveDefaultsCommand command = new RemoveDefaultsCommand();
        command.removeDefaultInventoryEntries(settings, settingsFile, defaultItemFiles, failedFiles);

        assertFalse(settings.contains("inventory-menu.layout.armors"));
        assertFalse(settings.contains("inventory-menu.layout.weapons"));
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(settingsFile.toFile());
        assertEquals(Set.of("custom"), saved.getConfigurationSection("inventory-menu.layout").getKeys(false));
        assertEquals("custom_icon", saved.getString("inventory-menu.layout.custom.icon"));
        assertEquals("Custom title", saved.getString("inventory-menu.title"));
        assertEquals(4, saved.getInt("inventory-menu.rows"));
        assertEquals(0, failedFiles.get());

        command.removeDefaultInventoryEntries(settings, settingsFile, defaultItemFiles, failedFiles);
        assertEquals(saved.saveToString(), Files.readString(settingsFile));
        assertEquals(0, failedFiles.get());
    }

    @Test
    void keepsGlobalLanguageOverridesWhenRemovingLanguageDefaults() throws IOException {
        Path languageFolder = Files.createDirectories(tempDir.resolve("pack/lang"));
        Path globalLanguage = Files.writeString(languageFolder.resolve("global.json"), "{\"custom.key\":\"value\"}");
        Path defaultLanguage = Files.writeString(languageFolder.resolve("en_us.json"), "{\"default.key\":\"value\"}");
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();

        new RemoveDefaultsCommand().deletePath(languageFolder, true, Set.of(globalLanguage), deletedFiles, failedFiles);

        assertTrue(Files.exists(languageFolder));
        assertTrue(Files.exists(globalLanguage));
        assertEquals("{\"custom.key\":\"value\"}", Files.readString(globalLanguage));
        assertFalse(Files.exists(defaultLanguage));
        assertEquals(1, deletedFiles.get());
        assertEquals(0, failedFiles.get());
    }

    @Test
    void removesBundledBrandingButPreservesCustomGlobalOverrides() throws IOException {
        Path globalLanguage = Files.writeString(tempDir.resolve("global.json"), """
                {"menu.game":"<glyph:logo>","menu.disconnect":"<shift:-256><gray>See you soon!<shift:-142><glyph:menu_banner><shift:-327>","custom.key":"value","menu.returnToGame":"Custom"}
                """);
        AtomicInteger failedFiles = new AtomicInteger();

        new RemoveDefaultsCommand().removeBundledGlobalLanguageEntries(globalLanguage, failedFiles);

        String sanitized = Files.readString(globalLanguage);
        assertFalse(sanitized.contains("menu.game"));
        assertFalse(sanitized.contains("menu.disconnect"));
        assertTrue(sanitized.contains("custom.key"));
        assertTrue(sanitized.contains("menu.returnToGame"));
        assertEquals(0, failedFiles.get());
    }
}
