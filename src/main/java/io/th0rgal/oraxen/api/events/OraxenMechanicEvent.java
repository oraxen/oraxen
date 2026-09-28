package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mechanic, player, and optional block shared by damage, place, break, and interact events.
 * Concrete events keep their own {@link org.bukkit.event.HandlerList}.
 */
public abstract class OraxenMechanicEvent<M extends Mechanic> extends Event implements Cancellable {

    private final M mechanic;
    private final Player player;
    private final Block block;
    private boolean cancelled;

    protected OraxenMechanicEvent(@NotNull M mechanic, @Nullable Block block, @NotNull Player player) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
    }

    /**
     * @return the mechanic for this event
     */
    @NotNull
    public M getMechanic() {
        return mechanic;
    }

    /**
     * @return the player for this event
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return the block for this event, or null when there is no block
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
