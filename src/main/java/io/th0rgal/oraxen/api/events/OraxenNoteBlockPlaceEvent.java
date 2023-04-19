package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenNoteBlockPlaceEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private final ItemStack itemInHand;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenNoteBlockPlaceEvent(NoteBlockMechanic mechanic, Block block, Player player, ItemStack itemInHand) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.itemInHand = itemInHand;
        this.isCancelled = false;
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    public NoteBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who broke this block
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was broken
     */
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the item in the player's hand when they placed the furniture.
     *
     * @return The ItemStack for the item in the player's hand when they
     *     placed the furniture
     */
    public ItemStack getItemInHand() {
        return itemInHand;
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
