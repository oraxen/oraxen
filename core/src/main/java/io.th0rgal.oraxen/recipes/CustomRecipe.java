package io.th0rgal.oraxen.recipes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomRecipe {

    private final String name;
    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private boolean ordered;

    public CustomRecipe(String name, ItemStack result, List<ItemStack> ingredients) {
        this.name = name;
        this.result = result;
        this.ingredients = ingredients;
    }

    public CustomRecipe(String name, ItemStack result, List<ItemStack> ingredients, boolean ordered) {
        this.name = name;
        this.result = result;
        this.ingredients = ingredients;
        this.ordered = ordered;
    }

    /*
     *
     */

    public String getName() {
        return name;
    }

    public ItemStack getResult() {
        return result;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    public boolean isOrdered() {
        return ordered;
    }

    /*
     *
     */

    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;
        if (object == this)
            return true;
        if (object instanceof CustomRecipe customRecipe) {
            return result.equals(customRecipe.result) && areEqual(ingredients, customRecipe.ingredients);
        }
        return false;
    }

    /*
     *
     */
    @Override
    public int hashCode() {
        return result.hashCode() / 2 + ingredients.hashCode() / 2 + (ordered ? 1 : 0);
    }

    /*
     *
     */

    private boolean areEqual(List<ItemStack> ingredients1, List<ItemStack> ingredients2) {
        if (ingredients1.size() != ingredients2.size())
            return false;
        for (int index = 0; index < ingredients1.size(); index++) {
            ItemStack ingredient1 = ingredients1.get(index);
            if (ordered) {
                ItemStack ingredient2 = ingredients2.get(index);
                if (ingredient1 == null && ingredient2 == null)
                    continue;
                if (ingredient1 == null || ingredient2 == null)
                    return false;
                if (!ingredient1.isSimilar(ingredient2))
                    return false;
            } else {
                if (ingredient1 == null)
                    continue;
                if (ingredients2.stream().noneMatch(ingredient1::isSimilar))
                    return false;
            }
        }
        return true;
    }

    /*
     *
     */

    public static CustomRecipe fromRecipe(Recipe bukkitRecipe) {
        if (bukkitRecipe instanceof ShapedRecipe recipe) {
            List<ItemStack> ingredients = new ArrayList<>(9);
            Map<Character, ItemStack> map = recipe.getIngredientMap();
            for (String row : recipe.getShape()) {
                char[] chars = row.toCharArray();
                for (int charIndex = 0; charIndex < 3; charIndex++) {
                    if (charIndex >= chars.length) {
                        ingredients.add(null);
                        continue;
                    }
                    ingredients.add(map.get(chars[charIndex]));
                }
            }
            return new CustomRecipe(recipe.getKey().getKey(), recipe.getResult(), ingredients, true);
        } else if (bukkitRecipe instanceof ShapelessRecipe recipe) {
            List<ItemStack> ingredients = new ArrayList<>(9);
            ingredients.addAll(recipe.getIngredientList());
            return new CustomRecipe(recipe.getKey().getKey(), recipe.getResult(), ingredients, false);
        } else {
            return null;
        }
    }
}
