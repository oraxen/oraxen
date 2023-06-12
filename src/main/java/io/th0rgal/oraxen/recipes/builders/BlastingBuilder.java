package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;


public class BlastingBuilder extends CookingBuilder {

    public BlastingBuilder(Player player) {
        super(player, "blasting");
    }

    @Override
    Inventory createInventory(Player player, String inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.BLAST_FURNACE, inventoryTitle);
    }
}
