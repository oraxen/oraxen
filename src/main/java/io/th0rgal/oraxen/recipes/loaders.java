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
import org.bukkit.inventory.ShapelessRecipe;

import java.util.List;

public class loaders {
    public abstract static class RecipeLoader {

        private ConfigurationSection section;

        public RecipeLoader(ConfigurationSection section) {
            this.section = section;
        }

        protected ConfigurationSection getSection() {
            return section;
        }

        protected ItemStack getResult() {
            ItemStack result;
            if (getSection().isString("result")) {
                String outputString = getSection().getString("result");
                if (OraxenItems.isAnItem(outputString))
                    result = OraxenItems.getItemById(outputString).getItem();
                else
                    result = new ItemStack(Material.valueOf(outputString));
            } else
                result = getSection().getItemStack("result");
            return result;
        }

        protected ItemStack getItemStack(Object object) {
            if (object instanceof String) {
                String stringInput = (String) object;
                if (OraxenItems.isAnItem(stringInput))
                    return OraxenItems.getItemById(stringInput).getItem();
                return new ItemStack(Material.getMaterial(stringInput));
            }
            return (ItemStack) object;
        }

        protected NamespacedKey getNamespacedKey() {
            return new NamespacedKey(OraxenPlugin.get(), getRecipeName());
        }

        protected String getRecipeName() {
            return section.getName();
        }

        public abstract void registerRecipe();

    }

    public static class ShapedLoader extends RecipeLoader {

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

    public static class ShapelessLoader extends RecipeLoader {

        public ShapelessLoader(ConfigurationSection section) {
            super(section);
        }

        @Override
        public void registerRecipe() {

            ShapelessRecipe recipe = new ShapelessRecipe(getNamespacedKey(), getResult());

        }
    }
}
