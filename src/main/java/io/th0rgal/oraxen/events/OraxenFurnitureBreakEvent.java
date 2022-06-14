package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureBreakEvent extends Event implements Cancellable {
    private boolean isCancelled;
    private final Block block;
    private final FurnitureMechanic furnitureMechanic;
    private final Player player;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureBreakEvent(FurnitureMechanic furnitureMechanic, Player player, Block block) {
        this.block = block;
        this.furnitureMechanic = furnitureMechanic;
        this.player = player;
    }

    public OraxenFurnitureBreakEvent(FurnitureMechanic furnitureMechanic, Player player) {
        this.furnitureMechanic = furnitureMechanic;
        this.player = player;
        this.block = null;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }


    public FurnitureMechanic getFurnitureMechanic() {
        return furnitureMechanic;
    }

    /**
     * @return player that broke the furniture
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the block that was broken.
     * In case of breaking a farming block this will be null, since it utilizes ItemFrames (entity)
     * @return block that was broken or null if it was an entity
     */
    @Nullable
    public Block getBlock() {
        return block;
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
