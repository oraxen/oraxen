package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.MushroomStemMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenMushroomStemBreakEvent extends Event implements Cancellable {

    MushroomStemMechanic mushroomStemMechanic;
    Player player;
    Block block;
    boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenMushroomStemBreakEvent(MushroomStemMechanic mechanic, Block block, Player player) {
        this.mushroomStemMechanic = mechanic;
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
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * @return The note block mechanic
     */
    public MushroomStemMechanic getNoteBlockMechanic() {
        return mushroomStemMechanic;
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
