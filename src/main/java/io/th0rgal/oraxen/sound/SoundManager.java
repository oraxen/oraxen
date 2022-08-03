package io.th0rgal.oraxen.sound;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;

    public SoundManager(YamlConfiguration soundConfig) {
        autoGenerate = soundConfig.getBoolean("settings.automatically_generate")
                && soundConfig.isConfigurationSection("sounds");
        customSounds = autoGenerate
                ? parseCustomSounds(Objects.requireNonNull(soundConfig.getConfigurationSection("sounds")))
                : new ArrayList<>();
    }

    public Collection<CustomSound> parseCustomSounds(ConfigurationSection section) {
        final Collection<CustomSound> output = new ArrayList<>();
        for (String soundName : section.getKeys(true)) {
            ConfigurationSection sound = section.getConfigurationSection(soundName);
            if (sound == null) continue;
            List<String> sounds = !sound.getStringList("sounds").isEmpty() ?
                    sound.getStringList("sounds") : Collections.singletonList(sound.getString("sound"));
            output.add(new CustomSound(
                    soundName, sounds, sound.getString("category"),
                    sound.getBoolean("replace"), sound.getString("subtitle"))
            );
        }
        return output;
    }

    public Collection<CustomSound> getCustomSounds() {
        return customSounds;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }
}
