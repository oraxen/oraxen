package io.th0rgal.oraxen.sound;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.key.Key;

import java.util.*;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;
    private final Map<Key, String> songKeys;

    public SoundManager(YamlConfiguration soundConfig) {
        this.autoGenerate = soundConfig.getBoolean("settings.automatically_generate");
        this.customSounds = new ArrayList<>();
        this.songKeys = new HashMap<>();

        if (autoGenerate) {
            ConfigurationSection soundsSection = soundConfig.getConfigurationSection("sounds");
            if (soundsSection != null) {
                for (String soundName : soundsSection.getKeys(false)) {
                    ConfigurationSection soundSection = soundsSection.getConfigurationSection(soundName);
                    if (soundSection != null) {
                        CustomSound sound = new CustomSound(soundName, soundSection);
                        if (sound.isJukeboxSound()) {
                            songKeys.put(NamespacedKey.fromString("oraxen:" + sound.getName()), sound.getSoundId());

                        }
                        customSounds.add(sound);
                    }
                }
            }
        }
    }

    public String songKeyToSoundId(Key key) {
        return songKeys.getOrDefault(key, key.toString());
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
}
