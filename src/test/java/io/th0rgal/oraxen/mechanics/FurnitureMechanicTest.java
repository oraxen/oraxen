package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FurnitureMechanicTest extends MechanicTestSupport {

    @Test
    void readsBasicFurnitureSettings() {
        OraxenPlugin.supportsDisplayEntities = false;
        FurnitureMechanic mechanic = new FurnitureMechanic(mechanicFactory(), mechanicSection("furniture",
                "hardness", 4,
                "item", "placed_item",
                "type", "ITEM_FRAME",
                "seat", java.util.Map.of("height", 0.75, "yaw", 90.0),
                "rotatable", true));

        assertEquals(4, mechanic.getHardness());
        assertTrue(mechanic.hasHardness());
        assertEquals(FurnitureMechanic.FurnitureType.ITEM_FRAME, mechanic.getFurnitureType());
        assertTrue(mechanic.hasSeat());
        assertEquals(0.75f, mechanic.getSeatHeight());
        assertTrue(mechanic.hasHitbox());
        assertFalse(mechanic.hasLimitedPlacing());
        assertFalse(mechanic.hasBlockSounds());
    }
}
