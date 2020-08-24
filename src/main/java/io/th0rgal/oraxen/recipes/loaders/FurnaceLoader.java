package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.FurnaceRecipe;

import java.util.Objects;

public class FurnaceLoader extends RecipeLoader {

    public FurnaceLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        FurnaceRecipe recipe = new FurnaceRecipe(getNamespacedKey(), getResult(),
            getRecipeChoice(Objects.requireNonNull(getSection().getConfigurationSection("input"))),
            getSection().getInt("experience"), getSection().getInt("cookingTime"));
        // addToWhitelistedRecipes(recipe); <- no whitelist for furnace recipes
        loadRecipe(recipe);
    }
}