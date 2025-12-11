package io.th0rgal.oraxen.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides shift functionality using Minecraft's native "space" font provider
 * (1.19.4+).
 * This replaces the legacy bitmap-based shift glyphs from shifts.yml with a
 * single
 * efficient space provider that maps characters to pixel advances.
 * <p>
 * The space provider supports advances from -1024 to +1024 pixels in powers of
 * 2,
 * allowing any arbitrary shift value to be composed from a combination of
 * characters.
 * <p>
 * Uses a dedicated font file (oraxen:shift) to avoid conflicts with glyph
 * codepoints
 * in the default font.
 */
public class ShiftProvider {

    /**
     * The font key for shift characters.
     * Uses a dedicated font to isolate shift characters from glyphs.
     */
    public static final Key FONT_KEY = Key.key("oraxen", "shift");

    /**
     * Starting codepoint for shift characters.
     * Uses BMP Private Use Area starting at U+F800.
     * AnimatedGlyph reserves U+E000-U+F7FF, so this range is safely separated.
     * Note: Even though these use separate font files (oraxen:shift vs
     * oraxen:animations/*),
     * keeping codepoint ranges distinct avoids confusion and potential issues.
     */
    private static final int BASE_CODEPOINT = 0xF800;

    /**
     * Maximum power of 2 supported (1024 = 2^10).
     */
    private static final int MAX_POWER = 10;

    /**
     * Map of codepoints to their pixel advances.
     * Includes both positive and negative advances.
     * All codepoints fit within BMP (single char).
     */
    private final Map<Integer, Integer> advances;

    /**
     * Codepoints for positive shifts (powers of 2: 1, 2, 4, ..., 1024).
     * All values fit within BMP range.
     */
    private final int[] positiveShiftCodepoints;

    /**
     * Codepoints for negative shifts (powers of 2: -1, -2, -4, ..., -1024).
     * All values fit within BMP range.
     */
    private final int[] negativeShiftCodepoints;

    public ShiftProvider() {
        this.advances = new LinkedHashMap<>();
        this.positiveShiftCodepoints = new int[MAX_POWER + 1];
        this.negativeShiftCodepoints = new int[MAX_POWER + 1];

        int codepoint = BASE_CODEPOINT;

        // Generate positive shift codepoints (1, 2, 4, 8, ..., 1024)
        for (int power = 0; power <= MAX_POWER; power++) {
            int cp = codepoint++;
            int advance = 1 << power; // 2^power
            positiveShiftCodepoints[power] = cp;
            advances.put(cp, advance);
        }

        // Generate negative shift codepoints (-1, -2, -4, -8, ..., -1024)
        for (int power = 0; power <= MAX_POWER; power++) {
            int cp = codepoint++;
            int advance = -(1 << power); // -2^power
            negativeShiftCodepoints[power] = cp;
            advances.put(cp, advance);
        }
    }

    /**
     * Gets a string of characters that, when rendered, will shift text by the
     * specified pixels.
     * Uses binary decomposition to find the optimal combination of shift
     * characters.
     *
     * @param pixels The number of pixels to shift (positive = right, negative =
     *               left)
     * @return A string of shift characters that combine to the requested pixel
     *         shift
     */
    public String getShiftString(int pixels) {
        if (pixels == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean negative = pixels < 0;
        int remaining = Math.abs(pixels);
        int[] shiftCodepoints = negative ? negativeShiftCodepoints : positiveShiftCodepoints;

        // Decompose into powers of 2 from largest to smallest
        for (int power = MAX_POWER; power >= 0 && remaining > 0; power--) {
            int value = 1 << power;
            while (remaining >= value) {
                // Use Character.toString(int) to properly handle supplementary codepoints
                result.append(Character.toString(shiftCodepoints[power]));
                remaining -= value;
            }
        }

        return result.toString();
    }

    /**
     * Gets the maximum shift value supported in a single direction.
     *
     * @return The maximum supported shift (2047 pixels = sum of all powers from 0
     *         to 10)
     */
    public int getMaxShift() {
        // Sum of 1 + 2 + 4 + ... + 1024 = 2^11 - 1 = 2047
        return (1 << (MAX_POWER + 1)) - 1;
    }

    /**
     * Generates the space provider JSON object.
     *
     * @return JsonObject representing the space font provider
     */
    public JsonObject toProviderJson() {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "space");

        JsonObject advancesJson = new JsonObject();
        for (Map.Entry<Integer, Integer> entry : advances.entrySet()) {
            // Use Character.toString(int) to properly handle supplementary codepoints
            advancesJson.addProperty(Character.toString(entry.getKey()), entry.getValue());
        }
        provider.add("advances", advancesJson);

        return provider;
    }

    /**
     * Generates the complete font file JSON for the shift font.
     * This creates a standalone font file at assets/oraxen/font/shift.json
     *
     * @return JsonObject representing the complete font file
     */
    public JsonObject generateFontFile() {
        JsonObject font = new JsonObject();
        JsonArray providers = new JsonArray();
        providers.add(toProviderJson());
        font.add("providers", providers);
        return font;
    }

    /**
     * Gets the font key for shift characters.
     *
     * @return The Adventure Key for the shift font (oraxen:shift)
     */
    public Key getFontKey() {
        return FONT_KEY;
    }

    /**
     * Gets all codepoints used by this shift provider.
     * Useful for excluding these codepoints from other processing.
     *
     * @return Array of all shift codepoints
     */
    public int[] getAllShiftCodepoints() {
        int[] all = new int[advances.size()];
        int i = 0;
        for (int cp : advances.keySet()) {
            all[i++] = cp;
        }
        return all;
    }

    /**
     * Checks if a codepoint is a shift codepoint.
     *
     * @param codepoint The codepoint to check
     * @return true if the codepoint is used for shifting
     */
    public boolean isShiftCodepoint(int codepoint) {
        return advances.containsKey(codepoint);
    }

    /**
     * Gets the pixel advance for a specific codepoint.
     *
     * @param codepoint The shift codepoint
     * @return The pixel advance, or 0 if not a shift codepoint
     */
    public int getAdvance(int codepoint) {
        return advances.getOrDefault(codepoint, 0);
    }
}
