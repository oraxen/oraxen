package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShapedBlockMechanicTest extends MechanicTestSupport {

    @Test
    void readsShapeVariationAndPlacedMaterial() {
        ShapedBlockMechanic mechanic = new ShapedBlockMechanic(mechanicFactory(), mechanicSection("shaped",
                "type", "SLAB",
                "custom_variation", 2,
                "model", "block/slab"));

        assertEquals(ShapedBlockType.SLAB, mechanic.getBlockType());
        assertEquals(2, mechanic.getCustomVariation());
        assertEquals(Material.WAXED_EXPOSED_CUT_COPPER_SLAB, mechanic.getPlacedMaterial());
        assertEquals("block/slab", mechanic.getModel(mechanic.getSection()));
    }
}
