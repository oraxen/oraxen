package io.th0rgal.oraxen.font;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlyphAppearanceTest {

    @Test
    void parsesShadowColorFromConfig() {
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(section.getString("font", "minecraft:default")).thenReturn("minecraft:default");
        when(section.getString("shadow_color", null)).thenReturn("#112233");

        GlyphAppearance appearance = GlyphAppearance.fromConfig(section);

        assertEquals(Key.key("minecraft", "default"), appearance.font());
        assertEquals(0xFF112233, appearance.shadowColor());
    }

    @Test
    void appliesShadowColorOnSupportedVersions() {
        Component component = GlyphAppearance.applyShadowColor(Component.text("glyph"), 0x80112233);

        assertEquals(0x80112233, component.shadowColor().value());
    }
}
