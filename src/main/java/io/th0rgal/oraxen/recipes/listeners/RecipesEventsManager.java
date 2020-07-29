package io.th0rgal.oraxen.recipes.listeners;

import io.th0rgal.oraxen.Oraxen;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.recipes.CustomRecipe;

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
    private Map<CustomRecipe, String> permissionsPerRecipe = new HashMap<>();
    private Set<CustomRecipe> whitelistedCraftRecipes = new HashSet<>();

    public static RecipesEventsManager get() {
        if (instance == null) {
            instance = new RecipesEventsManager();
        }
        return instance;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(instance, Oraxen.get());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onCrafted(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        Player player = (Player) event.getView().getPlayer();
        if (hasPermissions(player, CustomRecipe.fromRecipe(recipe)))
            return;
        ItemStack result = event.getInventory().getResult();
        if (result == null)
            return;
        boolean containsOraxenItem = OraxenItems.isAnItem(OraxenItems.getIdByItem(result));
        if (!containsOraxenItem)
            for (ItemStack ingredient : event.getInventory().getMatrix())
                if (OraxenItems.isAnItem(OraxenItems.getIdByItem(ingredient))) {
                    containsOraxenItem = true;
                    break;
                }
        if (!containsOraxenItem)
            return;

        for (CustomRecipe whitelistedRecipe : whitelistedCraftRecipes) {
            if (whitelistedRecipe.equals(
                    new CustomRecipe(
                            Objects.requireNonNull(recipe).getResult(),
                            Arrays.asList(event.getInventory().getMatrix())
                    )
            ))
                return;
        }

        event.getInventory().setResult(new ItemStack(Material.AIR));
    }

    public void resetRecipes() {
        permissionsPerRecipe = new HashMap<>();
        whitelistedCraftRecipes = new HashSet<>();
    }

    public void addPermissionRecipe(CustomRecipe recipe, String permission) {
        permissionsPerRecipe.put(recipe, permission);
    }

    public void whitelistRecipe(CustomRecipe recipe) {
        whitelistedCraftRecipes.add(recipe);
    }

    private boolean hasPermissions(Player player, CustomRecipe recipe) {
        return (permissionsPerRecipe.containsKey(recipe) && player.hasPermission(permissionsPerRecipe.get(recipe)));
    }

}