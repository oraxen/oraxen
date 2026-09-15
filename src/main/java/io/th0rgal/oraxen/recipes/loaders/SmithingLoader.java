package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.SmithingTransformRecipe;

public class SmithingLoader extends RecipeLoader {
    public SmithingLoader(ConfigurationSection section) { super(section); }

    @Override
    public void registerRecipe() {
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(getNamespacedKey(), getValidResult(),
                getRequiredChoice("template"), getRequiredChoice("base"), getRequiredChoice("addition"));
        loadRecipe(recipe);
        RecipesEventsManager.get().registerSmithingRecipe(recipe.getKey(), getPermission());
    }
}
