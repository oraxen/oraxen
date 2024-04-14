package io.th0rgal.oraxen.sound;

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
        for (String namespace : section.getKeys(false)) {
            final Collection<SoundEvent> soundEvents = new ArrayList<>();
            ConfigurationSection namespaceSection = section.getConfigurationSection(namespace);
            if (namespaceSection == null) continue;
            for (String soundId : namespaceSection.getKeys(true)) {
                Key soundKey = Key.key(namespace, soundId);
                ConfigurationSection soundSection = namespaceSection.getConfigurationSection(soundId);
                if (soundSection == null) continue;

                final List<SoundEntry> soundEntries = new ArrayList<>();

                for (Object object : soundSection.getList("sounds", new ArrayList<>())) {
                    if (object instanceof String soundEntry)
                        soundEntries.add(SoundEntry.soundEntry().key(Key.key(soundEntry)).build());
                    else if (object instanceof ConfigurationSection soundEntry) {
                        soundEntries.add(SoundEntry.soundEntry().key(Key.key(soundEntry.getString("id")))
                                .stream(soundEntry.getBoolean("stream", SoundEntry.DEFAULT_STREAM))
                                .attenuationDistance(soundEntry.getInt("attenuation_distance", SoundEntry.DEFAULT_ATTENUATION_DISTANCE))
                                .pitch((float) soundEntry.getDouble("pitch"))
                                .volume((float) soundEntry.getDouble("volume"))
                                .preload(soundEntry.getBoolean("preload", SoundEntry.DEFAULT_PRELOAD))
                                .type(Arrays.stream(SoundEntry.Type.values()).filter(t -> t.equals(soundEntry.getString("type"))).findFirst().orElse(SoundEntry.DEFAULT_TYPE))
                                .weight(soundEntry.getInt("weight", SoundEntry.DEFAULT_WEIGHT))
                                .build()
                        );
                    }
                }

                SoundEvent soundEvent = SoundEvent.soundEvent().key(soundKey)
                        .sounds(soundEntries)
                        .replace(soundSection.getBoolean("replace", SoundEvent.DEFAULT_REPLACE))
                        .subtitle(soundSection.getString("subtitle")).build();

                // Skip soundEvents where it is "empty"
                if (soundEvent.equals(SoundEvent.soundEvent(soundKey, SoundEvent.DEFAULT_REPLACE, null, new ArrayList<>()))) continue;
                soundEvents.add(soundEvent);
            }

            soundRegistries.add(SoundRegistry.soundRegistry(namespace, soundEvents));
        }

        return soundRegistries;
    }

    public Collection<SoundRegistry> customSoundRegistries() {
        return new ArrayList<>(customSoundRegistries);
    }

    public boolean generateSounds() {
        return generateSounds;
    }
}
