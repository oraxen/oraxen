package io.th0rgal.oraxen.recipes.loaders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

public class ShapedLoader extends RecipeLoader {

    public ShapedLoader(ConfigurationSection section) {
        super(section);
    }

    @SuppressWarnings("deprecation") //because we are using RecipeChoice
    @Override
    public void registerRecipe() {

        ShapedRecipe recipe = new ShapedRecipe(getNamespacedKey(), getResult());

        List<?> inputs = getSection().getList("input");
        String shape = "ABCDEFGH";
        recipe.shape(shape);

        for (int i = 0; i < inputs.size(); i++) {
            char shapeLetter = shape.toCharArray()[i];
            recipe.setIngredient(shapeLetter, new RecipeChoice.ExactChoice(getItemStack(inputs.get(i))));
        }

        Bukkit.addRecipe(recipe);

    }

}