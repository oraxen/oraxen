package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LimitedPlacingTest extends MechanicTestSupport {

    @Test
    void denyBlocksOnlyTheListedBlocks() {
        LimitedPlacing limitedPlacing = new LimitedPlacing(standaloneSection("type", "DENY", "block_types", List.of("STONE")));

        assertFalse(limitedPlacing.allowsPlacementOn(block(Material.STONE)));
        assertTrue(limitedPlacing.allowsPlacementOn(block(Material.DIRT)));
    }

    @Test
    void denyWithoutListsAllowsEveryBlock() {
        LimitedPlacing limitedPlacing = new LimitedPlacing(standaloneSection("type", "DENY"));

        assertTrue(limitedPlacing.allowsPlacementOn(block(Material.STONE)));
    }

    @Test
    void allowPermitsOnlyTheListedBlocks() {
        LimitedPlacing limitedPlacing = new LimitedPlacing(standaloneSection("type", "ALLOW", "block_types", List.of("STONE")));

        assertTrue(limitedPlacing.allowsPlacementOn(block(Material.STONE)));
        assertFalse(limitedPlacing.allowsPlacementOn(block(Material.DIRT)));
    }

    @Test
    void readsTypeIgnoringCase() {
        LimitedPlacing limitedPlacing = new LimitedPlacing(standaloneSection("type", "allow"));

        assertEquals(LimitedPlacing.LimitedPlacingType.ALLOW, limitedPlacing.getType());
    }

    @Test
    void invalidTypeWarnsAndFallsBackToDeny() {
        try (MockedStatic<Logs> logs = mockStatic(Logs.class)) {
            LimitedPlacing limitedPlacing = new LimitedPlacing(standaloneSection("type", "sideways"));

            assertEquals(LimitedPlacing.LimitedPlacingType.DENY, limitedPlacing.getType());
            logs.verify(() -> Logs.logWarning("Valid types are: ALLOW, DENY"));
        }
    }

    private static Block block(Material material) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        return block;
    }
}
