package io.th0rgal.oraxen.events;

import io.th0rgal.oraxen.mechanics.provided.gameplay.mushroomstem.MushroomStemMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenMushroomStemInteractEvent extends Event implements Cancellable {

    MushroomStemMechanic mushroomStemMechanic;
    Player player;
    Block block;
    ItemStack itemInHand;
    boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenMushroomStemInteractEvent(MushroomStemMechanic mechanic, Block block, ItemStack itemInHand, Player player) {
        this.mushroomStemMechanic = mechanic;
        this.itemInHand = itemInHand;
        this.block = block;
        this.player = player;
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
     * @return The player who interacted with the note block
     */
    public Player getPlayer() {
        return player;
    }

    /*
     * @return The block that was interacted with
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return The item in hand when the player interacted with the note block
     */
    public ItemStack getItemInHand() {
        return itemInHand;
    }

}
