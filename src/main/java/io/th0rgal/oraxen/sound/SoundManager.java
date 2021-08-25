package io.th0rgal.oraxen.sound;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collection;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;

    public SoundManager(YamlConfiguration soundConfig) {
        autoGenerate = soundConfig.getBoolean("settings.automatically_generate")
                && soundConfig.isConfigurationSection("sounds");
        customSounds = autoGenerate
                ? parseCustomSounds(soundConfig.getConfigurationSection("sounds"))
                : new ArrayList<>();
    }

    public Collection<CustomSound> parseCustomSounds(ConfigurationSection section) {
        final Collection<CustomSound> output = new ArrayList<>();
        for (String soundName : section.getKeys(false)) {
            ConfigurationSection sound = section.getConfigurationSection(soundName);
            output.add(new CustomSound(soundName, sound.getString("sound"), sound.getString("category")));
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
