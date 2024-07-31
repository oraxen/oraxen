package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired right before a player damages the Furniture.
 * If cancelled, the block will not be damaged.
 * @see FurnitureMechanic
 */
public class OraxenFurnitureDamageEvent extends Event implements Cancellable {

    boolean isCancelled;
    private final Block block;
    private final FurnitureMechanic mechanic;
    private final Player player;
    private final ItemDisplay baseEntity;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @param mechanic The FurnitureMechanic of this block
     * @param baseEntity The base-entity for the damaged furniture
     * @param player The player who damaged this block
     * @param block The block that was damaged
     */
    public OraxenFurnitureDamageEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final ItemDisplay baseEntity, @NotNull final Player player, @Nullable final Block block) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.baseEntity = baseEntity;
    }

    public OraxenFurnitureDamageEvent(@NotNull final FurnitureMechanic mechanic, @NotNull final ItemDisplay baseEntity, @NotNull final Player player) {
        this(mechanic, baseEntity, player, null);
    }

    /**
     * @return The FurnitureMechanic of this Furniture
     */
    @NotNull
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player that damaged the furniture
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the block that was damaged.
     * @return block that was broken or null if it was an entity
     */
    @Nullable
    public Block getBlock() {
        return block;
    }


    @NotNull
    public ItemDisplay getBaseEntity() {
        return baseEntity;
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
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
