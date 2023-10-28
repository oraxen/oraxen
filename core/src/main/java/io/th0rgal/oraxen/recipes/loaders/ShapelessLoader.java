package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.Objects;

public class ShapelessLoader extends RecipeLoader {

    public ShapelessLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getResult());
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");

        for (String ingredientLetter : Objects.requireNonNull(ingredientsSection).getKeys(false)) {
            ConfigurationSection itemSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (itemSection == null) continue;
            RecipeChoice ingredient = getRecipeChoice(itemSection);
            if (ingredient == null) continue;
            for (int i = 0; i < itemSection.getInt("amount"); i++)
                recipe.addIngredient(ingredient);
        }
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}
