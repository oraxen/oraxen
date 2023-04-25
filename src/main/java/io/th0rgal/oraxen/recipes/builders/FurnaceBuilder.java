package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;


public class FurnaceBuilder extends CookingBuilder {

    public FurnaceBuilder(Player player) {
        super(player, "furnace", InventoryType.FURNACE);
    }
}
