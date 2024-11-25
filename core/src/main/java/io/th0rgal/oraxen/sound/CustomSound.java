package io.th0rgal.oraxen.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CustomSound {

    private final String name;
    private final SoundCategory category;
    private final String subtitle;
    private final boolean replace;
    private final List<String> sounds = new ArrayList<>();
    private final boolean stream;

    // Jukebox data as an Optional record
    private final Optional<JukeboxData> jukeboxData;

    private record JukeboxData(
            Component description,
            int lengthInSeconds,
            int comparatorOutput) {
    }

    public CustomSound(@NotNull String name, @NotNull ConfigurationSection config) {
        this.name = name;

        // Parse sounds list
        List<String> soundsList = config.getStringList("sounds");
        if (soundsList.isEmpty() && config.contains("sound")) {
            soundsList = Collections.singletonList(config.getString("sound"));
        }

        // Process sounds, removing .ogg extension
        for (String sound : soundsList) {
            if (sound != null) {
                sounds.add(sound.replace(".ogg", ""));
            }
        }

        // Parse category
        String categoryStr = config.getString("category");
        this.category = categoryStr != null ? SoundCategory.valueOf(categoryStr.toUpperCase(Locale.ROOT))
                : SoundCategory.MASTER;

        this.subtitle = config.getString("subtitle");
        this.replace = config.getBoolean("replace", false);
        this.stream = config.getBoolean("stream", false);

        // Initialize jukebox properties
        ConfigurationSection jukeboxSection = config.getConfigurationSection("jukebox_song");
        if (jukeboxSection != null) {
            String descriptionText = jukeboxSection.getString("description",
                    subtitle != null ? subtitle : "<white>Music Disc</white>");
            this.jukeboxData = Optional.of(new JukeboxData(
                    AdventureUtils.MINI_MESSAGE.deserialize(descriptionText),
                    jukeboxSection.getInt("length_in_seconds", 120),
                    jukeboxSection.getInt("comparator_output", 15)));
        } else {
            this.jukeboxData = Optional.empty();
        }
    }

    public void play(@NotNull Player player, @NotNull Location location) {
        play(player, location, 1.0F, 1.0F);
    }

    public void play(@NotNull Player player, @NotNull Location location, float volume, float pitch) {
        play(player, location, category, volume, pitch);
    }

    public void play(@NotNull Player player, @NotNull Location location, @NotNull SoundCategory category, float volume,
            float pitch) {
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

    public boolean isStream() {
        return stream;
    }

    public boolean isJukeboxSound() {
        return jukeboxData.isPresent();
    }

    public int getLengthInSeconds() {
        return jukeboxData.map(data -> data.lengthInSeconds).orElse(120);
    }

    public int getComparatorOutput() {
        return jukeboxData.map(data -> data.comparatorOutput).orElse(15);
    }

    public Component getDescription() {
        return jukeboxData
                .map(data -> data.description)
                .orElse(AdventureUtils.MINI_MESSAGE.deserialize("<white>Music Disc</white>"));
    }

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        if (category != null)
            output.addProperty("category", category.toString().toLowerCase(Locale.ROOT));
        if (replace)
            output.addProperty("replace", true);
        if (subtitle != null)
            output.addProperty("subtitle", subtitle);
        if (stream)
            output.addProperty("stream", true);
        final JsonArray sounds = new JsonArray();
        if (this.sounds.isEmpty())
            sounds.getAsJsonArray();
        else
            for (String sound : this.sounds) {
                sounds.add(sound);
            }
        output.add("sounds", sounds);
        return output;
    }

    public JsonObject toJukeboxJson() {
        if (jukeboxData.isEmpty()) {
            return new JsonObject();
        }

        JsonObject songJson = new JsonObject();

        JsonObject soundEvent = new JsonObject();
        soundEvent.addProperty("id", "oraxen:" + name);
        songJson.add("sound_event", soundEvent);

        JsonObject description = new JsonObject();
        description.addProperty("text", subtitle != null ? subtitle : "Music Disc");
        songJson.add("description", description);

        songJson.addProperty("length_in_seconds", getLengthInSeconds());
        songJson.addProperty("comparator_output", getComparatorOutput());

        return songJson;
    }
}
