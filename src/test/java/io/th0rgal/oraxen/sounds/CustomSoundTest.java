package io.th0rgal.oraxen.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomSoundTest {

    @Test
    void parsesNamespacedSoundEventAndSoundReferences() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                id: oraxen:music.something
                sounds:
                  - music/something.ogg
                  - oraxen:music/something2.ogg
                stream: true
                volume: 0.5f
                pitch: 1f
                weight: 2
                jukebox:
                  comparator_output: 20
                  range: 32
                  duration: 120s
                  description: Description
                """);

        CustomSound sound = new CustomSound(config);

        assertEquals("oraxen", sound.getNamespace());
        assertEquals("music.something", sound.getKey());
        assertEquals("oraxen:music.something", sound.getSoundId());
        assertEquals("oraxen:music.something", sound.getJukeboxSongId());
        assertEquals(15, sound.getComparatorOutput());
        assertEquals(120, sound.getLengthInSeconds());
        assertEquals(32F, sound.getRange());

        JsonObject json = sound.toJson();
        JsonArray sounds = json.getAsJsonArray("sounds");
        assertEquals("minecraft:music/something", sounds.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("oraxen:music/something2", sounds.get(1).getAsJsonObject().get("name").getAsString());
        assertTrue(sounds.get(0).getAsJsonObject().get("stream").getAsBoolean());
        assertEquals(0.5F, sounds.get(0).getAsJsonObject().get("volume").getAsFloat());
        assertEquals(2, sounds.get(0).getAsJsonObject().get("weight").getAsInt());
        assertEquals(32, sounds.get(0).getAsJsonObject().get("attenuation_distance").getAsInt());
        assertFalse(sounds.get(0).getAsJsonObject().has("pitch"));
    }

    @Test
    void replacementWithoutSoundsGeneratesEmptySoundsArray() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                id: block.glass.place
                replace: true
                """);

        CustomSound sound = new CustomSound(config);
        JsonObject json = sound.toJson();

        assertEquals("minecraft", sound.getNamespace());
        assertEquals("block.glass.place", sound.getKey());
        assertTrue(json.get("replace").getAsBoolean());
        assertEquals(0, json.getAsJsonArray("sounds").size());
    }

    @Test
    void legacyNonNamespacedJukeboxSoundKeepsOraxenSongKey() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                id: welcome
                sound: welcome.ogg
                jukebox:
                  duration: 180s
                """);

        CustomSound sound = new CustomSound(config);

        assertEquals("minecraft:welcome", sound.getSoundId());
        assertEquals("oraxen:welcome", sound.getJukeboxSongId());
    }
}
