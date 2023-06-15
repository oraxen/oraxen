package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class StonecuttingBuilder extends RecipeBuilder {

    public StonecuttingBuilder(Player player) {
        super(player, "stonecutting");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.STONECUTTER, inventoryTitle);
    }

    @Override
    public void saveRecipe(String name) {
        saveRecipe(name, null);
    }

    @Override
    public void saveRecipe(String name, String permission) {

        ItemStack[] content = getInventory().getContents();
        ConfigurationSection newCraftSection = getConfig().createSection(name);
        setSerializedItem(newCraftSection.createSection("result"), content[1]);
        setSerializedItem(newCraftSection.createSection("input"), content[0]);

        if (permission != null)
            newCraftSection.set("permission", permission);

        saveConfig();
        close();
    }
}
