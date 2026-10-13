package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.consumable.ConsumableMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumableMechanicTest extends MechanicTestSupport {

    @Test
    void storesBaseMechanicData() {
        ConsumableMechanic mechanic = new ConsumableMechanic(mechanicFactory(), mechanicSection("consumable"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
