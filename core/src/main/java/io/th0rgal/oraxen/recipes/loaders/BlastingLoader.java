package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.RecipeChoice;

public class BlastingLoader extends RecipeLoader {
	public BlastingLoader(ConfigurationSection section) {
		super(section);
	}

	@Override
	public void registerRecipe() {
		ConfigurationSection inputSection = getSection().getConfigurationSection("input");
		if (inputSection == null) return;
		RecipeChoice recipeChoice = getRecipeChoice(inputSection);
		if (recipeChoice == null) return;
		BlastingRecipe recipe = new BlastingRecipe(getNamespacedKey(), getResult(),
				recipeChoice, getSection().getInt("experience"), getSection().getInt("cookingTime"));
		loadRecipe(recipe);
	}
}
