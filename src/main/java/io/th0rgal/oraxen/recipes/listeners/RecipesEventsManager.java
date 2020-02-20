package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class RecipesEventsManager implements Listener {

    private static RecipesEventsManager instance;
    private Map<Recipe, String> permissionsPerRecipe = new HashMap<>();
    private Set<Recipe> whitelistedCraftRecipes = new HashSet<>();

    public static RecipesEventsManager get() {
        if (instance == null) {
            instance = new RecipesEventsManager();
        }
        return instance;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(instance, OraxenPlugin.get());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onCrafted(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        Player player = (Player) event.getView().getPlayer();
        if (hasPermissions(player, recipe))
            return;
        ItemStack result = event.getInventory().getResult();
        boolean containsOraxenItem = result != null && OraxenItems.isAnItem(OraxenItems.getIdByItem(result));
        if (!containsOraxenItem)
            for (ItemStack ingredient : event.getInventory().getMatrix())
                if (OraxenItems.isAnItem(OraxenItems.getIdByItem(ingredient))) {
                    containsOraxenItem = true;
                    break;
                }
        if (!containsOraxenItem)
            return;

        for (Recipe whitelistedRecipe : whitelistedCraftRecipes)
            if (whitelistedRecipe.equals(recipe))
                return;

        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    public void addPermissionRecipe(Recipe recipe, String permission) {
        permissionsPerRecipe.put(recipe, permission);
    }

    public void addRecipe(Recipe recipe) {
        whitelistedCraftRecipes.add(recipe);
    }

    private boolean hasPermissions(Player player, Recipe recipe) {
        return (permissionsPerRecipe.containsKey(recipe) && player.hasPermission(permissionsPerRecipe.get(recipe)));
    }

}