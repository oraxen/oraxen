package io.th0rgal.oraxen.sounds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class CustomSound {

    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9/._-]+");

    private final String namespace;
    private final String key;
    private final boolean explicitNamespace;
    private final SoundCategory category;
    private final boolean writeCategory;
    private final String subtitle;
    private final boolean replace;
    private final List<SoundEntry> sounds = new ArrayList<>();

    // Jukebox data as an Optional record
    private final Optional<JukeboxData> jukeboxData;

    private record ParsedKey(String namespace, String key, boolean explicitNamespace) {
        String asString() {
            return namespace + ":" + key;
        }

        String asSoundJsonString() {
            return namespace.equals("minecraft") ? key : asString();
        }
    }

    private record SoundEntry(String name, boolean stream, float volume, float pitch, int weight,
            int attenuationDistance, boolean preload) {
        boolean allDefault() {
            return !stream && volume == 1.0F && pitch == 1.0F && weight == 1
                    && attenuationDistance == 16 && !preload;
        }
    }

    private record JukeboxData(
            String description,
            int lengthInSeconds,
            int comparatorOutput,
            @Nullable Float range,
            ParsedKey songKey) {
    }

    public CustomSound(@NotNull String name, @NotNull ConfigurationSection config) {
        this(config, name);
    }

    public CustomSound(@NotNull ConfigurationSection config) {
        this(config, null);
    }

    private CustomSound(@NotNull ConfigurationSection config, @Nullable String fallbackName) {
        String configuredId = config.getString("id", fallbackName);
        if (configuredId == null || configuredId.isBlank())
            throw new IllegalArgumentException("Custom sound is missing an id");

        ParsedKey soundKey = parseKey(configuredId);
        this.namespace = soundKey.namespace();
        this.key = soundKey.key();
        this.explicitNamespace = soundKey.explicitNamespace();

        ConfigurationSection jukeboxSection = config.getConfigurationSection("jukebox");
        if (jukeboxSection == null)
            jukeboxSection = config.getConfigurationSection("jukebox_song");
        Float jukeboxRange = jukeboxSection != null ? getPositiveFloatOrNull(jukeboxSection, "range") : null;

        boolean stream = getBoolean(config, "stream", false);
        float volume = getFloat(config, "volume", 1.0F);
        float pitch = getFloat(config, "pitch", 1.0F);
        int weight = Math.max(1, getInt(config, "weight", 1));
        int attenuationDistance = getAttenuationDistance(config, jukeboxRange);
        boolean preload = getBoolean(config, "preload", false);

        for (String sound : getConfiguredSounds(config))
            sounds.add(new SoundEntry(parseSoundReference(sound, namespace), stream, volume, pitch, weight,
                    attenuationDistance, preload));

        String categoryStr = config.getString("category");
        this.category = parseCategory(categoryStr);
        this.writeCategory = categoryStr != null && !categoryStr.isBlank();

        this.subtitle = config.getString("subtitle");
        this.replace = config.getBoolean("replace", false);

        if (jukeboxSection != null) {
            String descriptionText = jukeboxSection.getString("description",
                    subtitle != null ? subtitle : "<white>Music Disc</white>");
            ParsedKey songKey = getJukeboxSongKey(jukeboxSection);
            this.jukeboxData = Optional.of(new JukeboxData(
                    descriptionText,
                    getDurationInSeconds(jukeboxSection),
                    clamp(getInt(jukeboxSection, "comparator_output", 15), 1, 15),
                    jukeboxRange,
                    songKey));
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
        player.playSound(location, getSoundId(), category, volume, pitch);
    }

    public void stop(@NotNull Player player) {
        stop(player, null);
    }

    public void stop(@NotNull Player player, @Nullable SoundCategory category) {
        player.stopSound(getSoundId(), category);
    }

    /**
     * Returns the sound event path without a namespace for legacy callers.
     */
    public String getName() {
        return key;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return namespace + ":" + key;
    }

    public boolean hasExplicitNamespace() {
        return explicitNamespace;
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
        return new ArrayList<>(sounds.stream().map(SoundEntry::name).toList());
    }

    public boolean isStream() {
        return sounds.stream().anyMatch(SoundEntry::stream);
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

    @Nullable
    public Float getRange() {
        return jukeboxData.map(data -> data.range).orElse(null);
    }

    public Component getDescription() {
        return AdventureUtils.MINI_MESSAGE.deserialize(jukeboxData
                .map(data -> data.description)
                .orElse("<white>Music Disc</white>"));
    }

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        if (writeCategory)
            output.addProperty("category", category.toString().toLowerCase(Locale.ROOT));
        if (replace)
            output.addProperty("replace", true);
        if (subtitle != null)
            output.addProperty("subtitle", subtitle);

        final JsonArray sounds = new JsonArray();
        for (SoundEntry sound : this.sounds) {
            if (sound.allDefault()) {
                sounds.add(sound.name());
                continue;
            }

            JsonObject soundObject = new JsonObject();
            soundObject.addProperty("name", sound.name());
            if (sound.volume() != 1.0F)
                soundObject.addProperty("volume", sound.volume());
            if (sound.pitch() != 1.0F)
                soundObject.addProperty("pitch", sound.pitch());
            if (sound.weight() != 1)
                soundObject.addProperty("weight", sound.weight());
            if (sound.stream())
                soundObject.addProperty("stream", true);
            if (sound.attenuationDistance() != 16)
                soundObject.addProperty("attenuation_distance", sound.attenuationDistance());
            if (sound.preload())
                soundObject.addProperty("preload", true);
            sounds.add(soundObject);
        }
        output.add("sounds", sounds);
        return output;
    }

    public String getSoundId() {
        return namespace + ":" + key;
    }

    public String getJukeboxSongId() {
        return jukeboxData.map(data -> data.songKey.asString()).orElse(getSoundId());
    }

    public String getJukeboxSongNamespace() {
        return jukeboxData.map(data -> data.songKey.namespace()).orElse(namespace);
    }

    public String getJukeboxSongKey() {
        return jukeboxData.map(data -> data.songKey.key()).orElse(key);
    }

    public JsonObject toJukeboxJson() {
        if (jukeboxData.isEmpty()) {
            return new JsonObject();
        }

        JsonObject songJson = new JsonObject();

        JsonObject soundEvent = new JsonObject();
        soundEvent.addProperty("sound_id", getSoundId());
        Float range = getRange();
        if (range != null)
            soundEvent.addProperty("range", range);
        songJson.add("sound_event", soundEvent);

        songJson.add("description", AdventureUtils.GSON_SERIALIZER.serializeToTree(getDescription()));
        songJson.addProperty("length_in_seconds", getLengthInSeconds());
        songJson.addProperty("comparator_output", getComparatorOutput());

        return songJson;
    }

    @NotNull
    private static List<String> getConfiguredSounds(@NotNull ConfigurationSection config) {
        if (config.contains("sounds")) {
            Object soundsValue = config.get("sounds");
            if (soundsValue instanceof List<?> soundsList) {
                List<String> parsedSounds = new ArrayList<>();
                for (Object sound : soundsList)
                    if (sound != null)
                        parsedSounds.add(String.valueOf(sound));
                return parsedSounds;
            }
            String singleSound = config.getString("sounds");
            return singleSound != null ? Collections.singletonList(singleSound) : List.of();
        }

        Object soundValue = config.get("sound");
        if (soundValue instanceof List<?> soundList) {
            List<String> parsedSounds = new ArrayList<>();
            for (Object sound : soundList)
                if (sound != null)
                    parsedSounds.add(String.valueOf(sound));
            return parsedSounds;
        }

        String sound = config.getString("sound");
        return sound != null ? Collections.singletonList(sound) : List.of();
    }

    @NotNull
    private static String parseSoundReference(@NotNull String sound, @NotNull String eventNamespace) {
        ParsedKey key = parseKey(stripOgg(sound.trim().replace('\\', '/')));
        if (key.namespace().equals("minecraft") && !eventNamespace.equals("minecraft"))
            return key.asString();
        return key.asSoundJsonString();
    }

    @NotNull
    private static ParsedKey parseKey(@NotNull String rawKey) {
        String normalized = rawKey.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        boolean explicitNamespace = normalized.contains(":");
        String namespace = explicitNamespace ? normalized.substring(0, normalized.indexOf(':')) : "minecraft";
        String key = explicitNamespace ? normalized.substring(normalized.indexOf(':') + 1) : normalized;

        String sanitizedNamespace = sanitizeNamespace(namespace);
        String sanitizedKey = sanitizeKey(key);
        if (!sanitizedNamespace.equals(namespace) || !sanitizedKey.equals(key))
            Logs.logWarning("Invalid sound key '" + rawKey + "', using '" + sanitizedNamespace + ":" + sanitizedKey + "'");

        return new ParsedKey(sanitizedNamespace, sanitizedKey, explicitNamespace);
    }

    @NotNull
    private ParsedKey getJukeboxSongKey(@NotNull ConfigurationSection jukeboxSection) {
        String configuredSongKey = jukeboxSection.getString("song_key");
        if (configuredSongKey != null && !configuredSongKey.isBlank())
            return parseKey(configuredSongKey);

        // Legacy Oraxen jukebox songs used oraxen:<sound_name> while the sound event itself
        // lived in minecraft:<sound_name>. Keep that mapping for migrated non-namespaced ids.
        if (!explicitNamespace)
            return parseKey("oraxen:" + key);

        return parseKey(getSoundId());
    }

    @NotNull
    private static String sanitizeNamespace(@NotNull String namespace) {
        if (namespace.isBlank())
            return "minecraft";
        if (VALID_NAMESPACE.matcher(namespace).matches())
            return namespace;
        return namespace.replaceAll("[^a-z0-9_.-]", "_");
    }

    @NotNull
    private static String sanitizeKey(@NotNull String key) {
        String sanitizedKey = stripOgg(key);
        if (sanitizedKey.isBlank())
            return "missing";
        if (VALID_KEY.matcher(sanitizedKey).matches())
            return sanitizedKey;
        return sanitizedKey.replaceAll("[^a-z0-9/._-]", "_");
    }

    @NotNull
    private static String stripOgg(@NotNull String sound) {
        return sound.toLowerCase(Locale.ROOT).endsWith(".ogg") ? sound.substring(0, sound.length() - 4) : sound;
    }

    @NotNull
    private static SoundCategory parseCategory(@Nullable String categoryStr) {
        if (categoryStr == null || categoryStr.isBlank())
            return SoundCategory.MASTER;

        String normalized = categoryStr.toUpperCase(Locale.ROOT);
        if (normalized.equals("RECORD"))
            normalized = "RECORDS";

        try {
            return SoundCategory.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            Logs.logWarning("Invalid sound category '" + categoryStr + "', using MASTER");
            return SoundCategory.MASTER;
        }
    }

    private static boolean getBoolean(@NotNull ConfigurationSection section, @NotNull String path, boolean defaultValue) {
        Object value = section.get(path);
        if (value instanceof Boolean bool)
            return bool;
        if (value instanceof String string) {
            if (string.equalsIgnoreCase("true"))
                return true;
            if (string.equalsIgnoreCase("false"))
                return false;
        }
        return defaultValue;
    }

    private static int getInt(@NotNull ConfigurationSection section, @NotNull String path, int defaultValue) {
        Object value = section.get(path);
        if (value instanceof Number number)
            return number.intValue();
        if (value instanceof String string) {
            try {
                return Integer.parseInt(stripNumericSuffix(string.trim()));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static float getFloat(@NotNull ConfigurationSection section, @NotNull String path, float defaultValue) {
        Object value = section.get(path);
        if (value instanceof Number number)
            return Math.max(0.0F, number.floatValue());
        if (value instanceof String string) {
            try {
                return Math.max(0.0F, Float.parseFloat(stripNumericSuffix(string.trim())));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Nullable
    private static Float getPositiveFloatOrNull(@NotNull ConfigurationSection section, @NotNull String path) {
        if (!section.contains(path))
            return null;
        float value = getFloat(section, path, -1.0F);
        return value > 0.0F ? value : null;
    }

    private static int getAttenuationDistance(@NotNull ConfigurationSection section, @Nullable Float jukeboxRange) {
        if (section.contains("attenuation_distance"))
            return Math.max(1, getInt(section, "attenuation_distance", 16));
        if (jukeboxRange != null)
            return Math.max(1, (int) Math.ceil(jukeboxRange));
        return 16;
    }

    private static int getDurationInSeconds(@NotNull ConfigurationSection section) {
        Object duration = section.get("duration");
        if (duration == null)
            duration = section.get("length_in_seconds");

        if (duration instanceof Number number)
            return Math.max(1, number.intValue());

        if (duration instanceof String string)
            return Math.max(1, parseDurationString(string));

        return 120;
    }

    private static int parseDurationString(@NotNull String duration) {
        String normalized = duration.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        try {
            if (normalized.endsWith("seconds"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "seconds".length()));
            if (normalized.endsWith("second"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "second".length()));
            if (normalized.endsWith("secs"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "secs".length()));
            if (normalized.endsWith("sec"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "sec".length()));
            if (normalized.endsWith("s"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - 1));
            if (normalized.endsWith("minutes"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "minutes".length())) * 60;
            if (normalized.endsWith("minute"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "minute".length())) * 60;
            if (normalized.endsWith("mins"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "mins".length())) * 60;
            if (normalized.endsWith("min"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - "min".length())) * 60;
            if (normalized.endsWith("m"))
                return Integer.parseInt(normalized.substring(0, normalized.length() - 1)) * 60;
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return 120;
        }
    }

    @NotNull
    private static String stripNumericSuffix(@NotNull String value) {
        if (value.endsWith("f") || value.endsWith("F") || value.endsWith("d") || value.endsWith("D"))
            return value.substring(0, value.length() - 1);
        return value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
