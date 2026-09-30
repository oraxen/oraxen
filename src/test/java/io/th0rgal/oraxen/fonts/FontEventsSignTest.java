package io.th0rgal.oraxen.fonts;

import io.th0rgal.oraxen.glyphs.AnimatedGlyph;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FontEventsSignTest {

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
}
