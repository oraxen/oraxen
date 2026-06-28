package io.th0rgal.oraxen.paintings;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record CustomPainting(
        @NotNull Key variantKey,
        @NotNull Key assetId,
        @Nullable String title,
        @Nullable String author,
        int width,
        int height,
        boolean includeInRandom
) {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 16;

    public static List<CustomPainting> fromConfigSection(@Nullable ConfigurationSection paintingsSection) {
        List<CustomPainting> paintings = new ArrayList<>();
        if (paintingsSection == null) return paintings;

        for (String key : paintingsSection.getKeys(false)) {
            ConfigurationSection paintingSection = paintingsSection.getConfigurationSection(key);
            if (paintingSection == null) continue;
            if (!paintingSection.getBoolean("enabled", true)) continue;

            try {
                paintings.add(fromConfig(key, paintingSection));
            } catch (IllegalArgumentException exception) {
                Logs.logWarning("Failed to parse custom painting '" + key + "' in paintings.yml");
                Logs.debug(exception);
            }
        }
        return paintings;
    }

    public static CustomPainting fromConfig(String key, ConfigurationSection section) {
        Key variantKey = parseKey(key);
        Key assetId = parseKey(section.getString("asset_id", variantKey.asString()));
        String title = emptyToNull(section.getString("title", variantKey.value()));
        String author = emptyToNull(section.getString("author", "Oraxen"));
        int width = clamp(section.getInt("width", MIN_SIZE), MIN_SIZE, MAX_SIZE);
        int height = clamp(section.getInt("height", MIN_SIZE), MIN_SIZE, MAX_SIZE);
        boolean includeInRandom = section.getBoolean("include_in_random",
                section.getBoolean("random_place", false));

        return new CustomPainting(variantKey, assetId, title, author, width, height, includeInRandom);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("asset_id", assetId.asString());
        if (title != null) {
            json.add("title", AdventureUtils.GSON_SERIALIZER.serializeToTree(
                    AdventureUtils.MINI_MESSAGE.deserialize(title)));
        }
        if (author != null) {
            json.add("author", AdventureUtils.GSON_SERIALIZER.serializeToTree(
                    AdventureUtils.MINI_MESSAGE.deserialize(author)));
        }
        json.addProperty("width", width);
        json.addProperty("height", height);
        return json;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Key parseKey(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        try {
            return normalized.contains(":") ? Key.key(normalized) : Key.key("oraxen", normalized);
        } catch (InvalidKeyException e) {
            String sanitized = sanitizeKey(normalized);
            Logs.logWarning("Invalid custom painting key '" + value + "', using sanitized key '" + sanitized + "' instead.");
            return sanitized.contains(":") ? Key.key(sanitized) : Key.key("oraxen", sanitized);
        }
    }

    private static String sanitizeKey(String value) {
        if (value.isBlank()) return "painting";
        if (!value.contains(":")) return sanitizePath(value);

        String[] parts = value.split(":", 2);
        String namespace = sanitizeNamespace(parts[0]);
        String path = parts.length > 1 ? sanitizePath(parts[1].replace(':', '_')) : "painting";
        return namespace + ":" + path;
    }

    private static String sanitizeNamespace(String value) {
        String sanitized = value.replaceAll("[^a-z0-9_.-]", "_");
        return sanitized.isBlank() ? "oraxen" : sanitized;
    }

    private static String sanitizePath(String value) {
        String sanitized = value.replaceAll("[^a-z0-9_./-]", "_");
        return sanitized.isBlank() ? "painting" : sanitized;
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
