package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenFurnitureInteractEvent  extends Event implements Cancellable {



    FurnitureMechanic furnitureMechanic;
    Player player;
    Block block;
    boolean isCancelled;
    private static final HandlerList HANDLERS  = new HandlerList();

    public OraxenFurnitureInteractEvent(FurnitureMechanic mechanic, Block block, Player player) {
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

    /**
     * @return The note block mechanic
     */
    public FurnitureMechanic getFurnitureMechanic() {
        return furnitureMechanic;
    }

    /**
     * @return The player who broke the note block
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block that was broken
     */
    public Block getBlock() {
        return block;
    }
}
