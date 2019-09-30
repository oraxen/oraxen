package io.th0rgal.oraxen.recipes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class ShapedBuilder extends RecipeBuilder {

    public ShapedBuilder(Player player) {
        super(player, "shaped");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle);
    }

    @Override
    public void saveRecipe() {

    }

}