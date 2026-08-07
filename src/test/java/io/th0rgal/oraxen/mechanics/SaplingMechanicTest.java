package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingMechanic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaplingMechanicTest extends MechanicTestSupport {

    @Test
    void readsGrowthSettings() {
        SaplingMechanic mechanic = new SaplingMechanic("test_item", standaloneSection(
                "canGrowNaturally", false,
                "naturalGrowthTime", 100,
                "canGrowFromBoneMeal", true,
                "boneMealGrowthSpeedup", 25,
                "growSound", "entity.experience_orb.pickup",
                "minLightLevel", 9,
                "requiresWaterSource", true,
                "schematicName", "tree",
                "replaceBlocks", true,
                "shouldCopyBiomes", true,
                "shouldCopyEntities", false));

        assertFalse(mechanic.canGrowNaturally());
        assertEquals(100, mechanic.getNaturalGrowthTime());
        assertTrue(mechanic.canGrowFromBoneMeal());
        assertEquals(25, mechanic.getBoneMealGrowthSpeedup());
        assertTrue(mechanic.hasGrowSound());
        assertEquals("entity.experience_orb.pickup", mechanic.getGrowSound());
        assertTrue(mechanic.requiresLight());
        assertEquals(9, mechanic.getMinLightLevel());
        assertTrue(mechanic.requiresWaterSource());
        assertEquals("tree.schem", mechanic.getSchematicName());
        assertTrue(mechanic.replaceBlocks());
        assertTrue(mechanic.copyBiomes());
        assertFalse(mechanic.copyEntities());
    }
}
