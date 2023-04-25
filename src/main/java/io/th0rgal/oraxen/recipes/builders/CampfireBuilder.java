package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;


public class CampfireBuilder extends CookingBuilder {

    public CampfireBuilder(Player player) {
        super(player, "campfire", InventoryType.SMOKER);
    }
}
