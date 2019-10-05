package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public abstract class RecipeLoader {

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





