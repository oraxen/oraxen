package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.configs.ResourcesManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        settings.set("inventory.layout.armors.icon", "customized_default_icon");
        settings.set("inventory.layout.weapons.slot", 11);
        settings.set("inventory.layout.custom.icon", "custom_icon");
        settings.set("inventory.title", "Custom title");
        settings.set("inventory.rows", 4);
        Path settingsFile = tempDir.resolve("settings.yml");
        settings.save(settingsFile.toFile());
        AtomicInteger failedFiles = new AtomicInteger();
        Set<Path> defaultItemFiles = Set.of(tempDir.resolve("items/armors.yml"),
                tempDir.resolve("items/weapons.yml"), tempDir.resolve("items/blocks.yml"));

        RemoveDefaultsCommand command = new RemoveDefaultsCommand();
        command.removeDefaultInventoryEntries(settings, settingsFile, defaultItemFiles, failedFiles);

        assertFalse(settings.contains("inventory.layout.armors"));
        assertFalse(settings.contains("inventory.layout.weapons"));
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(settingsFile.toFile());
        assertEquals(Set.of("custom"), saved.getConfigurationSection("inventory.layout").getKeys(false));
        assertEquals("custom_icon", saved.getString("inventory.layout.custom.icon"));
        assertEquals("Custom title", saved.getString("inventory.title"));
        assertEquals(4, saved.getInt("inventory.rows"));
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

    @Test
    void removesUnchangedBundledSoundsAndPreservesEditedAndCustomEntries() throws IOException {
        YamlConfiguration bundledSounds = new YamlConfiguration();
        bundledSounds.set("sounds", List.of(
                Map.of("id", "oraxen:welcome", "sound", "welcome.ogg", "stream", true),
                Map.of("id", "block.wood.step", "replace", true),
                Map.of("id", "required.wood.hit", "subtitle", "subtitles.block.generic.hit",
                        "sounds", List.of("step/wood1", "step/wood2"))));

        YamlConfiguration sounds = new YamlConfiguration();
        sounds.set("settings.automatically_generate", true);
        sounds.set("sounds", List.of(
                Map.of("id", "oraxen:welcome", "sound", "welcome.ogg", "stream", true),
                Map.of("id", "required.wood.hit", "subtitle", "subtitles.block.generic.hit",
                        "sounds", List.of("step/wood1", "step/wood2")),
                Map.of("id", "block.wood.step", "replace", true, "sound", "custom_step.ogg"),
                Map.of("id", "example:custom", "sound", "custom.ogg")));
        Path soundsFile = tempDir.resolve("sounds.yml");
        sounds.save(soundsFile.toFile());
        AtomicInteger failedFiles = new AtomicInteger();

        RemoveDefaultsCommand.SoundCleanupResult result = new RemoveDefaultsCommand()
                .removeBundledSoundEntries(sounds, bundledSounds, soundsFile, failedFiles);

        assertTrue(result.ready());
        assertEquals(2, result.removedEntries());
        assertEquals(Set.of("minecraft:custom_step", "minecraft:custom"), result.retainedSoundFiles());
        assertEquals(List.of(
                Map.of("id", "block.wood.step", "replace", true, "sound", "custom_step.ogg"),
                Map.of("id", "example:custom", "sound", "custom.ogg")), sounds.getMapList("sounds"));
        assertTrue(sounds.getBoolean("settings.automatically_generate"));
        assertEquals(0, failedFiles.get());
    }

    @Test
    void keepsEmptySoundListAfterRemovingEveryBundledEntry() throws IOException {
        YamlConfiguration bundledSounds = new YamlConfiguration();
        bundledSounds.set("sounds", List.of(Map.of("id", "oraxen:welcome", "sound", "welcome.ogg")));
        YamlConfiguration sounds = new YamlConfiguration();
        sounds.set("settings.automatically_generate", true);
        sounds.set("sounds", List.of(Map.of("id", "oraxen:welcome", "sound", "welcome.ogg")));
        Path soundsFile = tempDir.resolve("sounds.yml");
        sounds.save(soundsFile.toFile());
        AtomicInteger failedFiles = new AtomicInteger();

        RemoveDefaultsCommand command = new RemoveDefaultsCommand();
        RemoveDefaultsCommand.SoundCleanupResult firstRun = command
                .removeBundledSoundEntries(sounds, bundledSounds, soundsFile, failedFiles);
        YamlConfiguration savedSounds = YamlConfiguration.loadConfiguration(soundsFile.toFile());
        RemoveDefaultsCommand.SoundCleanupResult secondRun = command
                .removeBundledSoundEntries(savedSounds, bundledSounds, soundsFile, failedFiles);

        assertEquals(1, firstRun.removedEntries());
        assertTrue(firstRun.ready());
        assertTrue(savedSounds.get("sounds") instanceof List<?>);
        assertTrue(savedSounds.getMapList("sounds").isEmpty());
        assertEquals(0, secondRun.removedEntries());
        assertTrue(secondRun.ready());
        assertEquals(0, failedFiles.get());
    }

    @Test
    void saveFailureKeepsOriginalSoundEntriesAndPreventsAssetCleanup() throws IOException {
        Map<String, Object> bundledWelcome = Map.of("id", "oraxen:welcome", "sound", "welcome.ogg");
        YamlConfiguration bundledSounds = new YamlConfiguration();
        bundledSounds.set("sounds", List.of(bundledWelcome));
        YamlConfiguration sounds = new YamlConfiguration();
        sounds.set("sounds", List.of(bundledWelcome));
        Path soundsFile = Files.createDirectory(tempDir.resolve("sounds.yml"));
        Path soundFolder = Files.createDirectories(tempDir.resolve("pack/sounds"));
        Path bundledWelcomeFile = Files.write(soundFolder.resolve("welcome.ogg"), new byte[] {1});
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();

        RemoveDefaultsCommand command = new RemoveDefaultsCommand();
        RemoveDefaultsCommand.SoundCleanupResult result;
        try (var logs = mockStatic(Logs.class)) {
            result = command.removeBundledSoundEntries(sounds, bundledSounds, soundsFile, failedFiles);
        }
        command.deleteBundledSoundFilesIfReady(result, soundFolder, deletedFiles, failedFiles);

        assertFalse(result.ready());
        assertEquals(0, result.removedEntries());
        assertEquals(List.of(bundledWelcome), sounds.getMapList("sounds"));
        assertTrue(Files.exists(bundledWelcomeFile));
        assertEquals(0, deletedFiles.get());
        assertEquals(1, failedFiles.get());
    }

    @Test
    void deletesOnlyUnreferencedBundledSounds() throws IOException {
        // The system temp path can contain symlinks such as /var on macOS.
        Path soundsFolder = Files.createDirectories(tempDir.toRealPath().resolve("pack/sounds"));
        Path unusedBundledSound = Files.write(soundsFolder.resolve("welcome.ogg"), new byte[] {1});
        Path referencedBundledSound = Files.write(soundsFolder.resolve("custom_step.ogg"), new byte[] {2});
        Path customSound = Files.write(soundsFolder.resolve("server_custom.ogg"), new byte[] {3});
        Path bundledJar = tempDir.resolve("oraxen.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bundledJar))) {
            for (String entry : List.of("pack/sounds/", "pack/sounds/welcome.ogg",
                    "pack/sounds/custom_step.ogg")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.closeEntry();
            }
        }
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();

        new RemoveDefaultsCommand().deleteBundledSoundFiles(soundsFolder, Set.of("minecraft:custom_step"),
                bundledJar, deletedFiles, failedFiles);

        assertFalse(Files.exists(unusedBundledSound));
        assertTrue(Files.exists(referencedBundledSound));
        assertTrue(Files.exists(customSound));
        assertEquals(1, deletedFiles.get());
        assertEquals(0, failedFiles.get());
    }

    @Test
    void keepsSoundFilesAndReportsFailureWhenBundledJarCannotBeRead() throws IOException {
        Path soundsFolder = Files.createDirectories(tempDir.resolve("pack/sounds"));
        Path bundledSound = Files.write(soundsFolder.resolve("welcome.ogg"), new byte[] {1});
        Path invalidJar = Files.writeString(tempDir.resolve("oraxen.jar"), "not a zip file");
        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedFiles = new AtomicInteger();

        try (var logs = mockStatic(Logs.class)) {
            new RemoveDefaultsCommand().deleteBundledSoundFiles(soundsFolder, Set.of(), invalidJar,
                    deletedFiles, failedFiles);
        }

        assertTrue(Files.exists(bundledSound));
        assertEquals(0, deletedFiles.get());
        assertEquals(1, failedFiles.get());
    }
}
