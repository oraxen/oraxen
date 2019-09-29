package io.th0rgal.oraxen.recipes;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;


public class RecipeBuilder {

    RecipeInterface recipeBuilder;

    public RecipeBuilder(NamespacedKey namespacedKey, Type recipeType, ItemStack output) {


    }

    public enum Type {
        SHAPED_RECIPE,
        SHAPELESS_RECIPE,
        FURNACE_RECIPE,
        CAMPFIRE_RECIPE,
        SMOKING_RECIPE,
        BLASTING_RECIPE,
        MERCHAND_RECIPE
    }

}


