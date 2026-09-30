package io.th0rgal.oraxen.recipes.builders;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class AnvilBuilder extends WorkstationBuilder {

    public AnvilBuilder(Player player) {
        super(player, "anvil", "experience_cost");
    }

    @Override
    Inventory createInventory(Player player, Component inventoryTitle) {
        return Bukkit.createInventory(player, InventoryType.ANVIL, inventoryTitle);
    }

    public void setExperienceCost(int experienceCost) {
        setValue(experienceCost);
    }
}
