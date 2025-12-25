package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenChorusBlockBreakEvent extends Event implements Cancellable {

    private final ChorusBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private Drop drop;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockBreakEvent(@NotNull final ChorusBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.drop = mechanic.getDrop();
        this.isCancelled = false;
    }

    /**
     * @return The ChorusBlockMechanic of this block
     */
    @NotNull
    public ChorusBlockMechanic getMechanic() {
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

    /**
     * @return The drop of the block
     */
    @NotNull
    public Drop getDrop() {
        return drop;
    }

    /**
     * Set the drop of the block
     * @param drop the new drop
     */
    public void setDrop(Drop drop) {
        this.drop = drop;
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
