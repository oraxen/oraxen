package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvManager {

    // Replace the cache on reload so an in-flight compute cannot republish a stale view.
    private volatile Map<UUID, PaginatedGui> itemsViews = new ConcurrentHashMap<>();

    public InvManager() {
        regen();
    }

    public void regen() {
        itemsViews = new ConcurrentHashMap<>();
    }

    public PaginatedGui getItemsView(Player player) {
        Map<UUID, PaginatedGui> currentViews = itemsViews;
        return currentViews.computeIfAbsent(player.getUniqueId(), uuid -> new ItemsView().create());
    }


    public Gui getRecipesShowcase(final int page, final List<CustomRecipe> filteredRecipes) {
        return new RecipesView().create(page, filteredRecipes);
    }
}
