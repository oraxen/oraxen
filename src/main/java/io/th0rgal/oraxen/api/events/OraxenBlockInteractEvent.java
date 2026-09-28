package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class OraxenBlockInteractEvent<M extends Mechanic> extends OraxenInteractEvent<M> {

    protected OraxenBlockInteractEvent(@NotNull M mechanic, @NotNull Player player, @Nullable ItemStack itemInHand,
                                       @NotNull EquipmentSlot hand, @NotNull Block block, @NotNull BlockFace blockFace) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
    }

    @NotNull
    @Override
    public Block getBlock() {
        return super.getBlock();
    }

    @NotNull
    @Override
    public BlockFace getBlockFace() {
        return super.getBlockFace();
    }
}
