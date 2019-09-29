package io.th0rgal.oraxen.recipes;

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
    private void onItemDamaged(InventoryClickEvent event) {

        ShapedBuilder shapedBuilder = ShapedBuilder.getShapedBuilder(event.getWhoClicked().getUniqueId());
        if (shapedBuilder == null || !event.getView().getTitle().equals(shapedBuilder.getInventoryTitle()))
            return;

        if (event.getSlotType() == InventoryType.SlotType.RESULT) {
            event.setCancelled(true);
            ItemStack currentResult = event.getCurrentItem() != null ? event.getCurrentItem().clone() : new ItemStack(Material.AIR);
            ItemStack currentCursor = event.getCursor() != null ? event.getCursor().clone() : new ItemStack(Material.AIR);
            event.setCurrentItem(currentCursor);
            event.setCursor(currentResult);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onInventoryClosed(InventoryCloseEvent event) {
        ShapedBuilder shapedBuilder = ShapedBuilder.getShapedBuilder(event.getPlayer().getUniqueId());
        if (shapedBuilder == null || !event.getView().getTitle().equals(shapedBuilder.getInventoryTitle()))
            return;

        shapedBuilder.setInventory(event.getPlayer().getUniqueId(), event.getInventory());
    }
}
