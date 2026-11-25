package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FurnitureOffsetParsingTest {

    @Test
    void parsesFiniteCoordinatesForEachFurnitureType() {
        assertEquals(new FurnitureGhastHitbox(-1, 0.5, 200, 1), FurnitureGhastHitbox.parse(" -1,0.5,2e2 1 "));
        assertEquals(new FurnitureShulkerHitbox(-1, 0.5, 200, 1, 0, BlockFace.UP),
                FurnitureShulkerHitbox.parse(" -1,0.5,2e2 1 "));
        assertEquals(new FurnitureBed(-1, 0.5, 200, true, true, true, false), FurnitureBed.parse(" -1,0.5,2e2 "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0,0", "0,0,0,", "0,,0", "0,0,NaN", "Infinity,0,0", "0,-Infinity,0", "0,0,1e999"})
    void rejectsInvalidCoordinatesForEachFurnitureType(String coordinates) {
        assertNull(FurnitureGhastHitbox.parse(coordinates + " 1"));
        assertNull(FurnitureShulkerHitbox.parse(coordinates + " 1"));
        assertNull(FurnitureBed.parse(coordinates));
    }
}
