package io.th0rgal.oraxen.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.ResourcesManager;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.sounds.CustomSound;
import io.th0rgal.oraxen.sounds.SoundManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RemoveDefaultsCommand {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    record SoundCleanupResult(int removedEntries, Set<String> retainedSoundFiles, boolean ready) {
    }

    OraxenCommand getRemoveDefaultsCommand() {
        OraxenCommand confirmed = new OraxenCommand("confirm")
                .withPermission("oraxen.command.remove-defaults")
                .executes((sender, args) -> {
                    removeDefaults(sender);
                });
        return new OraxenCommand("remove-defaults")
                .withPermission("oraxen.command.remove-defaults")
                .withSubcommand(confirmed)
                .executes((sender, args) -> {
                    Message.REMOVE_DEFAULTS_CONFIRM.send(sender);
                });
    }

    private void removeDefaults(CommandSender sender) {
        Settings.GENERATE_DEFAULT_CONFIGS.setValue(false);
        Settings.GENERATE_DEFAULT_ASSETS.setValue(false);
        Settings.RECEIVE_LOADED_SOUND.setValue(false);

        AtomicInteger deletedFiles = new AtomicInteger();
        AtomicInteger failedOperations = new AtomicInteger();
        Path dataFolder = OraxenPlugin.get().getDataFolder().toPath();

        SoundCleanupResult soundCleanup = removeBundledSoundEntries(
                dataFolder.resolve("sounds.yml"), failedOperations);
        deleteBundledSoundFilesIfReady(soundCleanup, dataFolder.resolve("pack/sounds"),
                deletedFiles, failedOperations);

        deletePath(dataFolder.resolve("pack/textures/default"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("pack/models/default"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("pack/textures/animations"), false, deletedFiles, failedOperations);

        Path languageFolder = dataFolder.resolve("pack/lang");
        deletePath(languageFolder, true, Set.of(languageFolder.resolve("global.json")), deletedFiles, failedOperations);
        removeBundledGlobalLanguageEntries(languageFolder.resolve("global.json"), failedOperations);
        deletePath(dataFolder.resolve("pack/font"), true, deletedFiles, failedOperations);
        deleteKnownDefaultFiles(dataFolder, "recipes", Set.of(), deletedFiles, failedOperations);
        // Navigation icons are required by the item and recipe menus, not sample content.
        Set<Path> defaultItemFiles = deleteKnownDefaultFiles(dataFolder, "items",
                Set.of(dataFolder.resolve("items/guis.yml")), deletedFiles, failedOperations);
        removeDefaultInventoryEntries(OraxenPlugin.get().getConfigsManager().getSettings(),
                dataFolder.resolve("settings.yml"), defaultItemFiles, failedOperations);

        deletePath(dataFolder.resolve("glyphs/animations.yml"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("glyphs/chat_tags.yml"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("glyphs/emoji.yml"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("glyphs/animations"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("glyphs/chat_tags"), false, deletedFiles, failedOperations);
        deletePath(dataFolder.resolve("glyphs/emoji"), false, deletedFiles, failedOperations);

        if (failedOperations.get() > 0) {
            Message.REMOVE_DEFAULTS_FAILED.send(sender,
                    AdventureUtils.tagResolver("deleted", String.valueOf(deletedFiles.get())),
                    AdventureUtils.tagResolver("failed", String.valueOf(failedOperations.get())),
                    AdventureUtils.tagResolver("sounds", String.valueOf(soundCleanup.removedEntries())));
            return;
        }

        Message.REMOVE_DEFAULTS_SUCCESS.send(sender,
                AdventureUtils.tagResolver("files", String.valueOf(deletedFiles.get())),
                AdventureUtils.tagResolver("sounds", String.valueOf(soundCleanup.removedEntries())));
    }

    SoundCleanupResult removeBundledSoundEntries(Path soundsFile, AtomicInteger failedOperations) {
        YamlConfiguration soundConfig = new YamlConfiguration();
        soundConfig.options().parseComments(true);
        try {
            soundConfig.load(soundsFile.toFile());
        } catch (IOException | InvalidConfigurationException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to load sounds.yml before removing bundled sound defaults");
            Logs.debug(exception);
            return new SoundCleanupResult(0, Set.of(), false);
        }

        YamlConfiguration bundledSoundConfig;
        try (InputStream input = OraxenPlugin.get().getResource("sounds.yml")) {
            if (input == null)
                throw new IOException("Bundled sounds.yml is missing");
            bundledSoundConfig = OraxenYaml.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to load bundled sound defaults");
            Logs.debug(exception);
            return new SoundCleanupResult(0, Set.of(), false);
        }

        return removeBundledSoundEntries(soundConfig, bundledSoundConfig, soundsFile, failedOperations);
    }

    SoundCleanupResult removeBundledSoundEntries(YamlConfiguration soundConfig,
            YamlConfiguration bundledSoundConfig, Path soundsFile, AtomicInteger failedOperations) {
        Object currentSoundValue = soundConfig.get("sounds");
        Object bundledSoundValue = bundledSoundConfig.get("sounds");
        if (!(currentSoundValue instanceof List<?> currentSounds)
                || !(bundledSoundValue instanceof List<?> bundledSounds)) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Could not remove bundled sound entries because sounds.yml has no sound list");
            return new SoundCleanupResult(0, Set.of(), false);
        }

        Set<Map<String, Object>> bundledEntries = new HashSet<>();
        for (Object bundledSound : bundledSounds)
            if (bundledSound instanceof Map<?, ?> bundledMap)
                bundledEntries.add(normalizeSoundMap(bundledMap));

        List<Object> retainedSounds = new ArrayList<>();
        int removedEntries = 0;
        for (Object sound : currentSounds) {
            if (sound instanceof Map<?, ?> soundMap && bundledEntries.contains(normalizeSoundMap(soundMap))) {
                removedEntries++;
            } else {
                retainedSounds.add(sound);
            }
        }

        if (removedEntries > 0) {
            soundConfig.set("sounds", retainedSounds);
            try {
                soundConfig.save(soundsFile.toFile());
            } catch (IOException exception) {
                soundConfig.set("sounds", currentSounds);
                failedOperations.incrementAndGet();
                Logs.logWarning("Failed to save sounds.yml while removing bundled sound entries");
                Logs.debug(exception);
                return new SoundCleanupResult(0, Set.of(), false);
            }
        }

        Set<String> retainedSoundFiles = collectRetainedSoundFiles(retainedSounds);
        if (retainedSoundFiles == null) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Could not resolve retained sound references; keeping bundled sound files");
            return new SoundCleanupResult(removedEntries, Set.of(), false);
        }
        return new SoundCleanupResult(removedEntries, retainedSoundFiles, true);
    }

    private Map<String, Object> normalizeSoundMap(Map<?, ?> soundMap) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : soundMap.entrySet())
            normalized.put(String.valueOf(entry.getKey()), normalizeSoundValue(entry.getValue()));
        return normalized;
    }

    private Object normalizeSoundValue(Object value) {
        if (value instanceof Map<?, ?> map)
            return normalizeSoundMap(map);
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object entry : list)
                normalized.add(normalizeSoundValue(entry));
            return normalized;
        }
        return value;
    }

    private Set<String> collectRetainedSoundFiles(List<?> retainedSounds) {
        YamlConfiguration parserConfig = new YamlConfiguration();
        parserConfig.set("settings.automatically_generate", true);
        parserConfig.set("settings.generate_sounds", true);
        parserConfig.set("sounds", retainedSounds);

        List<String> warnings = new ArrayList<>();
        SoundManager soundManager = new SoundManager(parserConfig, warnings::add);
        if (!warnings.isEmpty())
            return null;

        Set<String> soundFiles = new HashSet<>();
        for (CustomSound sound : soundManager.getCustomSounds()) {
            for (String reference : sound.getSounds()) {
                int namespaceSeparator = reference.indexOf(':');
                String namespace = namespaceSeparator >= 0
                        ? reference.substring(0, namespaceSeparator)
                        : sound.getNamespace();
                if (!namespace.equals("minecraft"))
                    continue;

                String path = namespaceSeparator >= 0
                        ? reference.substring(namespaceSeparator + 1)
                        : reference;
                String normalizedPath = normalizeSoundPath(path);
                if (normalizedPath == null)
                    return null;
                soundFiles.add("minecraft:" + normalizedPath);
            }
        }
        return soundFiles;
    }

    private String normalizeSoundPath(String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.startsWith("/"))
            return null;
        List<String> normalizedSegments = new ArrayList<>();
        for (String segment : normalized.split("/")) {
            if (segment.equals(".."))
                return null;
            if (!segment.isBlank() && !segment.equals("."))
                normalizedSegments.add(segment);
        }
        if (normalizedSegments.isEmpty())
            return null;
        normalized = String.join("/", normalizedSegments);
        if (normalized.endsWith(".ogg"))
            normalized = normalized.substring(0, normalized.length() - 4);
        return normalized.isBlank() ? null : normalized;
    }

    void deleteBundledSoundFiles(Path soundFolder, Set<String> retainedSoundFiles,
            AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        Path bundledJar;
        try {
            bundledJar = Path.of(OraxenPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to locate bundled sound files in the Oraxen JAR");
            Logs.debug(exception);
            return;
        }
        deleteBundledSoundFiles(soundFolder, retainedSoundFiles, bundledJar, deletedFiles, failedOperations);
    }

    void deleteBundledSoundFiles(Path soundFolder, Set<String> retainedSoundFiles, Path bundledJar,
            AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        Path normalizedSoundFolder = soundFolder.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalizedSoundFolder)) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Skipped bundled sound cleanup because pack/sounds is a symbolic link");
            return;
        }
        if (!Files.isDirectory(normalizedSoundFolder, LinkOption.NOFOLLOW_LINKS))
            return;

        Set<Path> bundledSoundFiles = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(bundledJar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName().replace('\\', '/');
                String resourcePrefix = "pack/sounds/";
                if (entry.isDirectory() || !entryName.startsWith(resourcePrefix))
                    continue;

                Path relative = Path.of(entryName.substring(resourcePrefix.length())).normalize();
                if (relative.isAbsolute() || relative.startsWith(".."))
                    continue;
                Path bundledSoundFile = normalizedSoundFolder.resolve(relative).normalize();
                if (bundledSoundFile.startsWith(normalizedSoundFolder))
                    bundledSoundFiles.add(bundledSoundFile);
            }
        } catch (IOException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to enumerate bundled sound files in the Oraxen JAR");
            Logs.debug(exception);
            return;
        }

        for (Path bundledSoundFile : bundledSoundFiles) {
            if (!Files.isRegularFile(bundledSoundFile, LinkOption.NOFOLLOW_LINKS))
                continue;
            if (hasSymbolicLinkBetween(bundledSoundFile)) {
                failedOperations.incrementAndGet();
                Logs.logWarning("Skipped bundled sound cleanup because its path contains a symbolic link");
                continue;
            }
            Path relative = normalizedSoundFolder.relativize(bundledSoundFile);
            String relativePath = relative.toString().replace('\\', '/');
            String normalizedPath = normalizeSoundPath(relativePath);
            if (normalizedPath != null && retainedSoundFiles.contains("minecraft:" + normalizedPath))
                continue;
            deleteFile(bundledSoundFile, deletedFiles, failedOperations);
        }
    }

    void deleteBundledSoundFilesIfReady(SoundCleanupResult soundCleanup, Path soundFolder,
            AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        if (soundCleanup.ready())
            deleteBundledSoundFiles(soundFolder, soundCleanup.retainedSoundFiles(), deletedFiles, failedOperations);
    }

    private boolean hasSymbolicLinkBetween(Path file) {
        for (Path path = file; path != null; path = path.getParent())
            if (Files.isSymbolicLink(path))
                return true;
        return false;
    }

    void removeBundledGlobalLanguageEntries(Path globalLanguage, AtomicInteger failedOperations) {
        if (!Files.isRegularFile(globalLanguage)) return;

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(globalLanguage, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return;

            JsonObject language = parsed.getAsJsonObject();
            boolean updated = removeIfBundled(language, "menu.game", "<glyph:logo>");
            updated |= removeIfBundled(language, "menu.disconnect",
                    "<shift:-256><gray>See you soon!<shift:-142><glyph:menu_banner><shift:-327>");
            if (updated)
                Files.writeString(globalLanguage, GSON.toJson(language) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException | IllegalArgumentException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to remove bundled language entries from " + globalLanguage.getFileName());
            Logs.debug(exception);
        }
    }

    private boolean removeIfBundled(JsonObject language, String key, String bundledValue) {
        JsonElement value = language.get(key);
        if (value == null || !value.isJsonPrimitive() || !bundledValue.equals(value.getAsString())) return false;
        language.remove(key);
        return true;
    }

    void removeDefaultInventoryEntries(YamlConfiguration settings, Path settingsFile, Set<Path> defaultItemFiles,
            AtomicInteger failedOperations) {
        boolean updated = false;
        for (Path defaultItemFile : defaultItemFiles) {
            String fileName = defaultItemFile.getFileName().toString();
            if (!fileName.endsWith(".yml")) continue;

            String path = "inventory.layout." + fileName.substring(0, fileName.length() - 4);
            if (!settings.contains(path)) continue;
            settings.set(path, null);
            updated = true;
        }
        if (!updated) return;

        Settings.invalidateCache();
        try {
            settings.save(settingsFile.toFile());
        } catch (IOException exception) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to remove default inventory entries from settings.yml");
            Logs.debug(exception);
        }
    }

    Set<Path> deleteKnownDefaultFiles(Path dataFolder, String folder, Set<Path> excludedPaths,
            AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        Set<Path> defaultFiles = new HashSet<>();
        ResourcesManager.browseJar(entry -> {
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.startsWith(folder + "/")
                    && !excludedPaths.contains(dataFolder.resolve(entryName)))
                defaultFiles.add(dataFolder.resolve(entryName));
        });

        for (Path defaultFile : defaultFiles)
            if (Files.exists(defaultFile))
                deleteFile(defaultFile, deletedFiles, failedOperations);
        return defaultFiles;
    }

    private void deletePath(Path path, boolean keepRoot, AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        deletePath(path, keepRoot, Set.of(), deletedFiles, failedOperations);
    }

    void deletePath(Path path, boolean keepRoot, Set<Path> excludedPaths, AtomicInteger deletedFiles,
            AtomicInteger failedOperations) {
        if (!Files.exists(path))
            return;

        try {
            if (Files.isRegularFile(path)) {
                if (!excludedPaths.contains(path)) {
                    Files.delete(path);
                    deletedFiles.incrementAndGet();
                }
                return;
            }

            if (!Files.isDirectory(path))
                return;

            try (Stream<Path> files = Files.walk(path)) {
                files.filter(file -> !keepRoot || !file.equals(path))
                        .filter(file -> !excludedPaths.contains(file))
                        .sorted(Comparator.reverseOrder())
                        .forEach(file -> deleteFile(file, deletedFiles, failedOperations));
            }
        } catch (IOException e) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to delete default path: " + path.getFileName());
            if (Settings.DEBUG.toBool())
                Logs.debug(e);
        }
    }

    private void deleteFile(Path path, AtomicInteger deletedFiles, AtomicInteger failedOperations) {
        try {
            Files.deleteIfExists(path);
            deletedFiles.incrementAndGet();
        } catch (IOException e) {
            failedOperations.incrementAndGet();
            Logs.logWarning("Failed to delete default file: " + path.getFileName());
            if (Settings.DEBUG.toBool())
                Logs.debug(e);
        }
    }
}
