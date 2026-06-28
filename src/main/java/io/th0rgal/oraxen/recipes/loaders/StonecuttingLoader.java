package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;

public class StonecuttingLoader extends RecipeLoader {
	public StonecuttingLoader(ConfigurationSection section) {
		super(section);
	}

	@Override
	public void registerRecipe() {
		ConfigurationSection inputSection = getSection().getConfigurationSection("input");
		if (inputSection == null) return;
		RecipeChoice recipeChoice = getRecipeChoice(inputSection);
		if (recipeChoice == null) {
			Logs.logError("Recipe " + getRecipeName() + " has an invalid or unresolvable input ingredient; skipping recipe.");
			return;
		}
		StonecuttingRecipe recipe = new StonecuttingRecipe(getNamespacedKey(), getValidResult(), recipeChoice);
		loadRecipe(recipe);
	}
}
