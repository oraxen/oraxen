package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenFurniturePlaceEvent extends Event implements Cancellable {

    private final FurnitureMechanic mechanic;
    private final Player player;
    private final Block block;
    private final Entity baseEntity;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurniturePlaceEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Block block, @NotNull final Entity baseEntity, @NotNull final Player player, @NotNull final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
        this.baseEntity = baseEntity;
        this.itemInHand = itemInHand;
        this.hand = hand;
    }

    /**
     * @return The FurnitureMechanic of this furniture
     */
    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who placed this furniture
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block this furniture was placed at
     */
    @NotNull
    public Block getBlock() {
        return block;
    }

    /**
     * @return The item frame for this furniture
     */
    @NotNull
    public Entity getBaseEntity() {
        return baseEntity;
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
     * @return The hand used to place the furniture
     */
    @NotNull
    public EquipmentSlot getHand() { return hand; }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
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
