package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired right before a player damages a ChorusBlock
 * If cancelled, the block will not be damaged.
 * @see ChorusBlockMechanic
 */
public class OraxenChorusBlockDamageEvent extends Event implements Cancellable {

    private final ChorusBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @param mechanic The ChorusBlockMechanic of this block
     * @param block The block that was damaged
     * @param player The player who damaged this block
     */
    public OraxenChorusBlockDamageEvent(@NotNull final ChorusBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.isCancelled = false;
    }

    /**
     * @return The ChorusBlockMechanic of the damaged block
     */
    @NotNull
    public ChorusBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who damaged the ChorusBlock
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was damaged
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
