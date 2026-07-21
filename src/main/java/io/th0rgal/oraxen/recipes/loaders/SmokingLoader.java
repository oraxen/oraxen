package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.utils.logs.Logs;
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
		if (recipeChoice == null) {
			Logs.logError("Recipe " + getRecipeName() + " has an invalid or unresolvable input ingredient; skipping recipe.");
			return;
		}
		SmokingRecipe recipe = new SmokingRecipe(getNamespacedKey(), getValidResult(),
				recipeChoice, getSection().getInt("experience"), getSection().getInt("cookingTime"));
		loadRecipe(recipe);
	}
}
