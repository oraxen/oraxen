package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.watering.WateringMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WateringMechanicTest extends MechanicTestSupport {

    @Test
    void readsCanItems() {
        WateringMechanic mechanic = new WateringMechanic(mechanicFactory(), mechanicSection("watering", "emptyCanItem", "empty_can", "filledCanItem", "filled_can"));

        assertEquals("empty_can", mechanic.getEmptyCanItem());
        assertEquals("filled_can", mechanic.getFilledCanItem());
        assertFalse(mechanic.isEmpty());
        assertFalse(mechanic.isFilled());
    }
}
