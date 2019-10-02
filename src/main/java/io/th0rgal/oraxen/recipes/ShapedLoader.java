package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;

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
            recipe.setIngredient(shape.toCharArray()[i], new RecipeChoice.ExactChoice(new ItemStack(Material.STICK)));

        }


    }

}
