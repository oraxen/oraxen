package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.light.LightMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LightMechanicTest extends MechanicTestSupport {

    @Test
    void absentLightDoesNotCreateLightData() {
        LightMechanic mechanic = new LightMechanic(standaloneSection());

        assertEquals(-1, mechanic.getLightLevel());
        assertFalse(mechanic.hasLightLevel());
    }
}
