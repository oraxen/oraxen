package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.BlockLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvolutionTaskTest {

    @ParameterizedTest
    @CsvSource({
            "63, false, true",  // open sky above the ground block
            "64, false, true",  // the furniture's own barrier is the highest block
            "70, false, false", // a roof covers the furniture
            "65, true, true",   // the furniture's own upper barrier is the highest block
            "70, true, false"   // a roof covers the furniture and its upper barrier
    })
    void rainBoostsOnlyFurnitureOpenToTheSky(int highestBlockY, boolean upperBarrier, boolean exposed) {
        World world = mock(World.class);
        Location location = new Location(world, 0.5, 64.5, 0.5);
        when(world.hasStorm()).thenReturn(true);
        when(world.getHighestBlockYAt(location)).thenReturn(highestBlockY);
        List<BlockLocation> barriers = upperBarrier
                ? List.of(new BlockLocation(0, 0, 0), new BlockLocation(0, 1, 0), new BlockLocation(1, 6, 0))
                : List.of(new BlockLocation(0, 0, 0));

        assertEquals(exposed, EvolutionTask.isExposedToRain(world, location, barriers));
    }
}
