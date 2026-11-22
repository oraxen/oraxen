package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.RecipeCrafting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.recipe.CraftingBookCategory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShapedLoader extends RecipeLoader {

    public ShapedLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        String[] shape = getSection().getStringList("shape").stream()
                .map(row -> row.replace('_', ' ')).toArray(String[]::new);
        if (shape.length < 1 || shape.length > 3 || shape[0].isEmpty() || shape[0].length() > 3)
            throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid shape");
        for (String row : shape)
            if (row.length() != shape[0].length())
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has unequal shape rows");
        ConfigurationSection ingredients = getSection().getConfigurationSection("ingredients");
        if (ingredients == null) throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' requires ingredients");
        Map<Character, RecipeChoice> choices = new LinkedHashMap<>();
        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1 || symbol.charAt(0) == ' ')
                throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid ingredient symbol");
            Object value = ingredients.get(symbol);
            choices.put(symbol.charAt(0), getWorkbenchRecipeChoice(value));
        }
        ShapedRecipe recipe = new ShapedRecipe(getUniqueKey(), getValidResult());
        recipe.shape(shape);
        for (Map.Entry<Character, RecipeChoice> choice : choices.entrySet())
            recipe.setIngredient(choice.getKey(), choice.getValue());
        for (String row : shape)
            for (char symbol : row.toCharArray())
                if (symbol != ' ' && !choices.containsKey(symbol))
                    throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an undefined symbol " + symbol);
        recipe.setGroup(getGroup());
        if (getSection().isString("category"))
            recipe.setCategory(CraftingBookCategory.valueOf(getSection().getString("category").toUpperCase(java.util.Locale.ROOT)));
        RecipeCrafting.registerShaped(recipe, getSection(), choices);
        addToWhitelistedRecipes(recipe);
        loadRecipe(recipe);
    }
}
