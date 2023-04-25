package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class FurnaceBuilder extends CookingBuilder {

    public FurnaceBuilder(Player player) {
        super(player, "furnace", InventoryType.FURNACE);
    }
}
