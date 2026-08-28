package io.th0rgal.oraxen.fonts;

import io.th0rgal.oraxen.glyphs.Glyph;
import io.th0rgal.oraxen.glyphs.GlyphGrid;
import io.th0rgal.oraxen.glyphs.ReferenceGlyph;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FontManagerTest {

    @Test
    void placeholderCompletionsAreKeptWhenUnicodeCompletionsAreEnabled() {
        Glyph glyph = createTabCompletableGlyph("sob", "ꐑ", ":sob:");

        List<String> completions = FontManager.getGlyphTabCompletions(List.of(glyph), true);

        assertEquals(List.of(":sob:", "ꐑ"), completions);
    }

    @Test
    void unicodeCompletionsCanBeDisabled() {
        Glyph glyph = createTabCompletableGlyph("sob", "ꐑ", ":sob:");

        List<String> completions = FontManager.getGlyphTabCompletions(List.of(glyph), false);

        assertEquals(List.of(":sob:"), completions);
    }

    @Test
    void referenceGlyphsParticipateInGlyphFeatures() {
        Glyph source = createTabCompletableGlyph("source", "ꐑꐒ", ":source:");
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection referenceSection = mock(ConfigurationSection.class);
        ConfigurationSection chatSection = mock(ConfigurationSection.class);
        when(section.getConfigurationSection("reference")).thenReturn(referenceSection);
        when(section.getConfigurationSection("chat")).thenReturn(chatSection);
        when(section.getBoolean("is_emoji", false)).thenReturn(true);
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(referenceSection.getString("glyph")).thenReturn("source");
        when(referenceSection.getString("index", "")).thenReturn("2");
        when(chatSection.getStringList("placeholders")).thenReturn(List.of(":reference:"));
        when(chatSection.getBoolean("tabcomplete", false)).thenReturn(true);
        when(chatSection.getString("permission", "")).thenReturn("");
        FontManager manager = mock(FontManager.class);
        when(manager.getGlyphFromID("source")).thenReturn(source);

        Glyph reference = new ReferenceGlyph("reference", section);
        assertTrue(((ReferenceGlyph) reference).resolve(manager));

        assertTrue(reference.isEmoji());
        assertEquals(List.of(":reference:", "ꐒ"), FontManager.getGlyphTabCompletions(List.of(reference), true));
    }

    private Glyph createTabCompletableGlyph(String name, String characters, String placeholder) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection chatSection = mock(ConfigurationSection.class);
        when(section.getConfigurationSection("chat")).thenReturn(chatSection);
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(chatSection.getStringList("placeholders")).thenReturn(List.of(placeholder));
        when(chatSection.getBoolean("tabcomplete", false)).thenReturn(true);
        when(chatSection.getString("permission", "")).thenReturn("");
        return new Glyph(name, section, List.of(characters), GlyphGrid.fromUnicodeRows(List.of(characters)));
    }
}
