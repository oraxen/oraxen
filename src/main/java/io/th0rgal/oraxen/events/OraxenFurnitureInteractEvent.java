package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureInteractEvent extends Event implements Cancellable {

    private final FurnitureMechanic furnitureMechanic;
    private final Player player;
    private final Block block;
    private boolean isCancelled;
    private final ItemFrame itemFrame;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(FurnitureMechanic mechanic, @Nullable Block block, Player player, ItemFrame itemFrame) {
        this.furnitureMechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
        this.itemFrame = itemFrame;
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

    /**
     * @return The furniture mechanic that was interacted with
     */
    public FurnitureMechanic getFurnitureMechanic() {
        return furnitureMechanic;
    }

    /**
     * @return The player who interacted with the furniture
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was interacted with
     */
    @Nullable
    public Block getBlock() {
        return block;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }
}
