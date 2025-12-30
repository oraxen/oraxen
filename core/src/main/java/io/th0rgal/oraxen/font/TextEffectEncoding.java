package io.th0rgal.oraxen.font;

import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;

/**
 * Encodes text effect parameters into RGB colors that shaders can decode.
 * Implementations should strive to preserve the base color as much as possible.
 */
public interface TextEffectEncoding {

    /**
     * Identifier used in configuration (e.g. "alpha_lsb").
     */
    String getName();

    /**
     * Encodes the effect parameters into the base color.
     *
     * @param baseColor Base color to preserve as much as possible
     * @param effectId Effect type id
     * @param speed Effect speed (1-7)
     * @param param Effect parameter (0-7)
     * @param charIndex Character index (0-7); may be ignored by some encodings
     */
    TextColor encode(TextColor baseColor, int effectId, int speed, int param, int charIndex);

    /**
     * Checks whether the encoded color matches this encoding scheme.
     */
    boolean matches(int rgb);

    /**
     * Decodes effect parameters from an encoded color, or returns null if not encoded.
     */
    @Nullable
    Decoded decode(int rgb);

    /**
     * Returns shader-side constants required for decoding.
     */
    ShaderEncoding shaderEncoding();

    /**
     * Decoded parameters from a magic color.
     */
    record Decoded(int effectType, int speed, int charIndex, int param) {
    }

    /**
     * Shader-side constants for decoding.
     *
     * @param lsbBits Number of low bits reserved for encoding
     * @param dataMask Mask for data bits inside the low bits
     * @param lowMask Mask for low bits
     * @param dataMin Lowest allowed encoded low-nibble value
     * @param dataGap Optional gap value to skip (set to -1 for none)
     * @param charIndexFromVertexId Whether shader should derive char index from gl_VertexID
     */
    record ShaderEncoding(int lsbBits, int dataMask, int lowMask, int dataMin, int dataGap, boolean charIndexFromVertexId) {
        public int dataMax() {
            int max = dataMin + dataMask;
            if (dataGap >= dataMin && dataGap <= max) {
                max += 1;
            }
            return max;
        }

        public boolean hasGap() {
            return dataGap >= 0;
        }
    }
}
