package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingRecipe;

public class SmithingLoader extends RecipeLoader {
	public SmithingLoader(ConfigurationSection section) {
		super(section);
	}

	@Override
	public void registerRecipe() {
		ConfigurationSection inputSection = getSection().getConfigurationSection("base");
		ConfigurationSection additionSection = getSection().getConfigurationSection("addition");
		if (inputSection == null) return;
		if (additionSection == null) return;
		RecipeChoice baseChoice = getRecipeChoice(inputSection);
		RecipeChoice additionChoice = getRecipeChoice(additionSection);
		if (baseChoice == null) return;
		if (additionChoice == null) return;
		SmithingRecipe recipe = new SmithingRecipe(getNamespacedKey(), getResult(), baseChoice, additionChoice);
		loadRecipe(recipe);
	}
}
