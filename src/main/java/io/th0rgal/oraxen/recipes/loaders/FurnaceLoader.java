package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.RecipeChoice;

public class FurnaceLoader extends RecipeLoader {

    public FurnaceLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        ConfigurationSection inputSection = getSection().getConfigurationSection("input");
        if (inputSection == null) return;
        RecipeChoice recipeChoice = getRecipeChoice(inputSection);
        if (recipeChoice == null) return;
        FurnaceRecipe recipe = new FurnaceRecipe(getNamespacedKey(), getResult(),
                recipeChoice, getSection().getInt("experience"), getSection().getInt("cookingTime"));
        // addToWhitelistedRecipes(recipe); <- no whitelist for furnace recipes
        loadRecipe(recipe);
    }
}
