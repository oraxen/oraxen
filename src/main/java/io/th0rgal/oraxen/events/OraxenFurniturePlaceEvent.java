package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenFurniturePlaceEvent extends Event implements Cancellable {

    FurnitureMechanic furnitureMechanic;

    Player player;

    Block block;

    ItemFrame itemFrame;

    boolean isCancelled;

    // this event should fire twice, once before the block and itemframe are created, and once after. boolean postCreation is false before the BlockPlaceEvent and true after.
    boolean postCreation;

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurniturePlaceEvent(FurnitureMechanic mechanic, Block block, ItemFrame itemFrame,  Player player, boolean postCreation){
        this.furnitureMechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
        this.postCreation = postCreation;
        this.itemFrame = itemFrame;
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
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public FurnitureMechanic getFurnitureMechanic() {
        return furnitureMechanic;
    }

    public Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return block;
    }

    public ItemFrame getItemFrame() {
        return itemFrame;
    }

    public boolean isPostCreation() {
        return postCreation;
    }
}