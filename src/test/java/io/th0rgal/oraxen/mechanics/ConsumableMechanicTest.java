package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.consumable.ConsumableMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.consumable.ConsumableMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.misc.consumable.ConsumableMechanicListener;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumableMechanicTest extends MechanicTestSupport {

    private final Player player = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final ItemStack item = mock(ItemStack.class);

    @Test
    void storesBaseMechanicData() {
        ConsumableMechanic mechanic = new ConsumableMechanic(mechanicFactory(), mechanicSection("consumable"));

        assertEquals("test_item", mechanic.getItemID());
    }

    @Test
    void cancelledClickDoesNotConsumeTheItem() {
        when(item.getAmount()).thenReturn(3);
        PlayerInteractEvent event = clickEvent(EquipmentSlot.HAND);
        event.setCancelled(true);

        use(event);

        verify(item, never()).setAmount(anyInt());
        verify(inventory, never()).setItem(anyInt(), any());
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    @Test
    void consumingTheLastItemClearsTheOffHand() {
        when(item.getAmount()).thenReturn(1);

        use(clickEvent(EquipmentSlot.OFF_HAND));

        verify(inventory).setItem(EquipmentSlot.OFF_HAND, null);
    }

    private PlayerInteractEvent clickEvent(EquipmentSlot hand) {
        when(player.getInventory()).thenReturn(inventory);
        return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, mock(Block.class), BlockFace.UP, hand);
    }

    private void use(PlayerInteractEvent event) {
        try (MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            items.when(() -> OraxenItems.getIdByItem(item)).thenReturn("test_item");
            new ConsumableMechanicListener(mock(ConsumableMechanicFactory.class)).onItemUsed(event);
        }
    }
}
