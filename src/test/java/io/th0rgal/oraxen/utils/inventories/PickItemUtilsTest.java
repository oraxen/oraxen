package io.th0rgal.oraxen.utils.inventories;

import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PickItemUtilsTest {

    @Test
    void selectsMatchingHotbarItemWithoutMovingIt() {
        ItemStack pickedItem = item();
        ItemStack[] slots = new ItemStack[36];
        slots[2] = pickedItem;

        PlayerInventory inventory = inventory(slots, 7);
        Player player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);

        PickItemUtils.pickItem(player, pickedItem);

        verify(inventory).setHeldItemSlot(2);
        verify(inventory, never()).setItem(anyInt(), nullable(ItemStack.class));
    }

    @Test
    void movesMatchingStorageItemToSuitableHotbarSlot() {
        ItemStack pickedItem = item();
        ItemStack selectedItem = item();
        ItemStack[] slots = new ItemStack[36];
        slots[4] = selectedItem;
        slots[20] = pickedItem;

        PlayerInventory inventory = inventory(slots, 4);
        Player player = mock(Player.class);
        when(player.getInventory()).thenReturn(inventory);

        PickItemUtils.pickItem(player, pickedItem);

        verify(inventory).setItem(5, pickedItem);
        verify(inventory).setItem(20, null);
        verify(inventory).setHeldItemSlot(5);
    }

    private static ItemStack item() {
        Material material = mock(Material.class);
        when(material.isAir()).thenReturn(false);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.isSimilar(item)).thenReturn(true);
        when(item.getEnchantments()).thenReturn(java.util.Map.of());
        return item;
    }

    private static PlayerInventory inventory(ItemStack[] slots, int selectedSlot) {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getHeldItemSlot()).thenReturn(selectedSlot);
        when(inventory.getItem(anyInt())).thenAnswer(invocation -> slots[invocation.getArgument(0)]);
        doAnswer(invocation -> {
            slots[invocation.getArgument(0)] = invocation.getArgument(1);
            return null;
        }).when(inventory).setItem(anyInt(), nullable(ItemStack.class));
        return inventory;
    }
}
