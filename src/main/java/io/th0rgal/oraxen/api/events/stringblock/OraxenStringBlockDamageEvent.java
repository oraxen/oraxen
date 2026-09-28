package io.th0rgal.oraxen.api.events.stringblock;

import io.th0rgal.oraxen.api.events.OraxenBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired right before a player damages a string block.
 * Cancelling the event stops the damage.
 */
public class OraxenStringBlockDamageEvent extends OraxenBlockDamageEvent<StringBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockDamageEvent(@NotNull StringBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player);
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
