package io.th0rgal.oraxen.glyphs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import io.th0rgal.oraxen.utils.logs.Logs;

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
    void configuredFontControlsGlyphProviderAndRenderingFont() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("font")).thenReturn("mizius:icons");
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        Glyph glyph = new Glyph("example", section, List.of("\ue001"), GlyphGrid.SINGLE);

        assertEquals(Key.key("mizius", "icons"), glyph.getTargetFont());
        assertEquals(Key.key("mizius", "icons"), glyph.getFont());
        assertEquals(Key.key("mizius", "icons"), glyph.getGlyphComponent().style().font());
    }

    @Test
    void missingTargetFontPreservesAppearanceFontAndDefaultProviderFont() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection appearance = mock(ConfigurationSection.class);
        when(section.getConfigurationSection("appearance")).thenReturn(appearance);
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(appearance.getString("font", "minecraft:default")).thenReturn("legacy:font");
        Glyph glyph = new Glyph("example", section, List.of("\ue001"), GlyphGrid.SINGLE);

        assertEquals(Key.key("minecraft", "default"), glyph.getTargetFont());
        assertEquals(Key.key("legacy", "font"), glyph.getFont());
    }

    @Test
    void animatedGlyphComponentFallsBackUntilPackProcessing() {
        AnimatedGlyph glyph = createAnimatedGlyph("loading");

        assertEquals(Key.key("oraxen", "animations/loading"), glyph.getAnimationFont());
        assertEquals(Component.text(glyph.getGlyphTag()), glyph.getGlyphComponent());

        glyph.setProcessed("minecraft:loading.png", 8, 8);
        assertEquals(glyph.getAnimationComponent(), glyph.getGlyphComponent());
    }

    @Test
    void configuredAnimatedGlyphFontIsIgnored() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection animation = mock(ConfigurationSection.class);
        when(section.getString("texture")).thenReturn("loading.png");
        when(section.getString("font")).thenReturn("mizius:icons");
        when(section.getConfigurationSection("animation")).thenReturn(animation);
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(animation.getInt("frames", -1)).thenReturn(2);
        when(animation.getInt("fps", AnimatedGlyph.DEFAULT_FPS)).thenReturn(AnimatedGlyph.DEFAULT_FPS);
        when(animation.getBoolean("loop", true)).thenReturn(true);

        AnimatedGlyph glyph = new AnimatedGlyph("loading", section);
        glyph.setProcessed("minecraft:loading.png", 8, 8);

        assertEquals(Key.key("oraxen", "animations/loading"), glyph.getAnimationFont());
        assertEquals(Key.key("oraxen", "animations/loading"), glyph.getAnimationComponent().children().getFirst().style().font());
    }

    @Test
    void reservedShiftTargetWarnsAndPreservesDefaultFonts() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection animation = mock(ConfigurationSection.class);
        when(section.getString("font")).thenReturn("oraxen:shift");
        when(section.getString("texture")).thenReturn("loading.png");
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("loading.png");
        when(section.getConfigurationSection("animation")).thenReturn(animation);
        when(animation.getInt("frames", -1)).thenReturn(2);
        try (var logs = mockStatic(Logs.class)) {
            Glyph glyph = new Glyph("static", section, List.of("A"), GlyphGrid.SINGLE);
            AnimatedGlyph animated = new AnimatedGlyph("loading", section);
            assertEquals(Key.key("minecraft", "default"), glyph.getTargetFont());
            assertEquals(Key.key("minecraft", "default"), glyph.getFont());
            assertEquals(Key.key("oraxen", "animations/loading"), animated.getAnimationFont());
            logs.verify(() -> Logs.logWarning("Glyph 'static' targets reserved font 'oraxen:shift'. Using the default glyph font instead."));
        }
    }

    private Glyph createGlyph(String name, List<String> unicodeRows) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("texture", "required/exit_icon.png")).thenReturn("example.png");
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        return new Glyph(name, section, unicodeRows, GlyphGrid.fromUnicodeRows(unicodeRows));
    }

    private AnimatedGlyph createAnimatedGlyph(String name) {
        return createAnimatedGlyph(name, null);
    }

    private AnimatedGlyph createAnimatedGlyph(String name, String font) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection animationSection = mock(ConfigurationSection.class);
        when(section.getString("texture")).thenReturn("loading.png");
        when(section.getString("font")).thenReturn(font);
        when(section.getConfigurationSection("animation")).thenReturn(animationSection);
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(animationSection.getInt("frames", -1)).thenReturn(2);
        when(animationSection.getInt("fps", AnimatedGlyph.DEFAULT_FPS)).thenReturn(AnimatedGlyph.DEFAULT_FPS);
        when(animationSection.getBoolean("loop", true)).thenReturn(true);
        return new AnimatedGlyph(name, section);
    }

}
