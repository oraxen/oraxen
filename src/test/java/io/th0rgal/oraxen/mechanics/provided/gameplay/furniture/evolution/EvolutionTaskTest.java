package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvolutionTaskTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "63 |        |        | true",  // open sky above the ground block
            "64 | 0      |        | true",  // the furniture's own barrier is the highest block
            "70 | 0      | 70     | false", // a roof covers the furniture
            "65 | 0,1    |        | true",  // the furniture's own upper barrier is the highest block
            "70 | 0,1    | 70     | false", // a roof covers the furniture and its upper barrier
            "67 | 0,3    | 66     | false"  // a roof sits between the furniture and its upper barrier
    })
    void rainBoostsOnlyFurnitureOpenToTheSky(int highestBlockY, String barrierHeights, Integer roofY, boolean exposed) {
        World world = mock(World.class);
        Location location = new Location(world, 0.5, 64.5, 0.5);
        List<Integer> ownHeights = barrierHeights == null ? List.of()
                : Arrays.stream(barrierHeights.split(",")).map(Integer::parseInt).toList();
        when(world.hasStorm()).thenReturn(true);
        when(world.getHighestBlockYAt(location)).thenReturn(highestBlockY);
        when(world.getBlockAt(eq(0), anyInt(), eq(0))).thenAnswer(invocation -> {
            int y = invocation.getArgument(1);
            if (roofY != null && y == roofY) return block(Material.STONE, false);
            if (ownHeights.contains(y - 64)) return block(Material.BARRIER, false);
            return block(Material.AIR, true);
        });
        List<BlockLocation> barriers = ownHeights.stream().map(y -> new BlockLocation(0, y, 0)).toList();

        assertEquals(exposed, EvolutionTask.isExposedToRain(world, location, barriers));
    }

    private static Block block(Material type, boolean passable) {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(type);
        when(block.isPassable()).thenReturn(passable);
        return block;
    }
}
