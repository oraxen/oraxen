package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackListener;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanicFactory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackpackMechanicTest extends MechanicTestSupport {

    @Test
    void readsBackpackSettings() {
        BackpackMechanic mechanic = new BackpackMechanic(mechanicFactory(), mechanicSection("backpack",
                "rows", 3,
                "title", "Bag",
                "open_sound", "open",
                "close_sound", "close",
                "volume", 0.7,
                "pitch", 1.3));

        assertEquals(3, mechanic.getRows());
        assertEquals("Bag", mechanic.getTitle());
        assertTrue(mechanic.hasOpenSound());
        assertEquals("open", mechanic.getOpenSound());
        assertTrue(mechanic.hasCloseSound());
        assertEquals("close", mechanic.getCloseSound());
        assertEquals(0.7f, mechanic.getVolume());
        assertEquals(1.3f, mechanic.getPitch());
    }

    @Test
    void preventsBlockInteractionWhileOpeningBackpack() {
        BackpackMechanicFactory factory = mock(BackpackMechanicFactory.class);
        BackpackMechanic mechanic = mock(BackpackMechanic.class);
        BackpackListener listener = new BackpackListener(factory);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack backpack = mock(ItemStack.class);
        Block block = mock(Block.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getItem()).thenReturn(backpack);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(backpack);
        when(factory.getMechanic("test_backpack")).thenReturn(mechanic);

        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(backpack)).thenReturn("test_backpack");
            listener.onPlayerInteract(event);
        }

        verify(event).setUseInteractedBlock(Event.Result.DENY);
        verify(event).setUseItemInHand(Event.Result.ALLOW);
    }
}
