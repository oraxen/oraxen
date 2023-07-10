package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated This event is deprecated and will be removed in a future version.
 * Use {@link io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent} instead.
 */
@Deprecated(since = "1.158.0", forRemoval = true)
public class OraxenStringBlockBreakEvent extends Event implements Cancellable {

    private final StringBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockBreakEvent(@NotNull final StringBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
    }

    /**
     * @return The StringBlockMechanic of this block
     */
    @NotNull
    public StringBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who broke this block
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was broken
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
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
