package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;


public class CampfireBuilder extends CookingBuilder {

    public CampfireBuilder(Player player) {
        super(player, "campfire");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.SMOKER, inventoryTitle);
    }
}
