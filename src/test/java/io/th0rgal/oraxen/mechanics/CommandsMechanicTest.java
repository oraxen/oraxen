package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.commands.CommandsMechanic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandsMechanicTest extends MechanicTestSupport {

    @Test
    void readsUsagePermissionAndCooldownSettings() {
        CommandsMechanic mechanic = new CommandsMechanic(mechanicFactory(), mechanicSection("commands",
                "one_usage", true,
                "permission", "oraxen.test",
                "cooldown", 20L));
        Player player = mock(Player.class);
        when(player.hasPermission("oraxen.test")).thenReturn(true);

        assertTrue(mechanic.isOneUsage());
        assertEquals("oraxen.test", mechanic.getPermission());
        assertTrue(mechanic.hasPermission(player));
        assertNotNull(mechanic.getCommands());
    }

    @Test
    void missingPermissionAllowsUsage() {
        CommandsMechanic mechanic = new CommandsMechanic(mechanicFactory(), mechanicSection("commands"));

        assertFalse(mechanic.isOneUsage());
        assertTrue(mechanic.hasPermission(mock(Player.class)));
    }
}
