package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.api.events.OraxenDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Fired right before a player damages a chorus block.
 * Cancelling the event stops the damage.
 */
public class OraxenChorusBlockDamageEvent extends OraxenDamageEvent<ChorusBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockDamageEvent(@NotNull ChorusBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player);
    }

    @NotNull
    @Override
    public ChorusBlockMechanic getMechanic() {
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
