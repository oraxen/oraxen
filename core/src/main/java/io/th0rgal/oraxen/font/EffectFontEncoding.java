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
 * Color layout:
 * <ul>
 *   <li>R = 253 (0xFD) - primary marker</li>
 *   <li>G high nibble = effect type (0-7)</li>
 *   <li>G low nibble = 0xD (13) - secondary marker</li>
 *   <li>B = 0 (unused)</li>
 * </ul>
 * <p>
 * The shader matches the exact 24-bit RGB value, so false positives from gradients
 * are extremely unlikely. For example, effect 0 uses #FD0D00, effect 1 uses #FD1D00.
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

    private static final ShaderEncoding SHADER_ENCODING =
            new ShaderEncoding(4, 0x07, 0x0F, 0, -1, R_MARKER, true);

    @Override
    public String getName() {
        return "effect_font";
    }

    @Override
    @NotNull
    public TextColor encode(TextColor baseColor, int effectId, int speed, int param, int charIndex) {
        // R = marker (253)
        int r = R_MARKER;

        // G = (effectType << 4) | G_LOW_MARKER
        int effectType = effectId & 0x07;
        int g = (effectType << 4) | G_LOW_MARKER;

        // B = 0 (unused - speed/param come from config, baked into shader)
        int b = 0;

        return TextColor.color(r, g, b);
    }

    @Override
    public boolean matches(int rgb) {
        int color = rgb & 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Match exact trigger color format: R=253, G low nibble=0xD, B=0
        return r == R_MARKER && (g & LOW_NIBBLE_MASK) == G_LOW_MARKER && b == 0;
    }

    @Override
    @Nullable
    public Decoded decode(int rgb) {
        if (!matches(rgb)) {
            return null;
        }

        int color = rgb & 0xFFFFFF;
        int g = (color >> 8) & 0xFF;

        int effectType = (g >> 4) & 0x07;

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
