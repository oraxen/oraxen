package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.RecipeChoice;

public class CampfireLoader extends RecipeLoader {
	public CampfireLoader(ConfigurationSection section) {
		super(section);
	}

	@Override
	public void registerRecipe() {
		ConfigurationSection inputSection = getSection().getConfigurationSection("input");
		if (inputSection == null) return;
		RecipeChoice recipeChoice = getRecipeChoice(inputSection);
		if (recipeChoice == null) return;
		CampfireRecipe recipe = new CampfireRecipe(getNamespacedKey(), getResult(),
				recipeChoice, getSection().getInt("experience"), getSection().getInt("cookingTime"));
		loadRecipe(recipe);
	}
}
