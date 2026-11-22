package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomRecipe {

    private final String name;
    private final NamespacedKey key;
    private final ItemStack result;
    private final List<ItemStack> ingredients;
    private boolean ordered;

    public CustomRecipe(String name, ItemStack result, List<ItemStack> ingredients) {
        this.key = NamespacedKey.fromString(name.contains(":") ? name : "oraxen:" + name);
        this.name = key.getKey();
        this.result = result;
        this.ingredients = ingredients;
    }

    public CustomRecipe(String name, ItemStack result, List<ItemStack> ingredients, boolean ordered) {
        this.key = NamespacedKey.fromString(name.contains(":") ? name : "oraxen:" + name);
        this.name = key.getKey();
        this.result = result;
        this.ingredients = ingredients;
        this.ordered = ordered;
    }

    /*
     *
     */

    public NamespacedKey getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public ItemStack getResult() {
        return result;
    }

    public List<ItemStack> getIngredients() {
        return !ingredients.isEmpty() ? ingredients : new ArrayList<>();
    }

    public boolean isOrdered() {
        return ordered;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        return object instanceof CustomRecipe customRecipe && ordered == customRecipe.ordered
                && result.equals(customRecipe.result) && areEqual(ingredients, customRecipe.ingredients);
    }

    @Override
    public int hashCode() {
        // Ingredients compare by similarity and, when unordered, in any order, so they stay out of the hash.
        return Objects.hash(result, ordered);
    }

    private boolean areEqual(List<ItemStack> ingredients1, List<ItemStack> ingredients2) {
        if (ordered) {
            if (ingredients1.size() != ingredients2.size()) return false;
            for (int index = 0; index < ingredients1.size(); index++) {
                ItemStack ingredient1 = ingredients1.get(index);
                ItemStack ingredient2 = ingredients2.get(index);
                if (ingredient1 == null || ingredient2 == null ? ingredient1 != ingredient2 : !ingredient1.isSimilar(ingredient2))
                    return false;
            }
            return true;
        }
        List<ItemStack> remaining = new ArrayList<>(ingredients2);
        remaining.removeIf(Objects::isNull);
        for (ItemStack ingredient : ingredients1) {
            if (ingredient == null) continue;
            int match = -1;
            for (int index = 0; index < remaining.size() && match < 0; index++)
                if (ingredient.isSimilar(remaining.get(index))) match = index;
            if (match < 0) return false;
            remaining.remove(match);
        }
        return remaining.isEmpty();
    }

    public static CustomRecipe fromRecipe(Recipe bukkitRecipe) {
        if (bukkitRecipe instanceof ShapedRecipe recipe) {
            List<ItemStack> ingredients = new ArrayList<>(9);
            Map<Character, RecipeChoice> map = recipe.getChoiceMap();
            for (String row : recipe.getShape()) {
                char[] chars = row.toCharArray();
                for (int charIndex = 0; charIndex < 3; charIndex++) {
                    if (charIndex >= chars.length) {
                        ingredients.add(null);
                        continue;
                    }
                    ingredients.add(getRepresentativeItem(map.get(chars[charIndex])));
                }
            }
            return new CustomRecipe(recipe.getKey().toString(), recipe.getResult(), ingredients, true);
        } else if (bukkitRecipe instanceof ShapelessRecipe recipe) {
            List<ItemStack> ingredients = new ArrayList<>(9);
            for (RecipeChoice choice : recipe.getChoiceList()) {
                ingredients.add(getRepresentativeItem(choice));
            }
            return new CustomRecipe(recipe.getKey().toString(), recipe.getResult(), ingredients, false);
        } else return null;
    }

    private static ItemStack getRepresentativeItem(RecipeChoice choice) {
        return choice == null ? null : choice.getItemStack().clone();
    }

    /**
     * Checks if the recipe is a dye recipe.
     * This does not ensure the second ingredient is dyeable,
     * only that the ingredient is not CustomArmor and therefore dyeable
     */
    public boolean isValidDyeRecipe() {
        if (!isDyeRecipe()) return false;
        List<ItemStack> items = ingredients.stream().filter(i -> i != null && !i.getType().toString().endsWith("_DYE")).toList();
        if (items.size() != 1) return false;
        ItemStack item = items.get(0);
        if (item == null) return false;
        return !OraxenItems.exists(item) || !item.hasItemMeta() || !(item.getItemMeta() instanceof LeatherArmorMeta) || item.getType() == Material.LEATHER_HORSE_ARMOR;
    }

    private boolean isDyeRecipe() {
        return ingredients.stream().filter(Objects::nonNull).toList().size() == 2 && ingredients.stream().anyMatch(item -> item != null && item.getType().toString().endsWith("_DYE"));
    }
}
