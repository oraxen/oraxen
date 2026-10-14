package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.itemtype.ItemTypeMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTypeMechanicTest extends MechanicTestSupport {

    @Test
    void readsItemTypeValue() {
        ItemTypeMechanic mechanic = new ItemTypeMechanic(mechanicFactory(), mechanicSection("itemtype", "value", "sword"));

        assertEquals("sword", mechanic.itemType);
    }
}
