package io.th0rgal.oraxen.utils.inventories;

import dev.triumphteam.gui.guis.PaginatedGui;
import io.th0rgal.oraxen.recipes.CustomRecipe;

import java.util.List;

public class InvManager {

    private PaginatedGui itemsView;
    private RecipesView recipesView;

    public InvManager() {
        regen();
    }

    public void regen() {
        itemsView = new ItemsView().create();
        recipesView = new RecipesView();
    }

    public PaginatedGui getItemsView() {
        return itemsView;
    }


    public PaginatedGui getRecipesShowcase(final int page, final List<CustomRecipe> filteredRecipes) {
        return recipesView.create(page, filteredRecipes);
    }
}
