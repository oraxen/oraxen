package io.th0rgal.oraxen.api.events;

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

    public OraxenFurniturePlaceEvent(FurnitureMechanic mechanic, Block block, Entity baseEntity, Player player, ItemStack itemInHand, EquipmentSlot hand) {
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
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who placed this furniture
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block this furniture was placed at
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return The item frame for this furniture
     */
    public Entity getBaseEntity() {
        return baseEntity;
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

    /**
     * Gets the hand used to place the furniture.
     *
     * @return The hand used to place the furniture
     */
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
    public HandlerList getHandlers() { return getHandlerList(); }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
