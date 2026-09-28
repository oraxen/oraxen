package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.music_disc.MusicDiscMechanicFactory;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicConfigDefaultsTest extends MechanicTestSupport {

    @Test
    void emptySectionMatchesAnnotationDefaults() {
        MiscMechanic misc = new MiscMechanic(mechanicFactory(), mechanicSection("misc"));
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_DISABLE_VANILLA_INTERACTIONS), misc.isVanillaInteractionDisabled());
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_CAN_STRIP_LOGS), misc.canStripLogs());
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_PIGLINS_IGNORE_WHEN_EQUIPPED), misc.piglinIgnoreWhenEquipped());
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_COMPOSTABLE), misc.isCompostable());
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_PREVENT_RENAMING), misc.preventsRenaming());
        assertEquals(annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_ALLOW_IN_VANILLA_RECIPES), misc.isAllowedInVanillaRecipes());

        BackpackMechanic backpack = new BackpackMechanic(mechanicFactory(), mechanicSection("backpack"));
        assertEquals(annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_ROWS), backpack.getRows());
        assertEquals(annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_TITLE), backpack.getTitle());
        assertEquals(annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_OPEN_SOUND), backpack.getOpenSound());
        assertEquals(annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_CLOSE_SOUND), backpack.getCloseSound());
        assertEquals(((Number) annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_VOLUME)).floatValue(), backpack.getVolume());
        assertEquals(((Number) annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_PITCH)).floatValue(), backpack.getPitch());

        MusicDiscMechanic disc = new MusicDiscMechanic(mechanicFactory(), mechanicSection("music_disc"));
        assertEquals(annotationDefault(MusicDiscMechanicFactory.class, MusicDiscMechanicFactory.PROP_SONG), disc.getSong());
        assertTrue(disc.hasNoSong());
    }

    @Test
    void configuredKeysOverrideAnnotationDefaults() {
        ConfigurationSection miscSection = mechanicSection("misc",
                "disable_vanilla_interactions", true,
                "can_strip_logs", true,
                "piglins_ignore_when_equipped", true,
                "compostable", true,
                "prevent_renaming", true,
                "allow_in_vanilla_recipes", true);
        MiscMechanic misc = new MiscMechanic(mechanicFactory(), miscSection);
        assertTrue(misc.isVanillaInteractionDisabled());
        assertTrue(misc.canStripLogs());
        assertTrue(misc.piglinIgnoreWhenEquipped());
        assertTrue(misc.isCompostable());
        assertTrue(misc.preventsRenaming());
        assertTrue(misc.isAllowedInVanillaRecipes());

        BackpackMechanic backpack = new BackpackMechanic(mechanicFactory(), mechanicSection("backpack",
                "rows", 3,
                "title", "Pouch",
                "open_sound", "open",
                "close_sound", "close",
                "volume", 0.4,
                "pitch", 1.5));
        assertEquals(3, backpack.getRows());
        assertEquals("Pouch", backpack.getTitle());
        assertEquals("open", backpack.getOpenSound());
        assertEquals("close", backpack.getCloseSound());
        assertEquals(0.4f, backpack.getVolume());
        assertEquals(1.5f, backpack.getPitch());

        MusicDiscMechanic disc = new MusicDiscMechanic(mechanicFactory(), mechanicSection("music_disc", "song", "oraxen:custom"));
        assertEquals("oraxen:custom", disc.getSong());
        assertFalse(disc.hasNoSong());
    }

    @Test
    void omittedStringDefaultStaysUnset() {
        assertNull(annotationDefault(MusicDiscMechanicFactory.class, MusicDiscMechanicFactory.PROP_SONG));
        assertEquals(6, annotationDefault(BackpackMechanicFactory.class, BackpackMechanicFactory.PROP_ROWS));
        assertEquals(false, annotationDefault(MiscMechanicFactory.class, MiscMechanicFactory.PROP_COMPOSTABLE));
    }

    private static Object annotationDefault(Class<?> factoryClass, String name) {
        ConfigProperty property = ConfigPropertyValues.property(factoryClass, name);
        return switch (property.type()) {
            case BOOLEAN -> property.defaultValue().isEmpty() ? false : Boolean.parseBoolean(property.defaultValue());
            case INTEGER -> Integer.parseInt(property.defaultValue());
            case DOUBLE -> Double.parseDouble(property.defaultValue());
            case STRING -> property.defaultValue().isEmpty() ? null : property.defaultValue();
            case LIST, OBJECT, ENUM -> throw new AssertionError(name + " is not a scalar");
        };
    }
}
