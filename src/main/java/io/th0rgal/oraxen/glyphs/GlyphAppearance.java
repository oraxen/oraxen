package io.th0rgal.oraxen.glyphs;

import io.th0rgal.oraxen.fonts.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates the appearance configuration for glyphs.
 * Supports custom fonts and shadow colors.
 *
 * @param font        The Adventure Key for the font (e.g., minecraft:default)
 * @param shadowColor The shadow color in ARGB format, or null for no custom shadow
 */
public record GlyphAppearance(@NotNull Key font, @Nullable Integer shadowColor) {

    // Text shadow colors require Adventure 4.18+ (Minecraft 1.21.4+).
    private static final boolean SHADOW_COLOR_AVAILABLE = hasShadowColorApi();
    private static final AtomicBoolean warnedShadowColorUnavailable = new AtomicBoolean();

    /**
     * Default appearance with minecraft:default font and no shadow override.
     */
    public static final GlyphAppearance DEFAULT = new GlyphAppearance(Key.key("minecraft", "default"), null);

    /**
     * Creates a GlyphAppearance from a configuration section.
     *
     * @param section The "appearance" configuration section, may be null
     * @return A GlyphAppearance with the configured values, or DEFAULT if section is null
     */
    @NotNull
    public static GlyphAppearance fromConfig(@Nullable ConfigurationSection section) {
        if (section == null) return DEFAULT;

        String fontString = section.getString("font", "minecraft:default");
        Key font = parseKey(fontString);

        String shadowString = section.getString("shadow_color", null);
        Integer shadowColor = parseArgbColor(shadowString);

        return new GlyphAppearance(font, shadowColor);
    }

    /**
     * Parses a font key string into an Adventure Key.
     *
     * @param fontString The font string (e.g., "minecraft:default" or just "default")
     * @return The parsed Key
     */
    @NotNull
    private static Key parseKey(String fontString) {
        if (fontString == null || fontString.isBlank()) {
            return Key.key("minecraft", "default");
        }
        if (fontString.contains(":")) {
            String[] parts = fontString.split(":", 2);
            return Key.key(parts[0], parts[1]);
        }
        return Key.key("minecraft", fontString);
    }

    /**
     * Parses a hex color string into an ARGB integer.
     * Supports formats: #AARRGGBB, #RRGGBB (assumes FF alpha), #RGB (expands to RRGGBB)
     *
     * @param hex The hex color string, may be null
     * @return The parsed ARGB integer, or null if input is invalid
     */
    @Nullable
    public static Integer parseArgbColor(@Nullable String hex) {
        if (hex == null || hex.isBlank()) return null;

        String color = hex.startsWith("#") ? hex.substring(1) : hex;

        try {
            return switch (color.length()) {
                case 3 -> {
                    // #RGB -> expand to #FFRRGGBB
                    char r = color.charAt(0);
                    char g = color.charAt(1);
                    char b = color.charAt(2);
                    yield (int) Long.parseLong("FF" + r + r + g + g + b + b, 16);
                }
                case 6 -> {
                    // #RRGGBB -> assume FF alpha
                    yield (int) Long.parseLong("FF" + color, 16);
                }
                case 8 -> {
                    // #AARRGGBB -> full format
                    yield (int) Long.parseLong(color, 16);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Checks if this appearance has a custom shadow color.
     *
     * @return true if shadowColor is not null
     */
    public boolean hasShadowColor() {
        return shadowColor != null;
    }

    /**
     * Applies a text shadow color when the runtime Adventure API supports it.
     *
     * @param component   The component to modify
     * @param shadowColor The ARGB shadow color, or null
     * @return The modified component when supported, otherwise the original component
     */
    @NotNull
    public static Component applyShadowColor(@NotNull Component component, @Nullable Integer shadowColor) {
        if (shadowColor == null)
            return component;

        if (!SHADOW_COLOR_AVAILABLE) {
            warnShadowColorUnavailable();
            return component;
        }

        return ShadowColorApplier.apply(component, shadowColor);
    }

    private static boolean hasShadowColorApi() {
        try {
            Class.forName("net.kyori.adventure.text.format.ShadowColor", false, GlyphAppearance.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void warnShadowColorUnavailable() {
        if (!warnedShadowColorUnavailable.compareAndSet(false, true)) return;

        try {
            Logs.logWarning("A glyph is using appearance.shadow_color, but text shadow colors are not available on this server. This requires Minecraft/Adventure support for text shadow colors, so the configured shadow color will be ignored.");
        } catch (Throwable ignored) {
            // Logging can be unavailable in tests or early startup; ignore silently.
        }
    }

    /**
     * Isolates the direct {@link ShadowColor} usage in a class that is only loaded when the
     * runtime Adventure API provides it, so older servers never link the missing types.
     */
    private static final class ShadowColorApplier {
        private ShadowColorApplier() {
        }

        @NotNull
        static Component apply(@NotNull Component component, int shadowColor) {
            return component.shadowColor(ShadowColor.shadowColor(shadowColor));
        }
    }
}
