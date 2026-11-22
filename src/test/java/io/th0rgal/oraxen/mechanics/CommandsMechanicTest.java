package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.commands.CommandsMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.commands.CommandsMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.commands.CommandsMechanicListener;
import io.th0rgal.oraxen.utils.commands.CommandsParser;
import io.th0rgal.oraxen.utils.timers.Timer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void cancelledClickDoesNotRunCommandsOrConsumeTheItem() {
        CommandsMechanicFactory factory = mock(CommandsMechanicFactory.class);
        CommandsMechanic mechanic = mock(CommandsMechanic.class);
        CommandsParser commands = mock(CommandsParser.class);
        Timer timer = mock(Timer.class);
        Player player = mock(Player.class);
        ItemStack item = mock(ItemStack.class);
        when(factory.getMechanic("test_item")).thenReturn(mechanic);
        when(mechanic.hasPermission(player)).thenReturn(true);
        when(mechanic.getTimer(player)).thenReturn(timer);
        when(mechanic.getCommands()).thenReturn(commands);
        when(mechanic.isOneUsage()).thenReturn(true);
        when(timer.isFinished()).thenReturn(true);
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, item, mock(Block.class),
                BlockFace.UP, EquipmentSlot.HAND);
        event.setCancelled(true);

        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(item)).thenReturn("test_item");
            new CommandsMechanicListener(factory).onInteract(event);
        }

        verify(commands, never()).perform(player);
        verify(timer, never()).reset();
        verify(item, never()).setAmount(anyInt());
    }
}
