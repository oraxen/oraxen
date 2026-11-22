package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.RecipeCrafting;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CountedCraftingTest {

    @Test
    void shiftCraftStopsWhenTheRemainingStackIsTooSmall() {
        CraftItemEvent event = mock(CraftItemEvent.class);
        CraftingInventory grid = mock(CraftingInventory.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack five = paper(5), three = paper(3), one = paper(1);
        ItemStack[] matrix = {five, null, null, null};
        when(five.asQuantity(3)).thenReturn(three);
        when(three.asQuantity(1)).thenReturn(one);
        when(event.getInventory()).thenReturn(grid);
        when(grid.getMatrix()).thenReturn(matrix);
        when(event.getWhoClicked()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(event.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[36]);

        ItemStack result = mock(ItemStack.class);
        when(result.getAmount()).thenReturn(1);
        when(result.getMaxStackSize()).thenReturn(64);
        when(result.getType()).thenReturn(Material.ENCHANTED_BOOK);
        when(result.clone()).thenReturn(result);
        RecipeCrafting.Match match = new RecipeCrafting.Match(null, new int[]{2, 0, 0, 0});
        CountedCrafting.take(event, stacks -> stacks[0].getAmount() >= 2 ? match : null,
                match, true, (matched, stacks) -> result);

        verify(event).setCancelled(true);
        verify(inventory, times(2)).addItem(result);
        verify(player, times(2)).incrementStatistic(Statistic.CRAFT_ITEM, Material.ENCHANTED_BOOK, 1);
        verify(grid).setMatrix(matrix);
        assertSame(one, matrix[0]);
    }

    @Test
    void shiftCraftDoesNotSpendIngredientsWhenTheOutputDoesNotFit() {
        CraftItemEvent event = mock(CraftItemEvent.class);
        CraftingInventory grid = mock(CraftingInventory.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack paper = paper(2);
        ItemStack[] matrix = {paper, null, null, null};
        when(event.getInventory()).thenReturn(grid);
        when(grid.getMatrix()).thenReturn(matrix);
        when(event.getWhoClicked()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(event.getAction()).thenReturn(InventoryAction.MOVE_TO_OTHER_INVENTORY);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{paper});
        ItemStack result = mock(ItemStack.class);
        when(result.getAmount()).thenReturn(1);

        RecipeCrafting.Match match = new RecipeCrafting.Match(null, new int[]{2, 0, 0, 0});
        CountedCrafting.take(event, stacks -> match, match, true, (matched, stacks) -> result);

        verify(event).setCancelled(true);
        verify(inventory, never()).addItem(any(ItemStack[].class));
        verify(grid, never()).setMatrix(any());
        assertSame(paper, matrix[0]);
    }

    private ItemStack paper(int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getAmount()).thenReturn(amount);
        when(item.getType()).thenReturn(Material.PAPER);
        return item;
    }
}
