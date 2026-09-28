package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player broke a custom block or furniture. The drop starts as the mechanic
 * default and can be replaced by a listener.
 */
public abstract class OraxenBreakEvent<M extends Mechanic> extends OraxenDamageEvent<M> {

    private Drop drop;

    protected OraxenBreakEvent(@NotNull M mechanic, @Nullable Block block, @NotNull Player player, @NotNull Drop drop) {
        super(mechanic, block, player);
        this.drop = drop;
    }

    /**
     * @return the mechanic that was broken
     */
    @NotNull
    @Override
    public M getMechanic() {
        return super.getMechanic();
    }

    /**
     * @return the player who broke it
     */
    @NotNull
    @Override
    public Player getPlayer() {
        return super.getPlayer();
    }

    /**
     * @return the block that was broken, or null when the furniture has no block
     */
    @Nullable
    @Override
    public Block getBlock() {
        return super.getBlock();
    }

    /**
     * @return the drop, initially the mechanic default
     */
    @NotNull
    public Drop getDrop() {
        return drop;
    }

    /**
     * @param drop the drop to use instead of the mechanic default
     */
    public void setDrop(@NotNull Drop drop) {
        this.drop = drop;
    }
}
