package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public abstract class RecipeLoader {

    private ConfigurationSection section;

    public RecipeLoader(ConfigurationSection section) {
        this.section = section;
    }

    protected ConfigurationSection getSection() {
        return section;
    }

    protected ItemStack getResult() {
        ConfigurationSection resultSection = getSection().getConfigurationSection("result");

        if (resultSection.isString("oraxen_item"))
            return OraxenItems.getItemById(resultSection.getString("oraxen_item")).getItem();

        if (resultSection.isString("minecraft_type"))
            return new ItemStack(Material.getMaterial(resultSection.getString("minecraft_type")));

        return resultSection.getItemStack("minecraft_item");

    }

    protected RecipeChoice getRecipeChoice(ConfigurationSection ingredientSection) {

        if (ingredientSection.isString("oraxen_item"))
            return new RecipeChoice.ExactChoice(OraxenItems.getItemById(ingredientSection.getString("oraxen_item")).getItem());

        if (ingredientSection.isString("minecraft_type"))
            return new RecipeChoice.MaterialChoice(Material.getMaterial(ingredientSection.getString("minecraft_type")));

        return new RecipeChoice.ExactChoice(ingredientSection.getItemStack("minecraft_item"));

    }

    protected NamespacedKey getNamespacedKey() {
        return new NamespacedKey(OraxenPlugin.get(), getRecipeName());
    }

    protected String getRecipeName() {
        return section.getName();
    }

    public abstract void registerRecipe();

}





