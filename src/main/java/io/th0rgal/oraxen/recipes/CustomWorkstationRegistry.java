package io.th0rgal.oraxen.recipes;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CustomWorkstationRegistry {
    private static volatile List<CustomWorkstationRecipe> recipes = List.of();
    private static List<CustomWorkstationRecipe> staging;

    private CustomWorkstationRegistry() {}

    public static void beginReload() { staging = new ArrayList<>(); }
    public static void register(CustomWorkstationRecipe recipe) {
        if (staging == null) throw new IllegalStateException("Recipe reload has not begun");
        staging.add(recipe);
    }
    public static void finishReload() {
        recipes = staging == null ? List.of() : List.copyOf(staging);
        staging = null;
    }

    public static CustomWorkstationRecipe match(CustomWorkstationRecipe.Type type, ItemStack first, ItemStack second) {
        for (CustomWorkstationRecipe recipe : recipes) {
            if (recipe.type() == type && recipe.matches(first, second)) return recipe;
        }
        return null;
    }

    public static List<CustomWorkstationRecipe> recipes(CustomWorkstationRecipe.Type type) {
        return recipes.stream().filter(recipe -> recipe.type() == type).toList();
    }
}
