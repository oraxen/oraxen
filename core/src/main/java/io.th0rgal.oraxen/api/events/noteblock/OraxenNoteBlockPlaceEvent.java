package io.th0rgal.oraxen.api.events.noteblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenNoteBlockPlaceEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenNoteBlockPlaceEvent(@NotNull final NoteBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player, @NotNull final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.itemInHand = itemInHand;
        this.hand = hand;
        this.isCancelled = false;
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    @NotNull
    public NoteBlockMechanic getMechanic() {
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
     * Gets the item in the player's hand when they placed the furniture.
     *
     * @return The ItemStack for the item in the player's hand when they
     *     placed the furniture
     */
    @NotNull
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * Gets the hand used to place the furniture.
     *
     * @return The EquipmentSlot for the hand used to place the furniture
     */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
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
