package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.bottledexp.BottledExpMechanic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BottledExpMechanicTest extends MechanicTestSupport {

    @Test
    void convertsPlayerExperienceToBottleCount() {
        BottledExpMechanic mechanic = new BottledExpMechanic(mechanicFactory(), mechanicSection("bottledexp", "ratio", 1.0));

        assertTrue(mechanic.getBottleEquivalent(10, 0.5f) > 0);
    }

    @Test
    void countsPartialLevelProgressAsExperiencePoints() {
        BottledExpMechanic mechanic = new BottledExpMechanic(mechanicFactory(), mechanicSection("bottledexp", "ratio", 1.0));
        Player player = mock(Player.class);
        when(player.getLevel()).thenReturn(5);
        when(player.getExp()).thenReturn(0.5f);
        when(player.getExpToLevel()).thenReturn(17);

        // Level 5 holds 55 points and half of the 17 points towards level 6 adds 8.5, so 63.5 points.
        assertEquals(7, mechanic.getBottleEquivalent(player));
    }
}
