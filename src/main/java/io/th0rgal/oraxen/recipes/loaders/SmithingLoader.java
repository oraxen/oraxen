package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.recipes.RecipeTransforms;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.SmithingTransformRecipe;

public class SmithingLoader extends RecipeLoader {
    public SmithingLoader(ConfigurationSection section) { super(section); }

    @Override
    public void registerRecipe() {
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(getUniqueKey(), getValidResult(),
                getRequiredChoice("template"), getRequiredChoice("base"), getRequiredChoice("addition"));
        String source = getSection().getString("transform.source", "base");
        if (!source.equals("base"))
            throw new IllegalArgumentException("Invalid smithing transform source " + source);
        RecipeTransforms.Transform transform = RecipeTransforms.parse(getSection());
        loadRecipe(recipe);
        RecipesEventsManager.get().registerSmithingRecipe(recipe, getPermission(), transform);
    }
}
