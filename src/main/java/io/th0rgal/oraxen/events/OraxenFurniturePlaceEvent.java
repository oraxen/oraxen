package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenFurniturePlaceEvent extends Event implements Cancellable {

    FurnitureMechanic furnitureMechanic;

    Player player;

    Block block;

    boolean isCancelled;

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurniturePlaceEvent(FurnitureMechanic mechanic, Block block, Player player){
        this.furnitureMechanic = mechanic;
        this.player = player;
        this.block = block;
        this.isCancelled = false;
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
}