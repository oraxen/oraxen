package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class ShapelessLoader extends RecipeLoader {

    public ShapelessLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        /* TODO: REWRITE IT
        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getResult());
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            ConfigurationSection subSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            RecipeChoice recipeChoice = getRecipeChoice(subSection);
            recipe.addIngredient(recipeChoice);
        }
        Bukkit.addRecipe(recipe);*/
    }
}