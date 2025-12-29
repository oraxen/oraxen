package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Text effects that can be applied to any text using shader-based rendering.
 * <p>
 * Unlike animated glyphs which swap between sprite frames, text effects modify
 * how existing characters render (color, position, opacity) using GLSL shaders.
 * <p>
 * Effects use a magic color scheme with R=253 to distinguish from animated glyphs (R=254).
 * <p>
 * Color encoding: 0xFDGGBB where:
 * <ul>
 *   <li>FD = Red channel (253) - text effect marker</li>
 *   <li>GG = Green channel: effectType (bits 0-3) | speed (bits 4-7)</li>
 *   <li>BB = Blue channel: charIndex (bits 0-3) | param (bits 4-7)</li>
 * </ul>
 * <p>
 * Example configuration:
 * <pre>
 * TextEffects:
 *   enabled: true
 *   effects:
 *     rainbow:
 *       enabled: true
 *     wave:
 *       enabled: true
 *     shake:
 *       enabled: true
 *     pulse:
 *       enabled: true
 * </pre>
 */
public class TextEffect {

    /**
     * Magic red value for text effects (253).
     * Different from animated glyphs (254) to allow shader to distinguish.
     */
    public static final int MAGIC_RED = 0xFD; // 253

    /**
     * Default speed for effects (1-15).
     */
    public static final int DEFAULT_SPEED = 3;

    /**
     * Default parameter for effects (amplitude, intensity, etc.).
     */
    public static final int DEFAULT_PARAM = 3;

    /**
     * Effect types available for text.
     */
    public enum Type {
        /**
         * Rainbow effect - cycles hue over time per character.
         */
        RAINBOW(0, "rainbow"),

        /**
         * Wave effect - vertical sine wave motion per character.
         */
        WAVE(1, "wave"),

        /**
         * Shake effect - random jitter per character.
         */
        SHAKE(2, "shake"),

        /**
         * Pulse effect - opacity fades in/out.
         */
        PULSE(3, "pulse"),

        /**
         * Gradient effect - static color gradient across text.
         */
        GRADIENT(4, "gradient"),

        /**
         * Typewriter effect - characters appear sequentially.
         */
        TYPEWRITER(5, "typewriter"),

        /**
         * Wobble effect - rotation oscillation.
         */
        WOBBLE(6, "wobble"),

        /**
         * Obfuscate effect - rapidly cycling random characters.
         */
        OBFUSCATE(7, "obfuscate");

        private final int id;
        private final String name;

        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * Gets a Type by its string name (case-insensitive).
         */
        @Nullable
        public static Type fromName(String name) {
            for (Type type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * Gets a Type by its numeric ID.
         */
        @Nullable
        public static Type fromId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }

    // Static configuration for which effects are enabled
    private static final Map<Type, Boolean> enabledEffects = new LinkedHashMap<>();
    private static boolean globalEnabled = true;

    static {
        // Enable all effects by default
        for (Type type : Type.values()) {
            enabledEffects.put(type, true);
        }
    }

    /**
     * Loads text effect configuration from settings.
     *
     * @param section The TextEffects configuration section
     */
    public static void loadConfig(@Nullable ConfigurationSection section) {
        if (section == null) {
            globalEnabled = false;
            return;
        }

        globalEnabled = section.getBoolean("enabled", true);

        ConfigurationSection effectsSection = section.getConfigurationSection("effects");
        if (effectsSection != null) {
            for (Type type : Type.values()) {
                ConfigurationSection effectSection = effectsSection.getConfigurationSection(type.getName());
                if (effectSection != null) {
                    enabledEffects.put(type, effectSection.getBoolean("enabled", true));
                }
            }
        }
    }

    /**
     * Checks if text effects are globally enabled.
     */
    public static boolean isEnabled() {
        return globalEnabled;
    }

    /**
     * Checks if a specific effect type is enabled.
     */
    public static boolean isEffectEnabled(Type type) {
        return globalEnabled && enabledEffects.getOrDefault(type, true);
    }

    /**
     * Sets whether a specific effect type is enabled.
     */
    public static void setEffectEnabled(Type type, boolean enabled) {
        enabledEffects.put(type, enabled);
    }

    /**
     * Sets whether text effects are globally enabled.
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }

    /**
     * Gets the magic color for a text effect character.
     *
     * @param type       The effect type
     * @param speed      Speed of the effect (1-15)
     * @param charIndex  Character index for phase offset (0-15, wraps for longer text)
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-15)
     * @return Magic color encoding the effect parameters
     */
    @NotNull
    public static TextColor getMagicColor(Type type, int speed, int charIndex, int param) {
        int effectId = type.getId() & 0x0F;
        int speedClamped = Math.max(1, Math.min(15, speed)) & 0x0F;
        int charIndexClamped = charIndex & 0x0F;
        int paramClamped = param & 0x0F;

        int green = effectId | (speedClamped << 4);
        int blue = charIndexClamped | (paramClamped << 4);

        return TextColor.color(MAGIC_RED, green, blue);
    }

    /**
     * Applies a text effect to a string, returning a Component with magic colors.
     *
     * @param text   The text to apply the effect to
     * @param type   The effect type
     * @param speed  Speed of the effect (1-15)
     * @param param  Additional parameter (amplitude, intensity, etc.) (0-15)
     * @return Component with per-character magic colors
     */
    @NotNull
    public static Component apply(String text, Type type, int speed, int param) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int len = text.codePointCount(0, text.length());
        int idx = 0;

        for (int i = 0; i < text.length(); ) {
            int codepoint = text.codePointAt(i);
            int charIndex = idx % 16; // Wrap for long text

            TextColor magic = getMagicColor(type, speed, charIndex, param);
            result = result.append(
                    Component.text(Character.toString(codepoint))
                            .color(magic)
            );

            i += Character.charCount(codepoint);
            idx++;
        }

        return result;
    }

    /**
     * Applies a text effect with default parameters.
     */
    @NotNull
    public static Component apply(String text, Type type) {
        return apply(text, type, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    // Convenience methods for each effect type

    /**
     * Applies rainbow effect - cycles through hues over time.
     *
     * @param text  The text to colorize
     * @param speed How fast the rainbow cycles (1-15)
     * @return Component with rainbow effect colors
     */
    @NotNull
    public static Component rainbow(String text, int speed) {
        return apply(text, Type.RAINBOW, speed, 0);
    }

    /**
     * Applies rainbow effect with default speed.
     */
    @NotNull
    public static Component rainbow(String text) {
        return rainbow(text, DEFAULT_SPEED);
    }

    /**
     * Applies wave effect - vertical sine wave motion.
     *
     * @param text      The text to animate
     * @param speed     How fast the wave moves (1-15)
     * @param amplitude Wave amplitude (1-15)
     * @return Component with wave effect colors
     */
    @NotNull
    public static Component wave(String text, int speed, int amplitude) {
        return apply(text, Type.WAVE, speed, amplitude);
    }

    /**
     * Applies wave effect with default parameters.
     */
    @NotNull
    public static Component wave(String text) {
        return wave(text, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies shake effect - random jitter.
     *
     * @param text      The text to animate
     * @param speed     How fast the shake updates (1-15)
     * @param intensity Shake intensity (1-15)
     * @return Component with shake effect colors
     */
    @NotNull
    public static Component shake(String text, int speed, int intensity) {
        return apply(text, Type.SHAKE, speed, intensity);
    }

    /**
     * Applies shake effect with default parameters.
     */
    @NotNull
    public static Component shake(String text) {
        return shake(text, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies pulse effect - opacity fades in/out.
     *
     * @param text  The text to animate
     * @param speed How fast the pulse cycles (1-15)
     * @return Component with pulse effect colors
     */
    @NotNull
    public static Component pulse(String text, int speed) {
        return apply(text, Type.PULSE, speed, 0);
    }

    /**
     * Applies pulse effect with default speed.
     */
    @NotNull
    public static Component pulse(String text) {
        return pulse(text, DEFAULT_SPEED);
    }

    /**
     * Applies gradient effect - static color gradient.
     *
     * @param text The text to colorize
     * @return Component with gradient effect colors
     */
    @NotNull
    public static Component gradient(String text) {
        return apply(text, Type.GRADIENT, 0, 0);
    }

    /**
     * Applies typewriter effect - characters appear sequentially.
     *
     * @param text  The text to animate
     * @param speed How fast characters appear (1-15)
     * @return Component with typewriter effect colors
     */
    @NotNull
    public static Component typewriter(String text, int speed) {
        return apply(text, Type.TYPEWRITER, speed, 0);
    }

    /**
     * Applies typewriter effect with default speed.
     */
    @NotNull
    public static Component typewriter(String text) {
        return typewriter(text, DEFAULT_SPEED);
    }

    /**
     * Applies wobble effect - rotation oscillation.
     *
     * @param text      The text to animate
     * @param speed     How fast the wobble cycles (1-15)
     * @param amplitude Wobble amplitude (1-15)
     * @return Component with wobble effect colors
     */
    @NotNull
    public static Component wobble(String text, int speed, int amplitude) {
        return apply(text, Type.WOBBLE, speed, amplitude);
    }

    /**
     * Applies wobble effect with default parameters.
     */
    @NotNull
    public static Component wobble(String text) {
        return wobble(text, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies obfuscate effect - rapidly cycling random characters.
     *
     * @param text  The text to obfuscate
     * @param speed How fast characters cycle (1-15)
     * @return Component with obfuscate effect colors
     */
    @NotNull
    public static Component obfuscate(String text, int speed) {
        return apply(text, Type.OBFUSCATE, speed, 0);
    }

    /**
     * Applies obfuscate effect with default speed.
     */
    @NotNull
    public static Component obfuscate(String text) {
        return obfuscate(text, DEFAULT_SPEED);
    }

    /**
     * Checks if a color value represents a text effect.
     * Text effect colors have R=253 (0xFD).
     * Also checks for shadow variant (colors divided by 4).
     *
     * @param color The color value to check
     * @return true if this is a text effect magic color
     */
    public static boolean isTextEffectColor(int color) {
        int r = (color >> 16) & 0xFF;

        // Primary color: R=253
        if (r == MAGIC_RED) {
            return true;
        }

        // Shadow color: 253/4 ≈ 63
        int shadowRed = MAGIC_RED / 4;
        return r >= shadowRed - 1 && r <= shadowRed + 1;
    }

    /**
     * Extracts text effect parameters from a magic color.
     *
     * @param color The color value
     * @return int array of [effectType, speed, charIndex, param], or null if not a text effect
     */
    @Nullable
    public static int[] extractParams(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Primary color: R=253
        if (r == MAGIC_RED) {
            int effectType = g & 0x0F;
            int speed = (g >> 4) & 0x0F;
            int charIndex = b & 0x0F;
            int param = (b >> 4) & 0x0F;
            return new int[]{effectType, Math.max(1, speed), charIndex, param};
        }

        // Shadow color: multiply by 4 to recover
        int shadowRed = MAGIC_RED / 4;
        if (r >= shadowRed - 1 && r <= shadowRed + 1) {
            int originalG = Math.min(255, g * 4);
            int originalB = Math.min(255, b * 4);
            int effectType = originalG & 0x0F;
            int speed = (originalG >> 4) & 0x0F;
            int charIndex = originalB & 0x0F;
            int param = (originalB >> 4) & 0x0F;
            return new int[]{effectType, Math.max(1, speed), charIndex, param};
        }

        return null;
    }
}
