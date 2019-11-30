package io.th0rgal.oraxen.recipes.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.Recipe;

import java.util.Map;

public class PermissionRecipesManager implements Listener {

    private static Map<Recipe, String> permissionsPerRecipe;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onCrafted(PrepareItemCraftEvent event) {
        event.getRecipe();

    }

}