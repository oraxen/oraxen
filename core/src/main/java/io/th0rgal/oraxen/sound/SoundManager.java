package io.th0rgal.oraxen.sound;

import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class SoundManager {

    private final boolean autoGenerate;
    private final Collection<CustomSound> customSounds;

    public SoundManager(YamlConfiguration soundConfig) {
        autoGenerate = soundConfig.getBoolean("settings.automatically_generate");
        customSounds = autoGenerate
                ? parseCustomSounds(Objects.requireNonNull(soundConfig.getConfigurationSection("sounds")))
                : new ArrayList<>();
    }

    public Collection<CustomSound> parseCustomSounds(ConfigurationSection section) {
        final Collection<CustomSound> output = new ArrayList<>();
        for (String soundName : section.getKeys(true)) {
            ConfigurationSection sound = section.getConfigurationSection(soundName);
            if (sound == null)
                continue;
            SoundCategory category = null;
            if (sound.isString("category")) {
                try {
                    category = (Objects.requireNonNullElse(
                            SoundCategory.valueOf(sound.getString("category").toUpperCase(Locale.ROOT)),
                            SoundCategory.MASTER));
                } catch (IllegalArgumentException ignored) {
                }
            }
            List<String> sounds = sound.getStringList("sounds").isEmpty()
                    ? Collections.singletonList(sound.getString("sound"))
                    : sound.getStringList("sounds");

            output.add(new CustomSound(soundName, sounds, category, sound.getBoolean("replace"),
                    sound.getString("subtitle"),
                    // by default, stream is false
                    sound.getBoolean("stream", false)));
        }
        return output;
    }

    public Collection<CustomSound> getCustomSounds() {
        return new ArrayList<>(customSounds);
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }
}
