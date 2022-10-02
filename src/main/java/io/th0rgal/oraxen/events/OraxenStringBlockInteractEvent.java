package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenStringBlockInteractEvent extends Event implements Cancellable {

    StringBlockMechanic stringBlockMechanic;
    Player player;
    Block block;
    ItemStack itemInHand;
    boolean isCancelled;
    BlockFace blockFace;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockInteractEvent(StringBlockMechanic mechanic, Block block, ItemStack itemInHand, Player player, BlockFace blockFace) {
        this.stringBlockMechanic = mechanic;
        this.itemInHand = itemInHand;
        this.block = block;
        this.player = player;
        this.isCancelled = false;
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

    /**
     * @return The string block mechanic
     */
    public StringBlockMechanic getStringBlockMechanic() {
        return stringBlockMechanic;
    }

    /**
     * @return The player who interacted with the string block
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
     * @return The item in hand when the player interacted with the string block
     */
    public ItemStack getItemInHand() {
        return itemInHand;
    }


    /**
     * @return Clicked block face
     */
    public BlockFace getBlockFace() {
        return blockFace;
    }

}
