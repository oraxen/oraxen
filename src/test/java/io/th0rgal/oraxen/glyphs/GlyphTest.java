package io.th0rgal.oraxen.glyphs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlyphTest {

    @AfterEach
    void resetAnimatedGlyphCodepoints() {
        AnimatedGlyph.resetCodepointCounter();
    }

    @Test
    void glyphTagUsesMiniMessageColonSyntax() {
        Glyph glyph = createGlyph("example", List.of("\ue001"));

        assertEquals("<glyph:example>", glyph.getGlyphTag());
    }

    @Test
    void exposesFlattenedAndFormattedCharactersSeparately() {
        Glyph glyph = createGlyph("example", List.of("AB", "CD"));

        assertEquals("ABCD", glyph.getCharacters());
        assertEquals("AB\nCD", glyph.getFormattedUnicodes());
    }

    @Test
    void jsonUsesConfiguredUnicodeRows() {
        Glyph glyph = createGlyph("example", List.of("AB", "CD"));

        JsonObject json = glyph.toJson();
        JsonArray chars = json.getAsJsonArray("chars");

        assertEquals(2, chars.size());
        assertEquals("AB", chars.get(0).getAsString());
        assertEquals("CD", chars.get(1).getAsString());
    }

    @Test
    void animatedGlyphComponentFallsBackUntilPackProcessing() {
        AnimatedGlyph glyph = createAnimatedGlyph("loading");

        assertEquals(Component.text(glyph.getGlyphTag()), glyph.getGlyphComponent());

        glyph.setProcessed("minecraft:loading.png", 8, 8);
        assertEquals(glyph.getAnimationComponent(), glyph.getGlyphComponent());
    }

    private Glyph createGlyph(String name, List<String> unicodeRows) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        return new Glyph(name, section, unicodeRows, GlyphGrid.fromUnicodeRows(unicodeRows));
    }

    private AnimatedGlyph createAnimatedGlyph(String name) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection animationSection = mock(ConfigurationSection.class);
        when(section.getString("texture")).thenReturn("loading.png");
        when(section.getConfigurationSection("animation")).thenReturn(animationSection);
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(animationSection.getInt("frames", -1)).thenReturn(2);
        when(animationSection.getInt("fps", AnimatedGlyph.DEFAULT_FPS)).thenReturn(AnimatedGlyph.DEFAULT_FPS);
        when(animationSection.getBoolean("loop", true)).thenReturn(true);
        return new AnimatedGlyph(name, section);
    }
}
