package io.th0rgal.oraxen.sounds;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundConfigMigrationTest {

    @Test
    void migratesLegacySoundMapToList() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                settings:
                  automatically_generate: true
                sounds:
                  welcome:
                    sound: welcome.ogg
                    stream: true
                    jukebox_song:
                      description: "<gold>Welcome</gold>"
                      length_in_seconds: 180
                      comparator_output: 12
                  block.glass.place:
                    replace: true
                    sounds: []
                """);

        assertTrue(SoundConfigMigration.migrateToNewFormat(config));

        List<Map<?, ?>> sounds = config.getMapList("sounds");
        assertEquals(2, sounds.size());
        assertEquals("welcome", sounds.get(0).get("id"));
        assertEquals("welcome.ogg", sounds.get(0).get("sound"));
        assertEquals(true, sounds.get(0).get("stream"));

        Map<?, ?> jukebox = (Map<?, ?>) sounds.get(0).get("jukebox");
        assertEquals("180s", jukebox.get("duration"));
        assertEquals(12, jukebox.get("comparator_output"));

        assertEquals("block.glass.place", sounds.get(1).get("id"));
        assertEquals(true, sounds.get(1).get("replace"));
        assertEquals(List.of(), sounds.get(1).get("sounds"));
    }

    @Test
    void leavesNewListFormatUnchanged() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                sounds:
                  - id: oraxen:music.something
                    sound: music/something.ogg
                """);

        assertFalse(SoundConfigMigration.migrateToNewFormat(config));
        assertEquals("oraxen:music.something", config.getMapList("sounds").get(0).get("id"));
    }

    @Test
    void mergesLegacySoundFileIntoExistingSoundsFile() throws Exception {
        YamlConfiguration existingSounds = new YamlConfiguration();
        existingSounds.loadFromString("""
                sounds:
                  - id: oraxen:music.existing
                    sound: music/existing.ogg
                  - id: block.glass.place
                    sounds: []
                """);

        YamlConfiguration newLegacySound = new YamlConfiguration();
        newLegacySound.loadFromString("""
                sounds:
                  block:
                    glass:
                      place:
                        sound: duplicate.ogg
                  music:
                    added:
                      sound: music/added.ogg
                      jukebox_song:
                        length_in_seconds: 90
                """);

        assertTrue(SoundConfigMigration.mergeSounds(existingSounds, newLegacySound));

        List<Map<?, ?>> sounds = existingSounds.getMapList("sounds");
        assertEquals(3, sounds.size());
        assertEquals("oraxen:music.existing", sounds.get(0).get("id"));
        assertEquals("block.glass.place", sounds.get(1).get("id"));
        assertEquals("music.added", sounds.get(2).get("id"));
        assertEquals("music/added.ogg", sounds.get(2).get("sound"));

        Map<?, ?> jukebox = (Map<?, ?>) sounds.get(2).get("jukebox");
        assertEquals("90s", jukebox.get("duration"));
    }
}
