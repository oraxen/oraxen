package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import team.unnamed.creative.sound.SoundEntry;
import team.unnamed.creative.sound.SoundEvent;
import team.unnamed.creative.sound.SoundRegistry;

import java.util.*;

public class SoundManager {

    private final boolean generateSounds;
    private final Collection<SoundRegistry> customSoundRegistries;

    public SoundManager(YamlConfiguration soundConfig) {
        generateSounds = soundConfig.getBoolean("settings.generate_sounds");
        customSoundRegistries = generateSounds
                ? parseCustomSounds(Objects.requireNonNull(soundConfig.getConfigurationSection("sounds")))
                : new ArrayList<>();
    }

    public Collection<SoundRegistry> parseCustomSounds(ConfigurationSection section) {
        final Collection<SoundRegistry> soundRegistries = new ArrayList<>();
        for (String namespace : new LinkedHashSet<>(section.getKeys(false))) {
            final Collection<SoundEvent> soundEvents = new ArrayList<>();
            ConfigurationSection namespaceSection = section.getConfigurationSection(namespace);
            if (namespaceSection == null) continue;
            for (String soundId : new LinkedHashSet<>(namespaceSection.getKeys(true))) {
                Key soundKey = Key.key(namespace, soundId);
                ConfigurationSection soundSection = namespaceSection.getConfigurationSection(soundId);
                if (soundSection == null) continue;

                final List<SoundEntry> soundEntries = new ArrayList<>();

                for (Object object : soundSection.getList("sounds", new ArrayList<>())) {
                    if (object instanceof String soundEntry)
                        soundEntries.add(SoundEntry.soundEntry().key(Key.key(soundEntry)).build());
                    else if (object instanceof Map<?,?> entry) {
                        try {
                            LinkedHashMap<String, Object> soundEntry = (LinkedHashMap<String, Object>) entry;
                            soundEntries.add(SoundEntry.soundEntry().key(Key.key(soundEntry.getOrDefault("id", "").toString()))
                                    .stream((Boolean) soundEntry.getOrDefault("stream", SoundEntry.DEFAULT_STREAM))
                                    .attenuationDistance((Integer) soundEntry.getOrDefault("attenuation_distance", SoundEntry.DEFAULT_ATTENUATION_DISTANCE))
                                    .pitch(Math.max(ParseUtils.parseFloat(soundEntry.getOrDefault("pitch", "").toString(), SoundEntry.DEFAULT_PITCH), 0.00001f))
                                    .volume(Math.max(ParseUtils.parseFloat(soundEntry.getOrDefault("volume", "").toString(), SoundEntry.DEFAULT_VOLUME), 0.00001f))
                                    .preload((Boolean) soundEntry.getOrDefault("preload", SoundEntry.DEFAULT_PRELOAD))
                                    .type(Arrays.stream(SoundEntry.Type.values()).filter(t -> t.name().equals(soundEntry.get("type"))).findFirst().orElse(SoundEntry.DEFAULT_TYPE))
                                    .weight((Integer) soundEntry.getOrDefault("weight", SoundEntry.DEFAULT_WEIGHT))
                                    .build()
                            );
                        } catch (Exception e) {
                            Logs.logWarning("Failed to parse sound-entry " + soundId);
                            if (Settings.DEBUG.toBool()) e.printStackTrace();
                        }
                    } else Logs.logWarning("Failed to parse sound-entry " + soundId);
                }

                SoundEvent soundEvent = SoundEvent.soundEvent().key(soundKey)
                        .sounds(soundEntries)
                        .replace(soundSection.getBoolean("replace", SoundEvent.DEFAULT_REPLACE))
                        .subtitle(soundSection.getString("subtitle")).build();

                // Skip soundEvents where it is "empty"
                if (soundEvent.equals(SoundEvent.soundEvent().key(soundKey).build())) continue;
                soundEvents.add(soundEvent);
            }

            soundRegistries.add(SoundRegistry.soundRegistry(namespace, soundEvents));
        }

        return soundRegistries;
    }

    public Collection<SoundRegistry> customSoundRegistries() {
        return new LinkedHashSet<>(customSoundRegistries);
    }

    public boolean generateSounds() {
        return generateSounds;
    }
}
