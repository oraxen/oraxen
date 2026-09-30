package io.th0rgal.oraxen.api.events.shapedblock;

import io.th0rgal.oraxen.api.events.OraxenBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OraxenShapedBlockBreakEvent extends OraxenBreakEvent<ShapedBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenShapedBlockBreakEvent(@NotNull ShapedBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player, mechanic.getDrop(player.getInventory().getItemInMainHand()));
    }

    @NotNull
    @Override
    public ShapedBlockMechanic getMechanic() {
        return super.getMechanic();
    }

    @NotNull
    @Override
    public Block getBlock() {
        return Objects.requireNonNull(super.getBlock());
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
