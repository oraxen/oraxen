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
public abstract class OraxenBreakEvent<M extends Mechanic> extends OraxenMechanicEvent<M> {

    private Drop drop;

    protected OraxenBreakEvent(@NotNull M mechanic, @Nullable Block block, @NotNull Player player, @NotNull Drop drop) {
        super(mechanic, block, player);
        this.drop = drop;
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
