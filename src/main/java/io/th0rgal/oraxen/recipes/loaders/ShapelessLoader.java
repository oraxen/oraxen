package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
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
        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getValidResult());
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");

        for (String ingredientLetter : Objects.requireNonNull(ingredientsSection).getKeys(false)) {
            ConfigurationSection itemSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (itemSection == null) continue;
            RecipeChoice ingredient = getWorkbenchRecipeChoice(itemSection);
            if (ingredient == null) {
                Logs.logError("Recipe " + getRecipeName() + " has an invalid or unresolvable ingredient '" + ingredientLetter + "'; skipping recipe.");
                return;
            }
            for (int i = 0; i < itemSection.getInt("amount"); i++)
                recipe.addIngredient(ingredient);
        }
        addToWhitelistedRecipes(recipe);
        RecipesEventsManager.get().registerShapelessOraxenRecipe(getRecipeName(), ingredientsSection);
        loadRecipe(recipe);
    }
}
