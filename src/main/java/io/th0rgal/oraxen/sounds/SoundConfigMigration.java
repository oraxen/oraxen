package io.th0rgal.oraxen.sounds;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for migrating the legacy sound.yml map format to the sounds.yml list format.
 */
public final class SoundConfigMigration {

    private static final Set<String> SOUND_DEFINITION_KEYS = Set.of(
            "sound",
            "sounds",
            "subtitle",
            "replace",
            "category",
            "stream",
            "volume",
            "pitch",
            "weight",
            "attenuation_distance",
            "preload",
            "jukebox",
            "jukebox_song"
    );

    private SoundConfigMigration() {
    }

    public record LegacySoundEntry(@NotNull String id, @NotNull ConfigurationSection section) {
    }

    /**
     * Migrates a loaded sound configuration in-place when it still uses the legacy map format.
     *
     * @return true when the configuration was changed and should be saved
     */
    public static boolean migrateToNewFormat(@NotNull YamlConfiguration configuration) {
        Object soundsValue = configuration.get("sounds");

        if (soundsValue instanceof List<?> soundsList)
            return migrateListEntries(configuration, soundsList);

        ConfigurationSection soundsSection = configuration.getConfigurationSection("sounds");
        if (soundsSection == null)
            return false;

        List<Map<String, Object>> migratedSounds = new ArrayList<>();
        for (LegacySoundEntry entry : collectLegacySoundEntries(soundsSection))
            migratedSounds.add(toNewSoundMap(entry.id(), entry.section()));

        configuration.set("sounds", migratedSounds);
        return true;
    }

    /**
     * Merges sounds from {@code source} into {@code target}. Both configurations are normalized
     * to the new list format first. Existing sound ids in {@code target} are kept.
     *
     * @return true when {@code target} was changed and should be saved
     */
    public static boolean mergeSounds(@NotNull YamlConfiguration target, @NotNull YamlConfiguration source) {
        boolean changed = migrateToNewFormat(target);
        migrateToNewFormat(source);

        List<Map<String, Object>> targetSounds = getNormalizedSoundMaps(target);
        Set<String> targetIds = getSoundIds(targetSounds);
        boolean merged = false;

        for (Map<String, Object> sourceSound : getNormalizedSoundMaps(source)) {
            Object id = sourceSound.get("id");
            if (id != null && containsSoundId(targetIds, String.valueOf(id)))
                continue;

            targetSounds.add(sourceSound);
            if (id != null)
                addSoundId(targetIds, String.valueOf(id));
            merged = true;
        }

        if (merged)
            target.set("sounds", targetSounds);

        return changed || merged;
    }

    @NotNull
    public static List<LegacySoundEntry> collectLegacySoundEntries(@NotNull ConfigurationSection soundsSection) {
        List<LegacySoundEntry> entries = new ArrayList<>();
        collectLegacySoundEntries(soundsSection, "", entries);
        return entries;
    }

    private static void collectLegacySoundEntries(@NotNull ConfigurationSection section, @NotNull String prefix,
            @NotNull List<LegacySoundEntry> entries) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child == null)
                continue;

            String id = prefix.isBlank() ? key : prefix + "." + key;
            if (isSoundDefinition(child)) {
                entries.add(new LegacySoundEntry(id, child));
                continue;
            }

            collectLegacySoundEntries(child, id, entries);
        }
    }

    public static boolean isSoundDefinition(@NotNull ConfigurationSection section) {
        for (String key : section.getKeys(false))
            if (SOUND_DEFINITION_KEYS.contains(key))
                return true;
        return false;
    }

    @NotNull
    public static Map<String, Object> toNewSoundMap(@NotNull String id, @NotNull ConfigurationSection section) {
        Map<String, Object> soundMap = new LinkedHashMap<>();
        soundMap.put("id", id);

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection childSection) {
                if (key.equals("jukebox_song")) {
                    if (!soundMap.containsKey("jukebox"))
                        soundMap.put("jukebox", migrateJukeboxSongSection(childSection));
                } else {
                    soundMap.put(key, sectionToMap(childSection));
                }
                continue;
            }

            soundMap.put(key, value);
        }

        return soundMap;
    }

    @NotNull
    private static Map<String, Object> migrateJukeboxSongSection(@NotNull ConfigurationSection section) {
        Map<String, Object> jukeboxMap = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection childSection)
                value = sectionToMap(childSection);

            if (key.equals("length_in_seconds"))
                jukeboxMap.put("duration", toDurationValue(value));
            else
                jukeboxMap.put(key, value);
        }
        return jukeboxMap;
    }

    @NotNull
    private static Map<String, Object> sectionToMap(@NotNull ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection childSection)
                value = sectionToMap(childSection);
            map.put(key, value);
        }
        return map;
    }

    @NotNull
    private static List<Map<String, Object>> getNormalizedSoundMaps(@NotNull YamlConfiguration configuration) {
        List<Map<String, Object>> normalizedSounds = new ArrayList<>();
        for (Map<?, ?> sound : configuration.getMapList("sounds"))
            normalizedSounds.add(normalizeMap(sound));
        return normalizedSounds;
    }

    @NotNull
    private static Set<String> getSoundIds(@NotNull List<Map<String, Object>> sounds) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> sound : sounds) {
            Object id = sound.get("id");
            if (id != null)
                addSoundId(ids, String.valueOf(id));
        }
        return ids;
    }

    private static boolean containsSoundId(@NotNull Set<String> ids, @NotNull String id) {
        if (ids.contains(id))
            return true;
        if (id.startsWith("minecraft:"))
            return ids.contains(id.substring("minecraft:".length()));
        return ids.contains("minecraft:" + id);
    }

    private static void addSoundId(@NotNull Set<String> ids, @NotNull String id) {
        ids.add(id);
        if (id.startsWith("minecraft:"))
            ids.add(id.substring("minecraft:".length()));
        else if (!id.contains(":"))
            ids.add("minecraft:" + id);
    }

    private static boolean migrateListEntries(@NotNull YamlConfiguration configuration, @NotNull List<?> soundsList) {
        boolean changed = false;
        List<Map<String, Object>> migratedSounds = new ArrayList<>();

        for (Object entry : soundsList) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                migratedSounds.add(Map.of());
                continue;
            }

            Map<String, Object> soundMap = normalizeMap(entryMap);
            Object legacyJukebox = soundMap.remove("jukebox_song");
            if (legacyJukebox != null && !soundMap.containsKey("jukebox")) {
                soundMap.put("jukebox", migrateJukeboxSongValue(legacyJukebox));
                changed = true;
            } else if (legacyJukebox != null) {
                changed = true;
            }

            migratedSounds.add(soundMap);
        }

        if (changed)
            configuration.set("sounds", migratedSounds);

        return changed;
    }

    @NotNull
    private static Map<String, Object> migrateJukeboxSongValue(@NotNull Object value) {
        if (value instanceof ConfigurationSection section)
            return migrateJukeboxSongSection(section);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> jukeboxMap = normalizeMap(map);
            Object length = jukeboxMap.remove("length_in_seconds");
            if (length != null)
                jukeboxMap.put("duration", toDurationValue(length));
            return jukeboxMap;
        }
        return Map.of();
    }

    @NotNull
    private static Object toDurationValue(@NotNull Object value) {
        if (value instanceof String string && string.matches(".*[a-zA-Z]$"))
            return string;
        return value + "s";
    }

    @NotNull
    private static Map<String, Object> normalizeMap(@NotNull Map<?, ?> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null)
                continue;

            Object value = entry.getValue();
            if (value instanceof Map<?, ?> childMap)
                value = normalizeMap(childMap);
            normalized.put(String.valueOf(entry.getKey()), value);
        }
        return normalized;
    }
}
