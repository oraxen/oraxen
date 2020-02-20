package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.CustomRecipe;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<Character, ItemStack> ingredients = new HashMap<>();
        for (String ingredientLetter : ingredientsSection.getKeys(false)) {
            ConfigurationSection itemSection = ingredientsSection.getConfigurationSection(ingredientLetter);
            ingredients.put(ingredientLetter.charAt(0), getIndredientItemStack(itemSection));
            recipe.setIngredient(
                    ingredientLetter.charAt(0),
                    getRecipeChoice(itemSection));
        }
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}