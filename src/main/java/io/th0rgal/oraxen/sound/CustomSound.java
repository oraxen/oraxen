package io.th0rgal.oraxen.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomSound {

    private final String name;
    private final SoundCategory category;
    private final String subtitle;
    private final boolean replace;
    private final List<String> sounds = new ArrayList<>();

    public CustomSound(@NotNull String name, @NotNull List<String> sounds, SoundCategory category, boolean replace, String subtitle) {
        this.name = name;
        List<String> temp = new ArrayList<>();
        for (String sound : sounds) {
            if (sound == null) continue;
            temp.add(sound.replace(".ogg", ""));
        }
        this.sounds.addAll(temp);
        this.category = category == null ? SoundCategory.MASTER : category;
        this.replace = replace;
        this.subtitle = subtitle;
    }

    public void play(@NotNull Player player, @NotNull Location location) {
        play(player, location, 1.0F, 1.0F);
    }

    public void play(@NotNull Player player, @NotNull Location location, float volume, float pitch) {
        play(player, location, category, volume, pitch);
    }

    public void play(@NotNull Player player, @NotNull Location location, @NotNull SoundCategory category, float volume, float pitch) {
        player.playSound(location, name, category, volume, pitch);
    }

    public void stop(@NotNull Player player) {
        stop(player, null);
    }

    public void stop(@NotNull Player player, @Nullable SoundCategory category) {
        player.stopSound(name, category);
    }

    public String getName() {
        return name;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public boolean isReplace() {
        return replace;
    }

    public List<String> getSounds() {
        return new ArrayList<>(sounds);
    }

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        if (category != null) output.addProperty("category", category.toString().toLowerCase(Locale.ROOT));
        if (replace) output.addProperty("replace", true);
        if (subtitle != null) output.addProperty("subtitle", subtitle);
        final JsonArray sounds = new JsonArray();
        if (this.sounds.isEmpty()) sounds.getAsJsonArray();
        else for (String sound : this.sounds) {
            sounds.add(sound);
        }
        output.add("sounds", sounds);
        return output;
    }

}
