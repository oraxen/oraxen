package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
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
    private final ItemFrame itemFrame;
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * @param mechanic The FurnitureMechanic of this block
     * @param player The player who damaged this block
     * @param block The block that was damaged
     * @param itemFrame The item frame for the damaged furniture
     */
    public OraxenFurnitureDamageEvent(FurnitureMechanic mechanic, Player player, @Nullable Block block, ItemFrame itemFrame) {
        this.mechanic = mechanic;
        this.player = player;
        this.block = block;
        this.itemFrame = itemFrame;
    }

    /**
     * @return The FurnitureMechanic of this Furniture
     */
    public FurnitureMechanic getMechanic() {
        return mechanic;
    }

    /**
     * @return The player that damaged the furniture
     */
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

    /**
     * @return The ItemFrame the furniture is in
     */
    public ItemFrame getItemFrame() {
        return itemFrame;
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
