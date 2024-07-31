package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureInteractEvent extends Event implements Cancellable {

    private final FurnitureMechanic mechanic;
    private final ItemDisplay baseEntity;
    private final Player player;
    private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    @Nullable
    private final Location interactionPoint;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final ItemDisplay baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this(mechanic, baseEntity, player, itemInHand, hand, null);
    }

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final ItemDisplay baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @Nullable Location interactionPoint) {
        this.player = player;
        this.mechanic = mechanic;
        this.baseEntity = baseEntity;
        this.itemInHand = itemInHand != null ? itemInHand : new ItemStack(Material.AIR);
        this.hand = hand;
        this.interactionPoint = interactionPoint;
        this.isCancelled = false;
    }

    /**
     * @return The FurnitureMechanic of the furniture that was interacted with
     */
    @NotNull
    public FurnitureMechanic mechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with the furniture
     */
    @NotNull
    public Player player() {
        return player;
    }

    /**
     * @return The base entity of the furniture
     */
    @NotNull
    public ItemDisplay baseEntity() {
        return baseEntity;
    }

    /**
     * @return The item in the player's hand when they interacted with the furniture
     */
    public ItemStack itemInHand() {
        return itemInHand;
    }

    /**
     * @return The hand the player interacted with the furniture with
     */
    public EquipmentSlot hand() {
        return hand;
    }


    @Nullable
    public Location interactionPoint() {
        //TODO Support for barrier and interaction hitboxes
        return interactionPoint != null ? interactionPoint : baseEntity.getLocation();
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
