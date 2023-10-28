package io.th0rgal.oraxen.recipes.loaders;

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
        ShapedRecipe recipe = new ShapedRecipe(getNamespacedKey(), getResult());

        List<String> shape = getSection().getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));
        ConfigurationSection ingredientsSection = getSection().getConfigurationSection("ingredients");
        for (String ingredientLetter : Objects.requireNonNull(ingredientsSection).getKeys(false)) {
            ConfigurationSection itemSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            if (itemSection  == null) continue;
            RecipeChoice recipeChoice = getRecipeChoice(itemSection);
            if (recipeChoice == null) continue;
            recipe.setIngredient(ingredientLetter.charAt(0), recipeChoice);
        }
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}
