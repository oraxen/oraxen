package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import net.Indyuce.mmoitems.MMOItems;
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
        ItemStack result;
        int amount = resultSection.getInt("amount", 1);

        if (resultSection.isString("oraxen_item"))
            result = ItemUpdater.updateItem(OraxenItems.getItemById(resultSection.getString("oraxen_item")).build());
        else if (resultSection.isString("crucible_item"))
            result = new WrappedCrucibleItem(resultSection.getString("crucible_item")).build();
        else if (resultSection.isString("mmoitems_id") && resultSection.isString("mmoitems_type"))
            result = MMOItems.plugin.getItem(resultSection.getString("mmoitems_type"), resultSection.getString("mmoitems_id"));
        else if (resultSection.isString("ecoitem_id"))
            result = new WrappedEcoItem(resultSection.getString("ecoitem_id")).build();
        else if (resultSection.isString("minecraft_type")) {
            Material material = Material.getMaterial(resultSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            result = new ItemStack(material);
        } else result = resultSection.getItemStack("minecraft_item");

        if (result != null) result.setAmount(amount);
        return result;
    }

    protected ItemStack getIndredientItemStack(ConfigurationSection ingredientSection) {
        if (ingredientSection.isString("oraxen_item"))
            return ItemUpdater.updateItem(OraxenItems.getItemById(ingredientSection.getString("oraxen_item")).build());

        if (ingredientSection.isString("crucible_item")) {
            return new WrappedCrucibleItem(ingredientSection.getString("crucible_item")).build();
        }

        if (ingredientSection.isString("mmoitems_id") && ingredientSection.isString("mmoitems_type")) {
            return MMOItems.plugin.getItem(ingredientSection.getString("mmoitems_type"), ingredientSection.getString("mmoitems_id"));
        }

        if (ingredientSection.isString("ecoitem_id")) {
            return new WrappedEcoItem(ingredientSection.getString("ecoitem_id")).build();
        }

        if (ingredientSection.isString("minecraft_type")) {
            Material material = Material.getMaterial(ingredientSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            return new ItemStack(material);
        }

        return ingredientSection.getItemStack("minecraft_item");
    }

    protected RecipeChoice getRecipeChoice(ConfigurationSection ingredientSection) {

        if (ingredientSection.isString("oraxen_item"))
            return new RecipeChoice.ExactChoice(ItemUpdater.updateItem(OraxenItems.getItemById(ingredientSection.getString("oraxen_item")).build()));

        if (ingredientSection.isString("crucible_item")) {
            ItemStack ingredient = new WrappedCrucibleItem(section.getString("crucible_item")).build();
            return new RecipeChoice.ExactChoice(ingredient != null ? ingredient : new ItemStack(Material.AIR));
        }

        if (ingredientSection.isString("mmoitems_id") && ingredientSection.isString("mmoitems_type")) {
            ItemStack ingredient = MMOItems.plugin.getItem(ingredientSection.getString("mmoitems_type"), ingredientSection.getString("mmoitems_id"));
            return new RecipeChoice.ExactChoice(ingredient != null ? ingredient : new ItemStack(Material.AIR));
        }

        if (ingredientSection.isString("ecoitem_id")) {
            ItemStack ingredient = new WrappedEcoItem(section.getString("ecoitem_id")).build();
            return new RecipeChoice.ExactChoice(ingredient != null ? ingredient : new ItemStack(Material.AIR));
        }

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
        RecipesEventsManager.get().whitelistRecipe(CustomRecipe.fromRecipe(recipe));
    }

}
