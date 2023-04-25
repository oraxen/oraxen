package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;


public class SmokingBuilder extends CookingBuilder {

    public SmokingBuilder(Player player) {
        super(player, "smoking", InventoryType.SMOKER);
    }
}
