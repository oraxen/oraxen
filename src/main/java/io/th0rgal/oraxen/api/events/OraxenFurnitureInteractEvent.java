package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureInteractEvent extends Event implements Cancellable {

    private final FurnitureMechanic mechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private final Entity baseEntity;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(FurnitureMechanic mechanic, Player player, @Nullable Block block, Entity baseEntity) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
        this.baseEntity = baseEntity;
    }

    /**
     * @return The FurnitureMechanic of the furniture that was interacted with
     */
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player who interacted with the furniture
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was interacted with, null if the furniture has no hitbox
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    public Entity getBaseEntity() {
        return baseEntity;
    }

    public Interaction getInteractionEntity() { return mechanic.getInteractionEntity(baseEntity); }

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
