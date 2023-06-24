package io.th0rgal.oraxen.api.events.noteblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired right before a player damages a NoteBlock.
 * If cancelled, the block will not be damaged.
 * @see NoteBlockMechanic
 */
public class OraxenNoteBlockDamageEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @param mechanic The NoteBlockMechanic of this block
     * @param block The block that was damaged
     * @param player The player who damaged this block
     */
    public OraxenNoteBlockDamageEvent(@NotNull final NoteBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
    }

    /**
     * @return The note block mechanic
     */
    @NotNull
    public NoteBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who broke the note block
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
