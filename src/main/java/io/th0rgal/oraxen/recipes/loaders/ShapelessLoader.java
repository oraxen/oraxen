package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.RecipeCrafting;
import io.th0rgal.oraxen.recipes.RecipeIngredients;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.ArrayList;
import java.util.List;

public class ShapelessLoader extends RecipeLoader {

    public ShapelessLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        List<RecipeChoice> choices = new ArrayList<>();
        Object raw = getSection().get("ingredients");
        if (raw instanceof ConfigurationSection legacy) {
            for (String name : legacy.getKeys(false)) {
                Object ingredient = legacy.get(name);
                RecipeChoice choice = getWorkbenchRecipeChoice(ingredient);
                int amount = RecipeIngredients.amount(ingredient);
                if (amount < 1 || amount > 9) throw new IllegalArgumentException("Invalid shapeless ingredient amount");
                for (int i = 0; i < amount; i++) choices.add(choice);
            }
        } else if (raw instanceof List<?> ingredients) {
            for (Object ingredient : ingredients) choices.add(getWorkbenchRecipeChoice(ingredient));
        } else throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' requires ingredients");
        if (choices.isEmpty() || choices.size() > 9)
            throw new IllegalArgumentException("A shapeless recipe requires between one and nine ingredients");
        ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getValidResult());
        choices.forEach(recipe::addIngredient);
        recipe.setGroup(getGroup());
        if (getSection().isString("category"))
            recipe.setCategory(CraftingBookCategory.valueOf(getSection().getString("category").toUpperCase(java.util.Locale.ROOT)));
        RecipeCrafting.registerShapeless(recipe, getSection(), choices);
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}
