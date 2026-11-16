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
        RecipesEventsManager recipes = RecipesEventsManager.get();
        var matrix = crafter.getInventory().getContents();
        if (!recipes.prepareCrafter(event.getRecipe(), matrix)) {
            event.setCancelled(true);
            return;
        }
        try {
            var result = recipes.craftingResult(event.getRecipe(), matrix, event.getResult());
            if (result == null) event.setCancelled(true);
            else event.setResult(result);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            event.setCancelled(true);
        }
    }
}
