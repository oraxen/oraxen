package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackMechanicTest extends MechanicTestSupport {

    @Test
    void readsBackpackSettings() {
        BackpackMechanic mechanic = new BackpackMechanic(mechanicFactory(), mechanicSection("backpack",
                "rows", 3,
                "title", "Bag",
                "open_sound", "open",
                "close_sound", "close",
                "volume", 0.7,
                "pitch", 1.3));

        assertEquals(3, mechanic.getRows());
        assertEquals("Bag", mechanic.getTitle());
        assertTrue(mechanic.hasOpenSound());
        assertEquals("open", mechanic.getOpenSound());
        assertTrue(mechanic.hasCloseSound());
        assertEquals("close", mechanic.getCloseSound());
        assertEquals(0.7f, mechanic.getVolume());
        assertEquals(1.3f, mechanic.getPitch());
    }
}
