package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
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

        ItemStack output;
        if (getSection().isString("output")) {
            String outputString = getSection().getString("output");

            if (OraxenItems.isAnItem(outputString)) {
                output = OraxenItems.getItemById(outputString).getItem();
            } else {
                output = new ItemStack(Material.valueOf(outputString));
            }

        } else {
            output = getSection().getItemStack("output");
        }

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(OraxenPlugin.get(), getRecipeName()), output);

        List<?> inputs = getSection().getList("input");
        String shape = "ABCDEFGHI";
        recipe.shape(shape);

        for (int i = 0; i < inputs.size(); i++) {

            char shapeLetter = shape.toCharArray()[i];
            Object input = inputs.get(i);
            if (input instanceof String) {
                String stringInput = (String) input;
                if (OraxenItems.isAnItem(stringInput)) {
                    recipe.setIngredient(shapeLetter, new RecipeChoice.ExactChoice(OraxenItems.getItemById(stringInput).getItem()));
                } else {
                    recipe.setIngredient(shapeLetter, Material.getMaterial(stringInput));
                }
            } else {
                recipe.setIngredient(shapeLetter, new RecipeChoice.ExactChoice((ItemStack) input));
            }

        }

        Bukkit.addRecipe(recipe);

    }

}
