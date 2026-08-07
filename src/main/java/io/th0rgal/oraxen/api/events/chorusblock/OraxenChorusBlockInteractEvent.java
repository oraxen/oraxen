package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenChorusBlockInteractEvent extends Event implements Cancellable {

    private final ChorusBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private final BlockFace blockFace;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockInteractEvent(@NotNull final ChorusBlockMechanic mechanic, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @NotNull final Block block, @NotNull final BlockFace blockFace) {
        this.mechanic = mechanic;
        this.itemInHand = itemInHand;
        this.block = block;
        this.player = player;
        this.isCancelled = false;
        this.hand = hand;
        this.blockFace = blockFace;
    }

    /**
     * @return The ChorusBlockMechanic of this block
     */
    @NotNull
    public ChorusBlockMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with this ChorusBlock
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The ChorusBlock that was interacted with
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    /**
     * @return Clicked block face
     */
    @NotNull
    public BlockFace getBlockFace() {
        return blockFace;
    }

    /**
     * @return The item in hand when the player interacted with the chorus block
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
