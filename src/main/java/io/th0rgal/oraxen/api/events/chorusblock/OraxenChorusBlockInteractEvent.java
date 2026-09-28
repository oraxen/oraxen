package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.api.events.OraxenBlockInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenChorusBlockInteractEvent extends OraxenBlockInteractEvent<ChorusBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockInteractEvent(@NotNull ChorusBlockMechanic mechanic, @NotNull Player player,
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
