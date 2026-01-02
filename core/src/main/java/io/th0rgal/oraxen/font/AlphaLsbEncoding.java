package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encodes text effects into the lowest 4 bits of each RGB channel,
 * with a high-nibble marker on the red channel for opt-in detection.
 * <p>
 * Layout:
 * - R high nibble: must be {@code R_HIGH_MARKER} (0xF0) for detection
 * - R low nibble: effect type (0-7)
 * - G low nibble: speed (1-7)
 * - B low nibble: param (0-7)
 * <p>
 * The high-nibble marker ensures only intentionally encoded colors
 * trigger text effects, avoiding false positives from natural colors.
 * <p>
 * Character index is derived from gl_VertexID in the shader.
 * <p>
 * Note: Adventure TextColor is RGB-only, so alpha cannot be encoded directly.
 */
public final class AlphaLsbEncoding implements TextEffectEncoding {

    public static final int LSB_BITS = 4;
    public static final int LOW_MASK = (1 << LSB_BITS) - 1; // 0x0F
    public static final int HIGH_MASK = 0xF0;
    public static final int DATA_MASK = (1 << (LSB_BITS - 1)) - 1; // 0x07
    public static final int DATA_MIN = 1;
    public static final int DATA_GAP = 5;
    public static final int DATA_MAX = computeDataMax();

    /**
     * High nibble marker for the red channel.
     * R must have (R & 0xF0) == 0xF0 to be recognized as effect text.
     * This gives R values 0xF1-0xF9 (241-249) excluding 0xF5 (245) due to DATA_GAP.
     */
    public static final int R_HIGH_MARKER = 0xF0;

    private static final ShaderEncoding SHADER_ENCODING =
            new ShaderEncoding(LSB_BITS, DATA_MASK, LOW_MASK, DATA_MIN, DATA_GAP, R_HIGH_MARKER, true);

    @Override
    public String getName() {
        return "alpha_lsb";
    }

    @Override
    @NotNull
    public TextColor encode(TextColor baseColor, int effectId, int speed, int param, int charIndex) {
        int rgb = baseColor.value();
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int effectValue = effectId & DATA_MASK;
        int speedClamped = clamp(speed, 1, DATA_MASK);
        int paramClamped = clamp(param, 0, DATA_MASK);

        // Force R high nibble to R_HIGH_MARKER for opt-in detection
        int rEnc = R_HIGH_MARKER | encodeNibble(effectValue);
        int gEnc = encodeChannel(g, speedClamped);
        int bEnc = encodeChannel(b, paramClamped);

        return TextColor.color(rEnc, gEnc, bEnc);
    }

    @Override
    public boolean matches(int rgb) {
        int color = rgb & 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Check high nibble marker on R channel
        if ((r & HIGH_MASK) != R_HIGH_MARKER) {
            return false;
        }

        int rLow = r & LOW_MASK;
        int gLow = g & LOW_MASK;
        int bLow = b & LOW_MASK;

        return isEncodedNibble(rLow) && isEncodedNibble(gLow) && isEncodedNibble(bLow);
    }

    @Override
    @Nullable
    public Decoded decode(int rgb) {
        if (!matches(rgb)) {
            return null;
        }

        int color = rgb & 0xFFFFFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int rLow = r & LOW_MASK;
        int gLow = g & LOW_MASK;
        int bLow = b & LOW_MASK;

        int effectType = decodeNibble(rLow);
        int speed = clamp(decodeNibble(gLow), 1, DATA_MASK);
        int param = decodeNibble(bLow);

        // Char index is derived in the shader (gl_VertexID).
        return new Decoded(effectType, speed, 0, param);
    }

    @Override
    public ShaderEncoding shaderEncoding() {
        return SHADER_ENCODING;
    }

    private static int encodeChannel(int base, int data) {
        return (base & ~LOW_MASK) | encodeNibble(data);
    }

    private static boolean isEncodedNibble(int low) {
        if (low < DATA_MIN || low > DATA_MAX) {
            return false;
        }
        return DATA_GAP < 0 || low != DATA_GAP;
    }

    private static int encodeNibble(int data) {
        int encoded = (data & DATA_MASK) + DATA_MIN;
        if (DATA_GAP >= 0 && encoded >= DATA_GAP) {
            encoded += 1;
        }
        return encoded;
    }

    private static int decodeNibble(int low) {
        int decoded = low - DATA_MIN;
        if (DATA_GAP >= 0 && low > DATA_GAP) {
            decoded -= 1;
        }
        return decoded;
    }

    private static int computeDataMax() {
        int max = DATA_MIN + DATA_MASK;
        if (DATA_GAP >= DATA_MIN && DATA_GAP <= max) {
            max += 1;
        }
        return max;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
