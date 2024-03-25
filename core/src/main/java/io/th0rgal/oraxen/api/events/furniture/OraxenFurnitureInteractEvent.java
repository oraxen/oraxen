package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
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
    private final Entity baseEntity;
    private final Player player;
    @Nullable private final ItemStack itemInHand;
    private final EquipmentSlot hand;
    @Nullable
    private final Location interactionPoint;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Entity baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand) {
        this(mechanic, baseEntity, player, itemInHand, hand, null);
    }

    public OraxenFurnitureInteractEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final Entity baseEntity, @NotNull final Player player, @Nullable final ItemStack itemInHand, @NotNull final EquipmentSlot hand, @Nullable Location interactionPoint) {
        this.player = player;
        this.mechanic = mechanic;
        this.baseEntity = baseEntity;
        this.itemInHand = itemInHand;
        this.hand = hand;
        this.interactionPoint = interactionPoint;
        this.isCancelled = false;
    }

    /**
     * @return The FurnitureMechanic of the furniture that was interacted with
     */
    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with the furniture
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The base entity of the furniture
     */
    @NotNull
    public Entity getBaseEntity() {
        return baseEntity;
    }

    /**
     * @return The Interaction type entity, if supported by the server version
     * @apiNote This will return as a normal Entity, not of type Interaction, or null.
     * This is due to backwards compatibility with old server versions.
     * You can safely cast any entity returned by this method to Interaction.
     */
    @Nullable
    public Entity getInteractionEntity() { return mechanic.getInteractionEntity(baseEntity); }

    /**
     * @return The item in the player's hand when they interacted with the furniture
     */
    @Nullable
    public ItemStack getItemInHand() {
        return itemInHand;
    }

    /**
     * @return The hand the player interacted with the furniture with
     */
    public EquipmentSlot getHand() {
        return hand;
    }

    /**
     * @return The exa
     */
    @Nullable
    public Location getInteractionPoint() {
        return interactionPoint;
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
