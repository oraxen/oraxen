package io.th0rgal.oraxen.api.events.custom_block.stringblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenStringBlockPlaceEvent extends OraxenBlockPlaceEvent implements Cancellable {

    public OraxenStringBlockPlaceEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Block block, @NotNull Player player, @NotNull ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player, itemInHand, hand);
    }

    /**
     * @return The StringBlockMechanic of this block
     */
    @NotNull
    @Override
    public StringBlockMechanic getMechanic() {
        return (StringBlockMechanic) super.getMechanic();
    }

}

