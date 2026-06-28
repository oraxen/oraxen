package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.cosmetic.skinnable.SkinnableMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkinnableMechanicTest extends MechanicTestSupport {

    @Test
    void storesBaseMechanicData() {
        SkinnableMechanic mechanic = new SkinnableMechanic(mechanicFactory(), mechanicSection("skinnable"));

        assertEquals("test_item", mechanic.getItemID());
    }
}
