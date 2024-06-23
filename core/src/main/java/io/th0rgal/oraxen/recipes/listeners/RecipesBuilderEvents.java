package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import io.th0rgal.oraxen.utils.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class RecipesBuilderEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("deprecation") // because we must use setCursor
    public void setCursor(InventoryClickEvent event) {

        RecipeBuilder recipeBuilder = RecipeBuilder.get(event.getWhoClicked().getUniqueId());
        if (recipeBuilder == null || !InventoryUtils.getTitleFromView(event).equals(recipeBuilder.getInventoryTitle())
            || event.getSlotType() != InventoryType.SlotType.RESULT)
            return;
        event.setCancelled(true);
        ItemStack currentResult = event.getCurrentItem() != null ? event.getCurrentItem().clone()
            : new ItemStack(Material.AIR);
        ItemStack currentCursor = event.getCursor() != null ? event.getCursor().clone() : new ItemStack(Material.AIR);
        event.setCurrentItem(currentCursor);
        event.setCursor(currentResult);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClosed(InventoryCloseEvent event) {
        RecipeBuilder recipeBuilder = RecipeBuilder.get(event.getPlayer().getUniqueId());
        if (recipeBuilder == null || !InventoryUtils.getTitleFromView(event).equals(recipeBuilder.getInventoryTitle()))
            return;

        recipeBuilder.setInventory(event.getInventory());
    }
}
