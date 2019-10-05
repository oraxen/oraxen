package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapedRecipe;

import java.util.Arrays;
import java.util.List;

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
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            recipe.setIngredient(
                    ingredientLetter.charAt(0),
                    getRecipeChoice(ingredientsSection
                            .getConfigurationSection(ingredientLetter)));
        }
        Logs.log("adding the recipe");
        Bukkit.addRecipe(recipe);
    }
}