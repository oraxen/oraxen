package io.th0rgal.oraxen.sounds;

import net.kyori.adventure.key.Key;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundManagerTest {

    @Test
    void loadsNewListFormatAndJukeboxMappings() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                settings:
                  automatically_generate: true
                sounds:
                  - id: oraxen:music.something
                    sound: music/something.ogg
                    jukebox:
                      duration: 120s
                      range: 5
                  - id: block.glass.place
                    replace: true
                    sounds: []
                """);

        SoundManager soundManager = new SoundManager(config);
        Collection<CustomSound> sounds = soundManager.getCustomSounds();

        assertEquals(2, sounds.size());
        assertTrue(sounds.stream().anyMatch(sound -> sound.getSoundId().equals("oraxen:music.something")));
        assertTrue(sounds.stream().anyMatch(sound -> sound.getKey().equals("block.glass.place")));
        assertEquals("oraxen:music.something", soundManager.songKeyToSoundId(Key.key("oraxen:music.something")));
        assertEquals(5F, soundManager.jukeboxRange(Key.key("oraxen:music.something")));
    }

    @Test
    void loadsLegacyMapFormatWithoutConfigMigration() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                settings:
                  automatically_generate: true
                sounds:
                  welcome:
                    sound: welcome.ogg
                    jukebox_song:
                      length_in_seconds: 180
                """);

        SoundManager soundManager = new SoundManager(config);

        assertEquals(1, soundManager.getCustomSounds().size());
        assertEquals("minecraft:welcome", soundManager.songKeyToSoundId(Key.key("oraxen:welcome")));
    }
}
