package io.th0rgal.oraxen.recipes.loaders;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemUpdater;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.recipes.CustomRecipe;
import io.th0rgal.oraxen.recipes.listeners.RecipesEventsManager;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

public abstract class RecipeLoader {

    private static final java.util.Set<NamespacedKey> KEYS_REGISTERED_THIS_PASS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final ConfigurationSection section;

    protected RecipeLoader(ConfigurationSection section) {
        this.section = section;
    }

    /** Called before each full recipe registration pass so duplicate keys within the pass can be detected. */
    public static void beginRegistrationPass() {
        KEYS_REGISTERED_THIS_PASS.clear();
    }

    protected ConfigurationSection getSection() {
        return section;
    }

    protected ItemStack getResult() {
        ConfigurationSection resultSection = OraxenYaml.getConfigurationSection(getSection(), "result");
        if (resultSection == null) return null;
        ItemStack result;
        int amount = resultSection.getInt("amount", 1);

        if (resultSection.isString("oraxen_item")) {
            String itemId = resultSection.getString("oraxen_item");
            ItemBuilder builder = OraxenItems.getItemById(itemId);
            if (builder == null)
                throw new IllegalArgumentException("Recipe result references unknown Oraxen item: " + itemId);
            result = ItemUpdater.updateItem(builder.build());
        } else if (resultSection.isString("crucible_item"))
            result = new WrappedCrucibleItem(resultSection.getString("crucible_item")).build();
        else if (resultSection.isString("mmoitems_id") && resultSection.isString("mmoitems_type")
                && PluginUtils.isEnabled("MMOItems"))
            result = MMOItems.plugin.getItem(resultSection.getString("mmoitems_type"), resultSection.getString("mmoitems_id"));
        else if (resultSection.isString("ecoitem_id"))
            result = new WrappedEcoItem(resultSection.getString("ecoitem_id")).build();
        else if (resultSection.isString("minecraft_type")) {
            Material material = OraxenYaml.getMaterial(resultSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            result = new ItemStack(material);
        } else result = resultSection.getItemStack("minecraft_item");

        if (result != null) result.setAmount(amount);
        return result;
    }

    protected ItemStack getValidResult() {
        ItemStack result = getResult();
        if (result == null || result.isEmpty())
            throw new IllegalArgumentException("Recipe '" + section.getName() + "' result is missing or invalid");
        return result;
    }

    protected ItemStack getIndredientItemStack(ConfigurationSection ingredientSection) {
        if (ingredientSection.isString("oraxen_item")) {
            String itemId = ingredientSection.getString("oraxen_item");
            ItemBuilder builder = OraxenItems.getItemById(itemId);
            if (builder == null)
                throw new IllegalArgumentException("Recipe " + section.getName() + " references unknown Oraxen ingredient: " + itemId);
            return ItemUpdater.updateItem(builder.build());
        }

        if (ingredientSection.isString("crucible_item")) {
            return new WrappedCrucibleItem(ingredientSection.getString("crucible_item")).build();
        }

        if (ingredientSection.isString("mmoitems_id") && ingredientSection.isString("mmoitems_type")
                && PluginUtils.isEnabled("MMOItems")) {
            return MMOItems.plugin.getItem(ingredientSection.getString("mmoitems_type"), ingredientSection.getString("mmoitems_id"));
        }

        if (ingredientSection.isString("ecoitem_id")) {
            return new WrappedEcoItem(ingredientSection.getString("ecoitem_id")).build();
        }

        if (ingredientSection.isString("minecraft_type")) {
            Material material = OraxenYaml.getMaterial(ingredientSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir()) return null;
            return new ItemStack(material);
        }

        return ingredientSection.getItemStack("minecraft_item");
    }

    protected RecipeChoice getRecipeChoice(ConfigurationSection ingredientSection) {
        return getRecipeChoice(ingredientSection, true);
    }

    protected RecipeChoice getWorkbenchRecipeChoice(ConfigurationSection ingredientSection) {
        return getRecipeChoice(ingredientSection, false);
    }

    private RecipeChoice getRecipeChoice(ConfigurationSection ingredientSection, boolean exactOraxenChoice) {

        if (ingredientSection.isString("oraxen_item")) {
            String itemId = ingredientSection.getString("oraxen_item");
            ItemBuilder builder = OraxenItems.getItemById(itemId);
            if (builder == null)
                throw new IllegalArgumentException("Recipe " + section.getName() + " references unknown Oraxen ingredient: " + itemId);
            ItemStack ingredient = ItemUpdater.updateItem(builder.build());
            return exactOraxenChoice || !hasBackpackMechanic(itemId) ? new RecipeChoice.ExactChoice(ingredient) : new RecipeChoice.MaterialChoice(ingredient.getType());
        }

        if (ingredientSection.isString("crucible_item")) {
            ItemStack ingredient = new WrappedCrucibleItem(ingredientSection.getString("crucible_item")).build();
            if (ingredient == null || ingredient.getType().isAir()) return null;
            return new RecipeChoice.ExactChoice(ingredient);
        }

        if (ingredientSection.isString("mmoitems_id") && ingredientSection.isString("mmoitems_type")
                && PluginUtils.isEnabled("MMOItems")) {
            ItemStack ingredient = MMOItems.plugin.getItem(ingredientSection.getString("mmoitems_type"), ingredientSection.getString("mmoitems_id"));
            if (ingredient == null || ingredient.getType().isAir()) return null;
            return new RecipeChoice.ExactChoice(ingredient);
        }

        if (ingredientSection.isString("ecoitem_id")) {
            ItemStack ingredient = new WrappedEcoItem(ingredientSection.getString("ecoitem_id")).build();
            if (ingredient == null || ingredient.getType().isAir()) return null;
            return new RecipeChoice.ExactChoice(ingredient);
        }

        if (ingredientSection.isString("minecraft_type")) {
            Material material = OraxenYaml.getMaterial(ingredientSection.getString("minecraft_type", "AIR"));
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

    private boolean hasBackpackMechanic(String itemId) {
        MechanicFactory backpackFactory = MechanicsManager.getMechanicFactory("backpack");
        return backpackFactory != null && backpackFactory.getMechanic(itemId) != null;
    }

    protected NamespacedKey getNamespacedKey() {
        return new NamespacedKey(OraxenPlugin.get(), getRecipeName());
    }

    protected String getRecipeName() {
        return section.getName();
    }

    protected String getGroup() {
        return getSection().getString("group", "");
    }

    public abstract void registerRecipe();

    protected void loadRecipe(Recipe recipe) {
        if (recipe instanceof Keyed keyed
                && keyed.getKey().getNamespace().equals(OraxenPlugin.get().getName().toLowerCase(java.util.Locale.ROOT))) {
            if (!KEYS_REGISTERED_THIS_PASS.add(keyed.getKey()))
                Logs.logWarning("Duplicate recipe name '" + getRecipeName() + "': another recipe with key '"
                        + keyed.getKey() + "' was already loaded and will be replaced. Rename one of the recipes to keep both.");
            Bukkit.removeRecipe(keyed.getKey());
        }
        if (!Bukkit.addRecipe(recipe))
            throw new IllegalStateException("Bukkit rejected recipe '" + getRecipeName() + "'");
        managesPermission(CustomRecipe.fromRecipe(recipe));
    }

    private void managesPermission(CustomRecipe recipe) {
        if (recipe != null && getSection().isString("permission")) {
            String permission = getSection().getString("permission");
            RecipesEventsManager.get().addPermissionRecipe(recipe, permission);
        }
    }

    protected String getPermission() {
        return getSection().getString("permission");
    }

    protected RecipeChoice getRequiredChoice(String key) {
        ConfigurationSection ingredient = getSection().getConfigurationSection(key);
        RecipeChoice choice = ingredient == null ? null : getRecipeChoice(ingredient);
        if (choice == null) throw new IllegalArgumentException("Recipe '" + getRecipeName() + "' has an invalid " + key + " ingredient");
        return choice;
    }

    protected int getIngredientAmount(String key) {
        ConfigurationSection ingredient = getSection().getConfigurationSection(key);
        return ingredient == null ? 1 : Math.max(1, ingredient.getInt("amount", 1));
    }

    protected void addToWhitelistedRecipes(Recipe recipe) {
        RecipesEventsManager.get().whitelistRecipe(CustomRecipe.fromRecipe(recipe));
    }

}
