package io.th0rgal.oraxen.recipes.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.PrepareItemCraftEvent;

public class RecipesEvents implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onCrafted(PrepareItemCraftEvent event) {

    }

}