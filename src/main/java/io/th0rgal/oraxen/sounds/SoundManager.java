package io.th0rgal.oraxen.sounds;

import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;
    private final Map<Key, String> songKeys;
    private final Map<Key, Float> songRanges;

    public SoundManager(YamlConfiguration soundConfig) {
        this.autoGenerate = soundConfig.contains("settings.generate_sounds")
                ? soundConfig.getBoolean("settings.generate_sounds", true)
                : soundConfig.getBoolean("settings.automatically_generate", true);
        this.customSounds = new ArrayList<>();
        this.songKeys = new HashMap<>();
        this.songRanges = new HashMap<>();

        if (autoGenerate) {
            for (ConfigurationSection soundSection : getSoundSections(soundConfig)) {
                try {
                    CustomSound sound = new CustomSound(soundSection);
                    if (sound.isJukeboxSound()) {
                        Key songKey = Key.key(sound.getJukeboxSongId());
                        Key soundId = Key.key(sound.getSoundId());
                        songKeys.put(songKey, sound.getSoundId());
                        songKeys.put(soundId, sound.getSoundId());
                        if (sound.getRange() != null) {
                            songRanges.put(songKey, sound.getRange());
                            songRanges.put(soundId, sound.getRange());
                        }
                    }
                    customSounds.add(sound);
                } catch (IllegalArgumentException exception) {
                    Logs.logWarning("Skipping invalid sound entry: " + exception.getMessage());
                    Logs.debug(exception);
                }
            }
        }
    }

    public String songKeyToSoundId(Key key) {
        return songKeys.getOrDefault(key, key.toString());
    }

    @Nullable
    public Float jukeboxRange(Key key) {
        return songRanges.get(key);
    }

    public Collection<CustomSound> getCustomSounds() {
        return new ArrayList<>(customSounds);
    }

    public Collection<CustomSound> getJukeboxSounds() {
        return customSounds.stream()
                .filter(CustomSound::isJukeboxSound)
                .toList();
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    private List<ConfigurationSection> getSoundSections(YamlConfiguration soundConfig) {
        Object soundsValue = soundConfig.get("sounds");
        if (soundsValue instanceof List<?> soundsList)
            return getListSoundSections(soundsList);

        ConfigurationSection soundsSection = soundConfig.getConfigurationSection("sounds");
        if (soundsSection == null)
            return List.of();

        List<ConfigurationSection> sections = new ArrayList<>();
        for (SoundConfigMigration.LegacySoundEntry entry : SoundConfigMigration.collectLegacySoundEntries(soundsSection)) {
            ConfigurationSection section = createSection(SoundConfigMigration.toNewSoundMap(entry.id(), entry.section()),
                    "sound_" + sections.size());
            sections.add(section);
        }
        return sections;
    }

    private List<ConfigurationSection> getListSoundSections(List<?> soundsList) {
        List<ConfigurationSection> sections = new ArrayList<>();
        for (Object entry : soundsList) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                Logs.logWarning("Skipping sound entry that is not a map");
                continue;
            }

            ConfigurationSection section = createSection(normalizeMap(entryMap), "sound_" + sections.size());
            sections.add(section);
        }
        return sections;
    }

    private ConfigurationSection createSection(Map<String, Object> values, String path) {
        YamlConfiguration configuration = new YamlConfiguration();
        ConfigurationSection section = configuration.createSection(path);
        copyValues(section, values);
        return section;
    }

    private void copyValues(ConfigurationSection section, Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> childMap) {
                ConfigurationSection childSection = section.createSection(entry.getKey());
                copyValues(childSection, normalizeMap(childMap));
                continue;
            }
            section.set(entry.getKey(), value);
        }
    }

    private Map<String, Object> normalizeMap(Map<?, ?> source) {
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
