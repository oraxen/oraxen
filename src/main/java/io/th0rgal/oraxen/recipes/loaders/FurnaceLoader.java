package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.FurnaceRecipe;

public class FurnaceLoader extends RecipeLoader {

    public FurnaceLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        FurnaceRecipe recipe = new FurnaceRecipe(getNamespacedKey(), getResult(), getRecipeChoice(getSection().getConfigurationSection("input")), getSection().getInt("experience"), getSection().getInt("cookingTime"));
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}