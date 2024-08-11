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

import java.util.Optional;

public class RecipesBuilderEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    @SuppressWarnings("deprecation") // because we must use setCursor
    public void setCursor(InventoryClickEvent event) {
        String recipeBuilderTitle = Optional.ofNullable(RecipeBuilder.get(event.getWhoClicked().getUniqueId())).map(RecipeBuilder::getInventoryTitle).orElse(null);
        if (!InventoryUtils.getTitleFromView(event).equals(recipeBuilderTitle) || event.getSlotType() != InventoryType.SlotType.RESULT) return;

        event.setCancelled(true);
        ItemStack currentResult =  Optional.ofNullable(event.getCurrentItem()).orElse(new ItemStack(Material.AIR)).clone();
        ItemStack currentCursor = Optional.ofNullable(event.getCursor()).orElse(new ItemStack(Material.AIR)).clone();
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
