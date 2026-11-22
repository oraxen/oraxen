package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.recipes.loaders.BrewingLoader;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.Objects;

public record RecipeBrewing(NamespacedKey key, RecipeChoice container, RecipeChoice ingredient, ItemStack result) {
    public static void beginReload() { BrewingLoader.beginReload(); }
    public static void validateReload() { BrewingLoader.validateReload(); }
    public static void finishReload() { BrewingLoader.finishReload(); }
    public static void cancelReload() { BrewingLoader.cancelReload(); }
    public static void clear() { BrewingLoader.clear(); }

    public RecipeBrewing {
        Objects.requireNonNull(key, "key");
        container = Objects.requireNonNull(container, "container").clone();
        ingredient = Objects.requireNonNull(ingredient, "ingredient").clone();
        result = Objects.requireNonNull(result, "result").clone();
        if (result.isEmpty() || result.getAmount() < 1)
            throw new IllegalArgumentException("Brewing result must not be empty");
    }

    @Override
    public ItemStack result() { return result.clone(); }

    @Override
    public RecipeChoice container() { return container.clone(); }

    @Override
    public RecipeChoice ingredient() { return ingredient.clone(); }

    public boolean matches(ItemStack bottle, ItemStack reagent) {
        return bottle != null && !bottle.isEmpty() && bottle.getAmount() > 0
                && reagent != null && !reagent.isEmpty() && reagent.getAmount() > 0
                && container.test(bottle) && ingredient.test(reagent);
    }
}
