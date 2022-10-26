package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenNoteBlockInteractEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Block block;
    private final Player player;
    private final ItemStack itemInHand;
    private final BlockFace blockFace;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenNoteBlockInteractEvent(NoteBlockMechanic mechanic, Block block, Player player, ItemStack itemInHand, BlockFace blockFace) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.itemInHand = itemInHand;
        this.blockFace = blockFace;
        this.isCancelled = false;
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    public NoteBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with this block
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was interacted with
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return The item in hand when the player interacted with the note block
     */
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return The BlockFace that was clicked
     */
    public BlockFace getBlockFace() {
        return blockFace;
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
