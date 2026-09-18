package io.th0rgal.oraxen.recipes.listeners;

import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;

public class CrafterRecipeEvents implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCraft(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter crafter)) return;
        if (RecipesEventsManager.containsRestrictedCraftingIngredient(crafter.getInventory().getContents()))
            event.setCancelled(true);
    }
}
