package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

public abstract class RecipeLoader {

    private final ConfigurationSection section;

    protected RecipeLoader(ConfigurationSection section) {
        this.section = section;
    }

    protected ConfigurationSection getSection() {
        return section;
    }

    protected ItemStack getResult() {
        ConfigurationSection resultSection = getSection().getConfigurationSection("result");
        if (resultSection == null) return null;

        if (resultSection.isString("oraxen_item"))
            return OraxenItems.getItemById(resultSection.getString("oraxen_item")).build();

        if (resultSection.isString("minecraft_type")) {
            Material material = Material.getMaterial(resultSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            return new ItemStack(material);
        }

        return resultSection.getItemStack("minecraft_item");

    }

    protected ItemStack getIndredientItemStack(ConfigurationSection ingredientSection) {
        if (ingredientSection.isString("oraxen_item"))
            return OraxenItems.getItemById(ingredientSection.getString("oraxen_item")).build();

        if (ingredientSection.isString("minecraft_type")) {
            Material material = Material.getMaterial(ingredientSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            return new ItemStack(material);
        }

        return ingredientSection.getItemStack("minecraft_item");
    }

    protected RecipeChoice getRecipeChoice(ConfigurationSection ingredientSection) {

        if (ingredientSection.isString("oraxen_item"))
            return new RecipeChoice.ExactChoice(
                    OraxenItems.getItemById(ingredientSection.getString("oraxen_item")).build());

        if (ingredientSection.isString("minecraft_type")) {
            Material material = Material.getMaterial(ingredientSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            return new RecipeChoice.MaterialChoice(material);
        }

        if (ingredientSection.isString("tag")) {
            String tagString = ingredientSection.getString("tag", "");
            NamespacedKey tagId = tagString.contains(":") ? NamespacedKey.fromString(tagString) : NamespacedKey.minecraft(tagString);
            tagId = tagId != null ? tagId : NamespacedKey.minecraft("oak_logs");
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagId, Material.class);
            if (tag == null) tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagId, Material.class);
            if (tag == null) return null;
            return new RecipeChoice.MaterialChoice(tag);
        }

        ItemStack itemStack = ingredientSection.getItemStack("minecraft_item");
        if (itemStack == null) return null;
        return new RecipeChoice.ExactChoice(itemStack);

    }

    protected NamespacedKey getNamespacedKey() {
        return new NamespacedKey(OraxenPlugin.get(), getRecipeName());
    }

    protected String getRecipeName() {
        return section.getName();
    }

    public abstract void registerRecipe();

    protected void loadRecipe(Recipe recipe) {
        Bukkit.addRecipe(recipe);
        managesPermission(CustomRecipe.fromRecipe(recipe));
    }

    private void managesPermission(CustomRecipe recipe) {
        if (getSection().isString("permission")) {
            String permission = getSection().getString("permission");
            RecipesEventsManager.get().addPermissionRecipe(recipe, permission);
        }
    }

    protected void addToWhitelistedRecipes(Recipe recipe) {
        //RecipesEventsManager.get().whitelistRecipe(CustomRecipe.fromRecipe(recipe));
    }

}
