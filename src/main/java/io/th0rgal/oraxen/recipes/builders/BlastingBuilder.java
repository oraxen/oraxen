package io.th0rgal.oraxen.recipes.builders;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;


public class BlastingBuilder extends CookingBuilder {

    public BlastingBuilder(Player player) {
        super(player, "blasting", InventoryType.BLAST_FURNACE);
    }
}
