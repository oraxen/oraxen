package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;

public class SmokingLoader extends RecipeLoader {
	public SmokingLoader(ConfigurationSection section) {
		super(section);
	}

	@Override
	public void registerRecipe() {
		ConfigurationSection inputSection = getSection().getConfigurationSection("input");
		if (inputSection == null) return;
		RecipeChoice recipeChoice = getRecipeChoice(inputSection);
		if (recipeChoice == null) return;
		SmokingRecipe recipe = new SmokingRecipe(getNamespacedKey(), getResult(),
				recipeChoice, getSection().getInt("experience"), getSection().getInt("cookingTime"));
		loadRecipe(recipe);
	}
}
