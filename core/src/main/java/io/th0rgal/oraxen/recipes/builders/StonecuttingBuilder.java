package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class StonecuttingBuilder extends RecipeBuilder {

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, "<glyph:recipe_stonecutter>");
    }

    public StonecuttingBuilder(Player player) {
        super(player, "stonecutting");
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public void saveRecipe(String name, String permission) {

        ItemStack input = getInventory().getItem(0);
        int recipeCount = 0;
        for (int i = 1; i < getInventory().getSize(); i++) {
            ItemStack result = getInventory().getItem(i);
            if (result == null) continue;
            ConfigurationSection newCraftSection = getConfig().createSection(name + "_" + recipeCount);
            setSerializedItem(newCraftSection.createSection("result"), result);
            setSerializedItem(newCraftSection.createSection("input"), input);

            if (permission != null && !permission.isEmpty()) newCraftSection.set("permission", permission);

            saveConfig();
            recipeCount++;
        }
        close();
    }
}
