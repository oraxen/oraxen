package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FurnitureShulkerHitboxTest {

    @Test
    void defaultsToClosedUpwardShulker() {
        assertEquals(new FurnitureShulkerHitbox(0, 0.5, -1, 1, 0, BlockFace.UP),
                FurnitureShulkerHitbox.parse("0,0.5,-1 1"));
    }

    @Test
    void parsesPeekAndDirection() {
        assertEquals(new FurnitureShulkerHitbox(1, 0, 0, 0.5, 30, BlockFace.NORTH),
                FurnitureShulkerHitbox.parse(" 1,0,0  0.5 30 north "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0,0,0", "0,0 1", "0,0,0 0.05", "0,0,0 3.5", "0,0,0 1 101", "0,0,0 1 -1",
            "0,0,0 1 0 north_east", "0,0,0 1 0 up extra", "0,0,NaN 1", "0,0,0 1 0.5"})
    void rejectsInvalidEntries(String entry) {
        assertNull(FurnitureShulkerHitbox.parse(entry));
    }

    @ParameterizedTest
    @CsvSource({
            "UP, 0, DOWN",
            "DOWN, 90, UP",
            "NORTH, 0, SOUTH",
            "NORTH, 90, EAST",
            "NORTH, 180, NORTH",
            "NORTH, 270, WEST",
            "EAST, 90, SOUTH"
    })
    void rotatesOpeningDirectionWithFurniture(BlockFace direction, float yaw, BlockFace attachedFace) {
        assertEquals(attachedFace, FurnitureShulkerHitbox.attachedFace(direction, yaw));
    }
}
