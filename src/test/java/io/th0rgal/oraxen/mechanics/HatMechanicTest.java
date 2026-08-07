package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.hat.HatMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HatMechanicTest extends MechanicTestSupport {

    @Test
    void storesBaseMechanicData() {
        HatMechanic mechanic = new HatMechanic(mechanicFactory(), mechanicSection("hat"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
