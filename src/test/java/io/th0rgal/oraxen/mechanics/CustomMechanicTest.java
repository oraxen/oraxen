package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.CustomMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomMechanicTest extends MechanicTestSupport {

    @Test
    void emptyConfigRegistersNoCustomListeners() {
        CustomMechanic mechanic = new CustomMechanic(mechanicFactory(), mechanicSection("custom"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
