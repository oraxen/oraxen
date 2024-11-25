package io.th0rgal.oraxen.sound;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;

    public SoundManager(YamlConfiguration soundConfig) {
        this.autoGenerate = soundConfig.getBoolean("settings.automatically_generate");
        this.customSounds = new ArrayList<>();

        if (autoGenerate) {
            ConfigurationSection soundsSection = soundConfig.getConfigurationSection("sounds");
            if (soundsSection != null) {
                for (String soundName : soundsSection.getKeys(false)) {
                    ConfigurationSection soundSection = soundsSection.getConfigurationSection(soundName);
                    if (soundSection != null) {
                        customSounds.add(new CustomSound(soundName, soundSection));
                    }
                }
            }
        }
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
