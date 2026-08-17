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

    // computeIfAbsent is reached from each player's own region thread on Folia,
    // and regen() clears the map during reloads.
    private final Map<UUID, PaginatedGui> itemsViews = new ConcurrentHashMap<>();

    public InvManager() {
        regen();
    }

    public void regen() {
        itemsViews.clear();
    }

    public PaginatedGui getItemsView(Player player) {
        return itemsViews.computeIfAbsent(player.getUniqueId(), uuid -> new ItemsView().create());
    }


    public Gui getRecipesShowcase(final int page, final List<CustomRecipe> filteredRecipes) {
        return new RecipesView().create(page, filteredRecipes);
    }
}
