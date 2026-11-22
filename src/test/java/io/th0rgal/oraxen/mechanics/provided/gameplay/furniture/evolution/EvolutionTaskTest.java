package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvolutionTaskTest {

    @ParameterizedTest
    @CsvSource({
            "63, true",  // open sky above the ground block
            "64, true",  // the furniture's own barrier is the highest block
            "70, false"  // a roof covers the furniture
    })
    void rainBoostsOnlyFurnitureOpenToTheSky(int highestBlockY, boolean exposed) {
        World world = mock(World.class);
        Location location = new Location(world, 0.5, 64.5, 0.5);
        when(world.hasStorm()).thenReturn(true);
        when(world.getHighestBlockYAt(location)).thenReturn(highestBlockY);

        assertEquals(exposed, EvolutionTask.isExposedToRain(world, location));
    }
}
