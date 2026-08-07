package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.togglelight.ToggleLightMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleLightMechanicTest extends MechanicTestSupport {

    @Test
    void clampsConfiguredLightLevels() {
        ToggleLightMechanic mechanic = new ToggleLightMechanic(mechanicFactory(), mechanicSection("togglelight", "toggle_light", 20, "light", -2));

        assertEquals(15, mechanic.getToggleLightLevel());
        assertEquals(0, mechanic.getBaseLightLevel());
        assertTrue(mechanic.hasToggleLight());
    }
}
