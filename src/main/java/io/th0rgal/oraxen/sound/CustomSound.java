package io.th0rgal.oraxen.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class CustomSound {

    private final String name;
    private final List<String> sounds = new ArrayList<>();
    private final String category;
    private final boolean replace;
    private final String subtitle;

    public CustomSound(String name, List<String> sounds, String category, boolean replace, String subtitle) {
        this.name = name;
        List<String> temp = new ArrayList<>();
        for (String sound : sounds) {
            if (sound == null) continue;
            temp.add(sound.replace(".ogg", ""));
        }
        this.sounds.addAll(temp);
        this.category = category;
        this.replace = replace;
        this.subtitle = subtitle;
    }

    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray sounds = new JsonArray();
        if (this.sounds.isEmpty()) sounds.getAsJsonArray();
        else for (String sound : this.sounds) {
            sounds.add(sound);
        }
        output.add("sounds", sounds);
        if (category != null) output.addProperty("category", category);
        if (replace) output.addProperty("replace", true);
        if (subtitle != null) output.addProperty("subtitle", subtitle);
        return output;
    }

}
