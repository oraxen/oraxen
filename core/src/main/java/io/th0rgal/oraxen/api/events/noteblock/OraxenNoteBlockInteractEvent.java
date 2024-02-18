package io.th0rgal.oraxen.api.events.noteblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenNoteBlockInteractEvent extends Event implements Cancellable {

    private final NoteBlockMechanic mechanic;
    private final Block block;
    private final Player player;
    private final ItemStack itemInHand;
    private final BlockFace blockFace;
    private final EquipmentSlot hand;
    private final Action action;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    @Deprecated
    public OraxenNoteBlockInteractEvent(@NotNull final NoteBlockMechanic mechanic, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @NotNull final Block block, @NotNull final BlockFace blockFace) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.itemInHand = itemInHand;
        this.blockFace = blockFace;
        this.hand = hand;
        this.action = Action.RIGHT_CLICK_BLOCK;
        this.isCancelled = false;
    }

    public OraxenNoteBlockInteractEvent(@NotNull final NoteBlockMechanic mechanic, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @NotNull final Block block, @NotNull final BlockFace blockFace, @NotNull final Action action) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.itemInHand = itemInHand;
        this.blockFace = blockFace;
        this.hand = hand;
        this.action = action;
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
     * @return The player who interacted with this block
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was interacted with
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    /**
     * @return The BlockFace that was clicked
     */
    @NotNull
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * @return The item in hand when the player interacted with the note block
     */
    @Nullable
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return The hand used to perform interaction
     */
    @NotNull
    public EquipmentSlot getHand() {
        return hand;
    }

    /**
     * @return The type of interaction
     */
    @NotNull
    public Action getAction() {
        return action;
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
