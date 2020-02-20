package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RecipesEventsManager implements Listener {

    private static RecipesEventsManager instance;
    private Map<Recipe, String> permissionsPerRecipe = new HashMap<>();
    private Set<Recipe> whitelistedCraftRecipes = new HashSet<>();

    public static RecipesEventsManager get() {
        if (instance == null) {
            instance = new RecipesEventsManager();
            Bukkit.getPluginManager().registerEvents(instance, OraxenPlugin.get());
        }
        return instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onCrafted(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        Player player = (Player) event.getView().getPlayer();
        if (hasPermissions(player, recipe))
            return;
        boolean containsOraxenItem = false;
        for (ItemStack ingredient : event.getInventory().getMatrix())
            if (OraxenItems.isAnItem(OraxenItems.getIdByItem(event.getInventory().getResult()))) {
                containsOraxenItem = true;
                break;
            }
        if (!containsOraxenItem)
            return;
        if (containsOraxenItem && whitelistedCraftRecipes.contains(event.getRecipe()))
            return;
        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    public void addRecipe(Recipe recipe, String permission) {
        permissionsPerRecipe.put(recipe, permission);
    }

    private boolean hasPermissions(Player player, Recipe recipe) {
        return (!permissionsPerRecipe.containsKey(recipe) || player.hasPermission(permissionsPerRecipe.get(recipe)));
    }

}