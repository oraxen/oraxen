package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenChorusBlockPlaceEvent extends Event implements Cancellable {

    private final ChorusBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockPlaceEvent(@NotNull final ChorusBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.itemInHand = itemInHand;
        this.hand = hand;
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
     * @return The player who placed this block
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was placed
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    /**
     * Gets the item in the player's hand when they placed the block.
     *
     * @return The ItemStack for the item in the player's hand when they
     *     placed the block
     */
    @Nullable
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * Gets the hand which the item was placed with.
     *
     * @return The hand used to place the item
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
