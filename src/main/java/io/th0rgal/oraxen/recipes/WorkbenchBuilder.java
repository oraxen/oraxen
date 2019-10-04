package io.th0rgal.oraxen.recipes;

import io.th0rgal.oraxen.recipes.builders.RecipeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public abstract class WorkbenchBuilder extends RecipeBuilder {

    public WorkbenchBuilder(Player player, String builderName) {
        super(player, builderName + " workbench");
    }

    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle);
    }

}
