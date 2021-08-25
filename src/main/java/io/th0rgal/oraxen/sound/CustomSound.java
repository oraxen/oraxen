package io.th0rgal.oraxen.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CustomSound {

    private final String name;
    private final String sound;
    private final String category;

    public CustomSound(String name, String sound, String category) {
        this.name = name;
        if (sound.endsWith(".ogg"))
            sound = sound.substring(0, sound.length() - 4);
        this.sound = sound;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray sounds = new JsonArray();
        sounds.add(sound);
        output.addProperty("category", category);
        output.add("sounds", sounds);
        return output;
    }

}