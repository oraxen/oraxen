package io.th0rgal.oraxen.recipes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public record CustomWorkstationRecipe(String name, Type type, Ingredient base, Ingredient addition,
                                      ItemStack result, int value, String permission, String fluid, int level) {

    public enum Type { ANVIL, GRINDSTONE, CAULDRON }

    public record Ingredient(RecipeChoice choice, int amount) {
        public Ingredient {
            amount = Math.max(1, amount);
        }

        public boolean matches(ItemStack item) {
            return item != null && !item.isEmpty() && item.getAmount() >= amount && choice.test(item);
        }
    }

    public boolean matches(ItemStack first, ItemStack second) {
        if (!base.matches(first)) return false;
        return addition == null ? second == null || second.isEmpty() : addition.matches(second);
    }

    public ItemStack createResult() {
        return result.clone();
    }
}
