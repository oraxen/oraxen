package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvManager {

    private Map<UUID, ChestGui> itemsViews = new HashMap<>();
    private  Map<UUID, ChestGui> recipesViews = new HashMap<>();

    public InvManager() {
        regen();
    }

    public void regen() {
        itemsViews.clear();
        recipesViews.clear();
    }

    public ChestGui getItemsView(Player player) {
        return itemsViews.computeIfAbsent(player.getUniqueId(), uuid -> new ItemsView().create());
    }


    public ChestGui getRecipesShowcase(Player player, final int page, final List<CustomRecipe> filteredRecipes) {
        return recipesViews.computeIfAbsent(player.getUniqueId(), uuid -> new RecipesView().create(page, filteredRecipes));
    }
}
