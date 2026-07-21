package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.misc.MiscMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MiscMechanicTest extends MechanicTestSupport {

    @Test
    void isMechanicType() {
        assertTrue(Mechanic.class.isAssignableFrom(MiscMechanic.class));
    }
}
