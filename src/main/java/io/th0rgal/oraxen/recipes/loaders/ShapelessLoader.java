package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

public class ShapelessLoader extends RecipeLoader {

    public ShapelessLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getResult());
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            ConfigurationSection subSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            RecipeChoice recipeChoice = getRecipeChoice(subSection);
            if (subSection == null)
                recipe.addIngredient(subSection.getInt("amount"), Material.getMaterial(subSection.getString("minecraft_type")));
            else
                for (int i = 0; i < subSection.getInt("amount"); i++)
                    recipe.addIngredient(recipeChoice);
        }

    }
}