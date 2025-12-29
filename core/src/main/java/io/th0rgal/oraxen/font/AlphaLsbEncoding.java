package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Encodes text effects into the lowest 4 bits of each RGB channel.
 * <p>
 * Layout (low 4 bits per channel):
 * - values {@code DATA_MIN..DATA_MAX} carry data (0-7), skipping {@code DATA_GAP}
 * <p>
 * R low bits: effect type
 * G low bits: speed
 * B low bits: param
 * <p>
 * Character index is derived from gl_VertexID in the shader.
 * Red channel values that collide with animated glyph sentinels are nudged
 * by one high-nibble step to avoid false positives.
 * <p>
 * Note: Adventure TextColor is RGB-only, so alpha cannot be encoded directly.
 */
public final class AlphaLsbEncoding implements TextEffectEncoding {

    public static final int LSB_BITS = 4;
    public static final int LOW_MASK = (1 << LSB_BITS) - 1; // 0x0F
    public static final int DATA_MASK = (1 << (LSB_BITS - 1)) - 1; // 0x07
    public static final int DATA_MIN = 1;
    public static final int DATA_GAP = 5;
    public static final int DATA_MAX = computeDataMax();

    private static final ShaderEncoding SHADER_ENCODING =
            new ShaderEncoding(LSB_BITS, DATA_MASK, LOW_MASK, DATA_MIN, DATA_GAP, true);

    @Override
    public String getName() {
        return "alpha_lsb";
    }

    @Override
    @NotNull
    public TextColor encode(TextColor baseColor, TextEffect.Type type, int speed, int param, int charIndex) {
        int rgb = baseColor.value();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int effectId = type.getId() & DATA_MASK;
        int speedClamped = clamp(speed, 1, DATA_MASK);
        int paramClamped = clamp(param, 0, DATA_MASK);

        int rEnc = avoidAnimationSentinels(encodeChannel(r, effectId));
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

    private static int avoidAnimationSentinels(int red) {
        // Avoid R=254 and shadow range (62-64) which are reserved for animated glyphs.
        if (red == 254) return red - 16;
        if (red >= 62 && red <= 64) return red + 16;
        return red;
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
