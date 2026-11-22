package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.RecipeProcessing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.StonecuttingRecipe;

public class StonecuttingLoader extends RecipeLoader {

    public StonecuttingLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        StonecuttingRecipe recipe = new StonecuttingRecipe(getNamespacedKey(), getValidResult(), getRequiredChoice("input"));
        recipe.setGroup(getGroup());
        RecipeProcessing.register(recipe, recipe.getInputChoice());
        loadRecipe(recipe);
    }
}
