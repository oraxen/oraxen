package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Text effects that can be applied to any text using shader-based rendering.
 * <p>
 * Unlike animated glyphs which swap between sprite frames, text effects modify
 * how existing characters render (color, position, opacity) using GLSL shaders.
 * <p>
 * Effects are encoded into the low bits of RGB using a configurable strategy
 * (default: {@link AlphaLsbEncoding}). This preserves the base color while
 * providing effect data to the shader.
 * <p>
 * Default encoding (alpha_lsb):
 * <ul>
 *   <li>Low 4 bits of each channel are reserved</li>
 *   <li>Bit 3 is a marker, bits 0-2 carry data (0-7)</li>
 *   <li>R -> effectType, G -> speed, B -> param</li>
 *   <li>Character index is derived in the shader from {@code gl_VertexID}</li>
 * </ul>
 * <p>
 * Example configuration:
 * <pre>
 * TextEffects:
 *   enabled: true
 *   shader:
 *     template: auto
 *     encoding: alpha_lsb
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
     * Legacy magic red value for the deprecated R=253 encoding.
     *
     * @deprecated Replaced by {@link AlphaLsbEncoding}; kept for compatibility.
     */
    @Deprecated
    public static final int MAGIC_RED = 0xFD; // 253

    /**
     * Legacy marker bits for the deprecated encoding.
     *
     * @deprecated Replaced by {@link AlphaLsbEncoding}; kept for compatibility.
     */
    @Deprecated
    public static final int EFFECT_MARKER = 0x88;

    /**
     * Default speed for effects (1-7).
     */
    public static final int DEFAULT_SPEED = 3;

    /**
     * Default parameter for effects (amplitude, intensity, etc.) (0-7).
     */
    public static final int DEFAULT_PARAM = 3;

    /**
     * Default base color when none is specified.
     */
    private static final TextColor DEFAULT_BASE_COLOR = TextColor.color(255, 255, 255);

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
         * Wobble effect - circular oscillation.
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

    /**
     * Shader template selection for text rendering.
     */
    public enum ShaderTemplate {
        AUTO("auto"),
        TEXT_EFFECTS("effects_only"),
        ANIMATED_GLYPHS("animated_only");

        private final String name;

        ShaderTemplate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Nullable
        public static ShaderTemplate fromName(@Nullable String name) {
            if (name == null) return null;
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            for (ShaderTemplate template : values()) {
                if (template.name.equals(normalized)) {
                    return template;
                }
            }

            return switch (normalized) {
                case "effects", "text_effects", "text-effects" -> TEXT_EFFECTS;
                case "animated", "animated_glyphs", "animated-glyphs" -> ANIMATED_GLYPHS;
                default -> null;
            };
        }
    }

    // Static configuration for which effects are enabled
    private static final Map<Type, Boolean> enabledEffects = new LinkedHashMap<>();
    private static boolean globalEnabled = true;
    private static TextEffectEncoding encoding = new AlphaLsbEncoding();
    private static ShaderTemplate shaderTemplate = ShaderTemplate.AUTO;

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
            encoding = new AlphaLsbEncoding();
            shaderTemplate = ShaderTemplate.AUTO;
            return;
        }

        globalEnabled = section.getBoolean("enabled", true);
        loadShaderConfig(section.getConfigurationSection("shader"));

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

    private static void loadShaderConfig(@Nullable ConfigurationSection shaderSection) {
        ShaderTemplate parsedTemplate = ShaderTemplate.AUTO;
        TextEffectEncoding parsedEncoding = new AlphaLsbEncoding();

        if (shaderSection != null) {
            String templateName = shaderSection.getString("template", ShaderTemplate.AUTO.getName());
            ShaderTemplate template = ShaderTemplate.fromName(templateName);
            if (template == null) {
                Logs.logWarning("Unknown TextEffects.shader.template '" + templateName + "', using 'auto'.");
            } else {
                parsedTemplate = template;
            }

            String encodingName = shaderSection.getString("encoding", parsedEncoding.getName());
            parsedEncoding = resolveEncoding(encodingName);
        }

        shaderTemplate = parsedTemplate;
        encoding = parsedEncoding;
    }

    private static TextEffectEncoding resolveEncoding(@Nullable String name) {
        if (name == null) {
            return new AlphaLsbEncoding();
        }

        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("alpha_lsb") || normalized.equals("alpha-lsb") || normalized.equals("lsb")) {
            return new AlphaLsbEncoding();
        }

        Logs.logWarning("Unknown TextEffects.shader.encoding '" + name + "', using 'alpha_lsb'.");
        return new AlphaLsbEncoding();
    }

    /**
     * Checks if text effects are globally enabled.
     */
    public static boolean isEnabled() {
        return globalEnabled;
    }

    /**
     * Returns the configured shader template selection.
     */
    public static ShaderTemplate getShaderTemplate() {
        return shaderTemplate;
    }

    /**
     * Returns the configured encoding strategy.
     */
    public static TextEffectEncoding getEncoding() {
        return encoding;
    }

    /**
     * Checks if a specific effect type is enabled.
     */
    public static boolean isEffectEnabled(Type type) {
        return globalEnabled && enabledEffects.getOrDefault(type, true);
    }

    /**
     * Returns true if at least one effect is enabled.
     */
    public static boolean hasAnyEffectEnabled() {
        if (!globalEnabled) return false;
        return enabledEffects.values().stream().anyMatch(Boolean::booleanValue);
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
     * Gets the encoded color for a text effect character, using the default base color.
     *
     * @param type       The effect type
     * @param speed      Speed of the effect (1-7)
     * @param charIndex  Character index for phase offset (0-7, wraps for longer text);
     *                   some encodings derive this in the shader and may ignore it.
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Encoded color carrying the effect parameters
     */
    @NotNull
    public static TextColor getMagicColor(Type type, int speed, int charIndex, int param) {
        return getMagicColor(DEFAULT_BASE_COLOR, type, speed, charIndex, param);
    }

    /**
     * Gets the encoded color for a text effect character, preserving the base color.
     *
     * @param baseColor  Base color to preserve
     * @param type       The effect type
     * @param speed      Speed of the effect (1-7)
     * @param charIndex  Character index for phase offset (0-7, wraps for longer text)
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Encoded color carrying the effect parameters
     */
    @NotNull
    public static TextColor getMagicColor(@NotNull TextColor baseColor, Type type, int speed, int charIndex, int param) {
        return encoding.encode(baseColor, type, speed, param, charIndex);
    }

    /**
     * Applies a text effect to a string, returning a Component with encoded colors.
     *
     * @param text   The text to apply the effect to
     * @param type   The effect type
     * @param speed  Speed of the effect (1-7)
     * @param param  Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Component with per-character encoded colors
     */
    @NotNull
    public static Component apply(String text, Type type, int speed, int param) {
        return apply(text, type, speed, param, null);
    }

    /**
     * Applies a text effect to a string while preserving the provided base color.
     *
     * @param text      The text to apply the effect to
     * @param type      The effect type
     * @param speed     Speed of the effect (1-7)
     * @param param     Additional parameter (amplitude, intensity, etc.) (0-7)
     * @param baseColor Base color to preserve, or null to use default (white)
     * @return Component with per-character encoded colors
     */
    @NotNull
    public static Component apply(String text, Type type, int speed, int param, @Nullable TextColor baseColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        TextColor effectiveBase = baseColor != null ? baseColor : DEFAULT_BASE_COLOR;
        Component result = Component.empty();
        int idx = 0;
        int mask = encoding.shaderEncoding().dataMask();

        for (int i = 0; i < text.length(); ) {
            int codepoint = text.codePointAt(i);
            int charIndex = mask > 0 ? (idx % (mask + 1)) : 0; // Wrap for long text

            TextColor magic = getMagicColor(effectiveBase, type, speed, charIndex, param);
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
     * @param speed How fast the rainbow cycles (1-7)
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
     * @param speed     How fast the wave moves (1-7)
     * @param amplitude Wave amplitude (1-7)
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
     * @param speed     How fast the shake updates (1-7)
     * @param intensity Shake intensity (1-7)
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
     * @param speed How fast the pulse cycles (1-7)
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
     * @param speed How fast characters appear (1-7)
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
     * Applies wobble effect - circular oscillation.
     *
     * @param text      The text to animate
     * @param speed     How fast the wobble cycles (1-7)
     * @param amplitude Wobble amplitude (1-7)
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
     * @param speed How fast characters cycle (1-7)
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
     *
     * @param color The color value to check
     * @return true if this is a text effect magic color
     */
    public static boolean isTextEffectColor(int color) {
        return encoding.matches(color);
    }

    /**
     * Extracts text effect parameters from a magic color.
     *
     * @param color The color value
     * @return int array of [effectType, speed, charIndex, param], or null if not a text effect.
     *         Some encodings derive charIndex in the shader; in that case charIndex may be 0 here.
     */
    @Nullable
    public static int[] extractParams(int color) {
        TextEffectEncoding.Decoded decoded = encoding.decode(color);
        if (decoded == null) return null;
        return new int[]{decoded.effectType(), decoded.speed(), decoded.charIndex(), decoded.param()};
    }
}
