package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenFurniturePlaceEvent extends Event implements Cancellable {

    private final FurnitureMechanic mechanic;
    private final Player player;
    private final Block block;
    private final Entity baseEntity;
    private final ItemStack item;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurniturePlaceEvent(FurnitureMechanic mechanic, Block block, Entity baseEntity, Player player, ItemStack item){
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
        this.baseEntity = baseEntity;
        this.item = item;
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
     * @return The item used to place the furniture
     */
    public ItemStack getItem() {
        return item;
    }

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
