package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Objects;

public class ShapedLoader extends RecipeLoader {

    public ShapedLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getNamespacedKey(), getValidResult());

        List<String> shape = getSection().getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");
        for (String ingredientLetter : Objects.requireNonNull(ingredientsSection).getKeys(false)) {
            ConfigurationSection itemSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (itemSection  == null) continue;
            RecipeChoice recipeChoice = getWorkbenchRecipeChoice(itemSection);
            if (recipeChoice == null) {
                Logs.logError("Recipe " + getRecipeName() + " has an invalid or unresolvable ingredient '" + ingredientLetter + "'; skipping recipe.");
                return;
            }
            recipe.setIngredient(ingredientLetter.charAt(0), recipeChoice);
        }
        addToWhitelistedRecipes(recipe);
        RecipesEventsManager.get().registerShapedOraxenRecipe(getRecipeName(), shape, ingredientsSection);
        loadRecipe(recipe);
    }
}
