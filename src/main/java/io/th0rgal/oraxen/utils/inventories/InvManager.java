package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InvManager implements Listener {

    // Replace the cache on reload so an in-flight compute cannot republish a stale view.
    private volatile Map<UUID, PaginatedGui> itemsViews = new ConcurrentHashMap<>();

    public InvManager() {
        regen();
    }

    public void regen() {
        itemsViews = new ConcurrentHashMap<>();
        ItemsView.resetWarnings();
    }

    public PaginatedGui getItemsView(Player player) {
        Map<UUID, PaginatedGui> currentViews = itemsViews;
        return currentViews.computeIfAbsent(player.getUniqueId(), uuid -> new ItemsView().create());
    }

    // Each player keeps their own view because a paginated GUI shares its current page between viewers.
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        itemsViews.remove(event.getPlayer().getUniqueId());
    }


    public Gui getRecipesShowcase(final int page, final List<CustomRecipe> filteredRecipes) {
        return new RecipesView().create(page, filteredRecipes);
    }
}
