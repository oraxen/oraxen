package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.text;

import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.PluginUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parsed configuration for a single virtual text entity attached to a furniture base.
 *
 * <p>The text is rendered client-side via packet-spawned TextDisplay entities;
 * no real entities are created server-side, so chunk saves, collisions, and
 * AI tick costs are all avoided.</p>
 */
public final class FurnitureTextDefinition {

    public enum Alignment { CENTER, LEFT, RIGHT }
    public enum Billboard { FIXED, VERTICAL, HORIZONTAL, CENTER }

    private final List<String> rawLines;
    private final int lineWidth;
    private final int backgroundArgb;
    private final byte textOpacity;
    private final boolean seeThrough;
    private final boolean shadow;
    private final boolean defaultBackground;
    private final Alignment alignment;
    private final Billboard billboard;
    private final Vector3f translation;
    private final Vector3f scale;
    private final float viewRange;
    private final int refreshTicks;
    private final boolean usesPlaceholders;

    private FurnitureTextDefinition(List<String> rawLines,
                                     int lineWidth,
                                     int backgroundArgb,
                                     byte textOpacity,
                                     boolean seeThrough,
                                     boolean shadow,
                                     boolean defaultBackground,
                                     Alignment alignment,
                                     Billboard billboard,
                                     Vector3f translation,
                                     Vector3f scale,
                                     float viewRange,
                                     int refreshTicks,
                                     boolean usesPlaceholders) {
        this.rawLines = rawLines;
        this.lineWidth = lineWidth;
        this.backgroundArgb = backgroundArgb;
        this.textOpacity = textOpacity;
        this.seeThrough = seeThrough;
        this.shadow = shadow;
        this.defaultBackground = defaultBackground;
        this.alignment = alignment;
        this.billboard = billboard;
        this.translation = translation;
        this.scale = scale;
        this.viewRange = viewRange;
        this.refreshTicks = refreshTicks;
        this.usesPlaceholders = usesPlaceholders;
    }

    public static FurnitureTextDefinition parse(ConfigurationSection section) {
        List<String> lines = new ArrayList<>();
        if (section.isList("text")) {
            for (Object entry : section.getList("text", new ArrayList<>())) {
                if (entry != null) lines.add(entry.toString());
            }
        } else if (section.isString("text")) {
            String raw = section.getString("text", "");
            lines.addAll(Arrays.asList(raw.split("\\n")));
        }

        int lineWidth = section.getInt("line_width", 200);
        int backgroundArgb = parseColor(section.getString("background_color", "#40000000"));
        int rawTextOpacity = section.getInt("text_opacity", -1);
        byte textOpacity = (byte) (rawTextOpacity < 0 ? -1 : Math.min(255, rawTextOpacity));
        boolean seeThrough = section.getBoolean("see_through", false);
        boolean shadow = section.getBoolean("shadow", false);
        boolean defaultBackground = section.getBoolean("default_background", false);
        Alignment alignment = parseAlignment(section.getString("alignment", "CENTER"));
        Billboard billboard = parseBillboard(section.getString("billboard", "CENTER"));

        Vector3f translation = readVector(section, "offset", new Vector3f(0f, 0.6f, 0f));
        Vector3f scale = readVector(section, "scale", new Vector3f(1f, 1f, 1f));

        float viewRange = (float) section.getDouble("view_range", 32.0);
        int refreshTicks = Math.max(0, section.getInt("refresh_ticks", 0));

        boolean usesPlaceholders = refreshTicks > 0
                || lines.stream().anyMatch(line -> line.contains("%") || line.contains("{"));

        return new FurnitureTextDefinition(
                List.copyOf(lines),
                lineWidth,
                backgroundArgb,
                textOpacity,
                seeThrough,
                shadow,
                defaultBackground,
                alignment,
                billboard,
                translation,
                scale,
                viewRange,
                refreshTicks,
                usesPlaceholders
        );
    }

    private static Vector3f readVector(ConfigurationSection section, String path, Vector3f fallback) {
        ConfigurationSection sub = section.getConfigurationSection(path);
        if (sub != null) {
            return new Vector3f(
                    (float) sub.getDouble("x", fallback.x),
                    (float) sub.getDouble("y", fallback.y),
                    (float) sub.getDouble("z", fallback.z)
            );
        }
        Object raw = section.get(path);
        if (raw instanceof List<?> list && list.size() == 3) {
            return new Vector3f(
                    ((Number) list.get(0)).floatValue(),
                    ((Number) list.get(1)).floatValue(),
                    ((Number) list.get(2)).floatValue()
            );
        }
        return new Vector3f(fallback);
    }

    private static Alignment parseAlignment(String value) {
        if (value == null) return Alignment.CENTER;
        try {
            return Alignment.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Alignment.CENTER;
        }
    }

    private static Billboard parseBillboard(String value) {
        if (value == null) return Billboard.CENTER;
        try {
            return Billboard.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Billboard.CENTER;
        }
    }

    private static int parseColor(String raw) {
        if (raw == null || raw.isEmpty()) return 0x40000000;
        String s = raw.trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            if (s.length() == 6) {
                // RGB → add 0x40 alpha by default for visibility
                return 0x40000000 | Integer.parseUnsignedInt(s, 16);
            }
            if (s.length() == 8) {
                return Integer.parseUnsignedInt(s, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0x40000000;
    }

    public Component renderComponent(@org.jetbrains.annotations.Nullable Player viewer) {
        if (rawLines.isEmpty()) return Component.empty();
        boolean papi = viewer != null && PluginUtils.isEnabled("PlaceholderAPI");
        Component result = null;
        for (String rawLine : rawLines) {
            String expanded = papi
                    ? me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, rawLine)
                    : rawLine;
            Component line = viewer != null
                    ? AdventureUtils.MINI_MESSAGE_PLAYER(viewer).deserialize(expanded)
                    : AdventureUtils.MINI_MESSAGE.deserialize(expanded);
            result = result == null ? line : result.append(Component.newline()).append(line);
        }
        return result == null ? Component.empty() : result;
    }

    public int getLineWidth() { return lineWidth; }
    public int getBackgroundArgb() { return backgroundArgb; }
    public byte getTextOpacity() { return textOpacity; }
    public boolean isSeeThrough() { return seeThrough; }
    public boolean hasShadow() { return shadow; }
    public boolean hasDefaultBackground() { return defaultBackground; }
    public Alignment getAlignment() { return alignment; }
    public Billboard getBillboard() { return billboard; }
    public Vector3f getTranslation() { return new Vector3f(translation); }
    public Vector3f getScale() { return new Vector3f(scale); }
    public float getViewRange() { return viewRange; }
    public int getRefreshTicks() { return refreshTicks; }
    public boolean usesPlaceholders() { return usesPlaceholders; }
    public List<String> getRawLines() { return rawLines; }
}
