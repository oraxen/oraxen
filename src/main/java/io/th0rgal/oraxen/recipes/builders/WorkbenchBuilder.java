package io.th0rgal.oraxen.recipes.builders;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public abstract class WorkbenchBuilder extends RecipeBuilder {

    protected WorkbenchBuilder(Player player, String builderName) {
        super(player, builderName);
    }

    @Override
    Inventory createInventory(Player player, Component inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.WORKBENCH, inventoryTitle);
    }

}