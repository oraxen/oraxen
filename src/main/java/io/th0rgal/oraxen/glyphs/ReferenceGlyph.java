package io.th0rgal.oraxen.glyphs;

import io.th0rgal.oraxen.fonts.*;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A reference glyph that aliases a subset of another glyph's characters.
 * <p>
 * Reference glyphs do NOT generate a separate font provider - they inherit
 * the texture/ascent/height from the source glyph. They can have their own
 * permissions and placeholders.
 * <p>
 * Example configuration:
 * 
 * <pre>
 * gui_header:
 *   reference:
 *     glyph: full_gui      # Source glyph ID
 *     index: 1..3          # Characters to include (1-indexed)
 *   chat:
 *     placeholders: [":header:"]
 *     permission: "gui.header"
 * </pre>
 */
public class ReferenceGlyph extends Glyph {

    private final String sourceGlyphId;
    private final IntRange indexRange;

    private Glyph resolvedSourceGlyph;
    private String resolvedCharacters;

    /**
     * Creates a reference glyph from configuration.
     *
     * @param glyphName    The name/ID of this reference glyph
     * @param glyphSection The configuration section containing reference settings
     */
    public ReferenceGlyph(String glyphName, ConfigurationSection glyphSection) {
        super(glyphName, glyphSection, List.of(""), GlyphGrid.SINGLE);

        ConfigurationSection refSection = glyphSection.getConfigurationSection("reference");
        if (refSection == null) {
            throw new IllegalArgumentException("Reference glyph '" + glyphName + "' missing 'reference' section");
        }

        this.sourceGlyphId = refSection.getString("glyph");
        if (sourceGlyphId == null || sourceGlyphId.isBlank()) {
            throw new IllegalArgumentException("Reference glyph '" + glyphName + "' missing 'reference.glyph'");
        }

        String indexString = refSection.getString("index", "");
        IntRange parsedRange = indexString.isBlank() ? IntRange.all() : IntRange.parse(indexString);
        if (parsedRange == null) {
            Logs.logWarning(
                    "Reference glyph '" + glyphName + "' has invalid index: " + indexString + ", using all characters");
            parsedRange = IntRange.all();
        }
        this.indexRange = parsedRange;
    }

    /**
     * Checks if a configuration section defines a reference glyph.
     *
     * @param section The configuration section to check
     * @return true if the section contains a 'reference' subsection
     */
    public static boolean isReferenceGlyph(@Nullable ConfigurationSection section) {
        return section != null && section.isConfigurationSection("reference");
    }

    /**
     * Resolves the source glyph reference.
     * Must be called after all regular glyphs are loaded.
     *
     * @param fontManager The font manager to look up the source glyph
     * @return true if resolution succeeded, false if source glyph not found
     */
    public boolean resolve(FontManager fontManager) {
        Glyph source = fontManager.getGlyphFromID(sourceGlyphId);
        if (source == null) {
            Logs.logError("Reference glyph '" + getName() + "' references non-existent glyph: " + sourceGlyphId);
            return false;
        }

        this.resolvedSourceGlyph = source;
        this.resolvedCharacters = extractCharacters(source);

        if (resolvedCharacters.isEmpty()) {
            Logs.logWarning("Reference glyph '" + getName() + "' resolved to empty characters from " + sourceGlyphId);
        }

        return true;
    }

    /**
     * Extracts the characters from the source glyph based on the index range.
     */
    private String extractCharacters(Glyph source) {
        char[] allChars = source.getAllChars();
        if (allChars.length == 0) {
            return "";
        }

        IntRange range = indexRange != null ? indexRange : IntRange.all();
        IntRange clamped = range.clamp(1, allChars.length);
        if (clamped == null) {
            return "";
        }

        // Convert to 0-based for array access
        IntRange zeroBased = clamped.toZeroBased();
        StringBuilder sb = new StringBuilder();
        for (int i = zeroBased.start(); i <= zeroBased.end() && i < allChars.length; i++) {
            sb.append(allChars[i]);
        }
        return sb.toString();
    }

    /**
     * Checks if this reference glyph has been resolved.
     *
     * @return true if resolve() was called successfully
     */
    public boolean isResolved() {
        return resolvedSourceGlyph != null;
    }

    public String getSourceGlyphId() {
        return sourceGlyphId;
    }

    @Nullable
    public IntRange getIndexRange() {
        return indexRange;
    }

    /**
     * Gets the resolved source glyph.
     *
     * @return The source glyph, or null if not resolved
     */
    @Nullable
    public Glyph getSourceGlyph() {
        return resolvedSourceGlyph;
    }

    /**
     * Gets the first character (for backward compatibility).
     *
     * @return The first character, or empty string if not resolved
     */
    public String getCharacter() {
        if (resolvedCharacters == null || resolvedCharacters.isEmpty()) {
            return "";
        }
        return String.valueOf(resolvedCharacters.charAt(0));
    }

    /**
     * Gets all resolved characters as a string.
     *
     * @return The resolved character string
     */
    public String getCharacters() {
        return resolvedCharacters != null ? resolvedCharacters : "";
    }

    @Override
    public List<String> getUnicodeRows() {
        return List.of(getCharacters());
    }

    @Override
    public String getFormattedUnicodes() {
        return getCharacters();
    }

    /**
     * Gets all resolved characters as a char array.
     *
     * @return The resolved characters
     */
    public char[] getAllChars() {
        return getCharacters().toCharArray();
    }

    /**
     * Gets the font key from the source glyph.
     *
     * @return The font key, or default if not resolved
     */
    @NotNull
    public Key getFont() {
        return resolvedSourceGlyph != null
                ? resolvedSourceGlyph.getFont()
                : Key.key("minecraft", "default");
    }

    /**
     * Gets the texture from the source glyph.
     */
    public String getTexture() {
        return resolvedSourceGlyph != null ? resolvedSourceGlyph.getTexture() : "";
    }

    /**
     * Gets the ascent from the source glyph.
     */
    public int getAscent() {
        return resolvedSourceGlyph != null ? resolvedSourceGlyph.getAscent() : 8;
    }

    /**
     * Gets the height from the source glyph.
     */
    public int getHeight() {
        return resolvedSourceGlyph != null ? resolvedSourceGlyph.getHeight() : 8;
    }

    /**
     * Reference glyphs do NOT generate their own font provider.
     * They use the same characters from the source glyph.
     *
     * @return null (no separate JSON needed)
     */
    @Nullable
    public JsonObject toJson() {
        return null; // Reference glyphs don't generate separate providers
    }

    public Component getGlyphComponent() {
        Component component = Component.text(getCharacters(), NamedTextColor.WHITE)
                .font(getFont());
        return GlyphAppearance.applyShadowColor(component, getAppearance().shadowColor());
    }

    /**
     * Gets the appearance configuration from the source glyph.
     */
    @NotNull
    public GlyphAppearance getAppearance() {
        return resolvedSourceGlyph != null
                ? resolvedSourceGlyph.getAppearance()
                : GlyphAppearance.DEFAULT;
    }
}
