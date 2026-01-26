package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encodes text effects using exact trigger colors that the shader matches precisely.
 * <p>
 * This encoding is used exclusively with effect fonts. Since effect fonts are
 * explicitly opt-in, we can use the color space freely for encoding without
 * needing to preserve the original color.
 * <p>
 * The shader matches the exact 24-bit RGB value configured per-effect in text_effects.yml.
 * Each effect has a unique trigger_color that is matched precisely by the shader.
 * <p>
 * Default color layout (for backwards compatibility with effects 0-7):
 * <ul>
 *   <li>R = 253 (0xFD) - primary marker</li>
 *   <li>G high nibble = effect type (0-7)</li>
 *   <li>G low nibble = 0xD (13) - secondary marker</li>
 *   <li>B = 0 (unused)</li>
 * </ul>
 * <p>
 * For effects 8+, unique colors are auto-generated using a spread algorithm
 * that distributes colors across the color space to minimize collision risk.
 * <p>
 * The encoding now supports up to 256 effects (0-255) instead of the previous 8 (0-7).
 */
public final class EffectFontEncoding implements TextEffectEncoding {

    /**
     * Primary marker: R channel must equal this value.
     */
    public static final int R_MARKER = 0xFD; // 253

    /**
     * Secondary marker: G channel low nibble must equal this value.
     */
    public static final int G_LOW_MARKER = 0x0D; // 13

    /**
     * Mask for extracting the low nibble.
     */
    public static final int LOW_NIBBLE_MASK = 0x0F;

    /**
     * Mask for extracting the high nibble.
     */
    public static final int HIGH_NIBBLE_MASK = 0xF0;

    /**
     * Maximum number of effects supported (0-255).
     */
    public static final int MAX_EFFECTS = 256;

    /**
     * Data mask for effect IDs (0xFF = 255, supporting 256 effects).
     */
    public static final int EFFECT_ID_MASK = 0xFF;

    private static final ShaderEncoding SHADER_ENCODING =
            new ShaderEncoding(8, EFFECT_ID_MASK, 0xFF, 0, -1, -1, true);

    @Override
    public String getName() {
        return "effect_font";
    }

    @Override
    @NotNull
    public TextColor encode(TextColor baseColor, int effectId, int speed, int param, int charIndex) {
        // For effect font encoding, the trigger color comes from the TextEffect.Definition
        // This method generates a default trigger color based on effect ID for legacy compatibility
        // The actual trigger color used is from Definition.getTriggerColor()
        return generateDefaultTriggerColor(effectId);
    }

    /**
     * Generates a default trigger color for an effect ID.
     * <p>
     * For IDs 0-7: Uses the legacy format #FDxD00 for backwards compatibility.
     * For IDs 8+: Uses a spread algorithm to generate unique colors across the color space.
     *
     * @param effectId The effect ID (0-255)
     * @return A unique trigger color for this effect
     */
    @NotNull
    public static TextColor generateDefaultTriggerColor(int effectId) {
        int id = effectId & EFFECT_ID_MASK;

        if (id < 8) {
            // Legacy format for effects 0-7: #FDxD00
            int r = R_MARKER;
            int g = (id << 4) | G_LOW_MARKER;
            int b = 0;
            return TextColor.color(r, g, b);
        }

        // For effects 8+, generate unique colors using a spread algorithm
        // Use R = 0xFD (marker), vary G and B to spread across color space
        int r = R_MARKER;
        int g = id; // Use effect ID directly for G channel
        int b = ((id * 37) & 0xFF); // Spread B using prime multiplier for better distribution

        return TextColor.color(r, g, b);
    }

    @Override
    public boolean matches(int rgb) {
        int color = rgb & 0xFFFFFF;
        int r = (color >> 16) & 0xFF;

        // With the new unlimited effects system, we check if R channel matches our marker
        // The actual effect matching is done by the shader using exact color comparison
        // against the ORAXEN_EFFECT_TRIGGERS array, not by this method
        return r == R_MARKER;
    }

    @Override
    @Nullable
    public Decoded decode(int rgb) {
        if (!matches(rgb)) {
            return null;
        }

        int color = rgb & 0xFFFFFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // For legacy colors (effects 0-7): G high nibble contains effect ID, B=0
        // For new colors (effects 8+): G contains effect ID directly
        int effectType;
        if (b == 0 && (g & LOW_NIBBLE_MASK) == G_LOW_MARKER) {
            // Legacy format: extract from G high nibble
            effectType = (g >> 4) & 0x07;
        } else {
            // New format: G channel is the effect ID
            effectType = g & EFFECT_ID_MASK;
        }

        // Speed and param come from config, not encoding
        // Return defaults here for API compatibility
        return new Decoded(effectType, 3, 0, 3);
    }

    @Override
    public ShaderEncoding shaderEncoding() {
        return SHADER_ENCODING;
    }

    /**
     * Returns the G channel low nibble marker value.
     */
    public static int getGLowMarker() {
        return G_LOW_MARKER;
    }
}
