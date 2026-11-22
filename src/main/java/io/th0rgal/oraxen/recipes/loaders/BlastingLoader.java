package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.recipes.RecipeProcessing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.BlastingRecipe;

public class BlastingLoader extends RecipeLoader {

    public BlastingLoader(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void registerRecipe() {
        int time = getSection().getInt("cooking-time", getSection().getInt("cookingTime", 100));
        double experience = getSection().getDouble("experience", 0);
        if (time <= 0 || !Double.isFinite(experience) || experience < 0)
            throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has invalid cooking settings");
        BlastingRecipe recipe = new BlastingRecipe(getNamespacedKey(), getValidResult(),
                getRequiredChoice("input"), (float) experience, time);
        recipe.setGroup(getGroup());
        RecipeProcessing.register(recipe, recipe.getInputChoice());
        loadRecipe(recipe);
    }
}
