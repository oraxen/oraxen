package io.th0rgal.oraxen.utils.inventories;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import io.th0rgal.oraxen.recipes.CustomRecipe;

import java.util.List;

public class InvManager {

    private final ChestGui itemsView;
    private final RecipesView recipesView;

    public InvManager() {
        itemsView = new ItemsView().create();
        recipesView = new RecipesView();
    }

    public ChestGui getItemsView() {
        return itemsView;
    }


    public ChestGui getRecipesShowcase(final int page, final List<CustomRecipe> filteredRecipes) {
        return recipesView.create(page, filteredRecipes);
    }
}
