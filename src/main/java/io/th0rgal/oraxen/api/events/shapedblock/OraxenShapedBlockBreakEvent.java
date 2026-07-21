package io.th0rgal.oraxen.api.events.shapedblock;

import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenShapedBlockBreakEvent extends Event implements Cancellable {

    private final ShapedBlockMechanic mechanic;
    private final Player player;
    private final Block block;
    private Drop drop;
    private boolean isCancelled;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenShapedBlockBreakEvent(@NotNull final ShapedBlockMechanic mechanic, @NotNull final Block block, @NotNull final Player player) {
        this.mechanic = mechanic;
        this.block = block;
        this.player = player;
        this.drop = mechanic.getDrop(player.getInventory().getItemInMainHand());
        this.isCancelled = false;
    }

    @NotNull
    public ShapedBlockMechanic getMechanic() {
        return mechanic;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @NotNull
    public Drop getDrop() {
        return drop;
    }

    public void setDrop(Drop drop) {
        this.drop = drop;
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
}
