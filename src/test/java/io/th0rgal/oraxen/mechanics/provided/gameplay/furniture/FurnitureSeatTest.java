package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.util.Vector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FurnitureSeatTest {

    @ParameterizedTest
    @CsvSource({
            "0, 1, 0",
            "90, 0, 1",
            "180, -1, 0",
            "270, 0, -1"
    })
    void rotatesXOffsetLikeFurnitureBarriers(float yaw, double expectedX, double expectedZ) {
        assertSeatMatchesBarrier(new FurnitureMechanic.FurnitureSeat(1, -1.3, 0), yaw, expectedX, expectedZ);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, -1",
            "90, 1, 0",
            "180, 0, 1",
            "270, -1, 0"
    })
    void rotatesZOffsetLikeFurnitureBarriers(float yaw, double expectedX, double expectedZ) {
        assertSeatMatchesBarrier(new FurnitureMechanic.FurnitureSeat(0, -1.3, 1), yaw, expectedX, expectedZ);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0, 0",
            "0, 90, 90, 0",
            "90, 0, 90, 90",
            "90, 90, 180, 90",
            "180, 0, 180, 180",
            "180, 90, 270, 180",
            "270, 0, 270, 270",
            "270, 90, 0, 270"
    })
    void seatYawRotatesWithFurniture(float furnitureYaw, float seatYaw, float expectedIfConfigured, float expectedIfNull) {
        FurnitureMechanic.FurnitureSeat configured = new FurnitureMechanic.FurnitureSeat(0, 0, 0, seatYaw);
        FurnitureMechanic.FurnitureSeat fallback = new FurnitureMechanic.FurnitureSeat(0, 0, 0, null);

        assertEquals(expectedIfConfigured, configured.rotatedYaw(furnitureYaw), 1.0e-9);
        assertEquals(expectedIfNull, fallback.rotatedYaw(furnitureYaw), 1.0e-9);
    }

    private static void assertSeatMatchesBarrier(FurnitureMechanic.FurnitureSeat seat, float yaw,
                                                 double expectedX, double expectedZ) {
        Vector rotated = seat.rotatedOffset(yaw);
        BlockLocation barrier = new BlockLocation((int) seat.offsetX(), 0, (int) seat.offsetZ()).groundRotate(yaw);

        assertEquals(expectedX, rotated.getX(), 1.0e-9);
        assertEquals(-1.3, rotated.getY(), 1.0e-9);
        assertEquals(expectedZ, rotated.getZ(), 1.0e-9);
        assertEquals(barrier.getX(), rotated.getX(), 1.0e-9);
        assertEquals(barrier.getZ(), rotated.getZ(), 1.0e-9);
    }
}
