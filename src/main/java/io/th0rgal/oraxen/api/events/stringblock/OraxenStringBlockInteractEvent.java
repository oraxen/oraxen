package io.th0rgal.oraxen.api.events.stringblock;

import io.th0rgal.oraxen.api.events.OraxenBlockInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenStringBlockInteractEvent extends OraxenBlockInteractEvent<StringBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockInteractEvent(@NotNull StringBlockMechanic mechanic, @NotNull Player player,
                                          @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand,
                                          @NotNull Block block, @NotNull BlockFace blockFace) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
