package io.th0rgal.oraxen.fonts;

import io.th0rgal.oraxen.glyphs.AnimatedGlyph;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FontEventsSignTest {

    @AfterEach
    void resetAnimatedGlyphCodepoints() {
        AnimatedGlyph.resetCodepointCounter();
    }

    @Test
    void restoresPlaceholdersForCompleteAnimatedSequences() {
        AnimatedGlyph spinner = mock(AnimatedGlyph.class);
        when(spinner.getPlaceholders()).thenReturn(new String[] {":spinner:"});
        when(spinner.getGlyphComponent()).thenReturn(Component.empty()
                .append(Component.text("\ue100"))
                .append(Component.text("\ue101"))
                .append(Component.text("\ue102")));

        assertEquals("Before :spinner: after :spinner:",
                FontEvents.animatedGlyphPlaceholders(
                        "Before \ue100\ue101\ue102 after \ue100\ue101\ue102", List.of(spinner)));
        assertEquals("\ue100\ue101", FontEvents.animatedGlyphPlaceholders("\ue100\ue101", List.of(spinner)));
    }

    @Test
    void restoresPlaceholdersForAnimationsWithDifferentConfiguredFontValues() {
        AnimatedGlyph first = createAnimatedGlyph("first", "mizius:first", ":first:");
        AnimatedGlyph second = createAnimatedGlyph("second", "mizius:second", ":second:");
        PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();
        String firstSequence = plainText.serialize(first.getGlyphComponent());
        String secondSequence = plainText.serialize(second.getGlyphComponent());

        assertNotEquals(firstSequence, secondSequence);
        assertEquals(":first: :second:", FontEvents.animatedGlyphPlaceholders(
                firstSequence + " " + secondSequence, List.of(first, second)));
    }

    private AnimatedGlyph createAnimatedGlyph(String name, String font, String placeholder) {
        ConfigurationSection section = mock(ConfigurationSection.class);
        ConfigurationSection animation = mock(ConfigurationSection.class);
        ConfigurationSection chat = mock(ConfigurationSection.class);
        when(section.getString("texture")).thenReturn(name + ".png");
        when(section.getString("font")).thenReturn(font);
        when(section.getConfigurationSection("animation")).thenReturn(animation);
        when(section.getConfigurationSection("chat")).thenReturn(chat);
        when(section.getInt("ascent", 8)).thenReturn(8);
        when(section.getInt("height", 8)).thenReturn(8);
        when(animation.getInt("frames", -1)).thenReturn(2);
        when(animation.getInt("fps", AnimatedGlyph.DEFAULT_FPS)).thenReturn(AnimatedGlyph.DEFAULT_FPS);
        when(animation.getBoolean("loop", true)).thenReturn(true);
        when(chat.getStringList("placeholders")).thenReturn(List.of(placeholder));

        AnimatedGlyph glyph = new AnimatedGlyph(name, section);
        glyph.setProcessed("minecraft:" + name + ".png", 8, 8);
        return glyph;
    }
}
