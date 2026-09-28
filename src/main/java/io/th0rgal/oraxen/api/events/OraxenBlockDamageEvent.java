package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Damage event for a custom block, which always has a block position.
 */
public abstract class OraxenBlockDamageEvent<M extends Mechanic> extends OraxenDamageEvent<M> {

    protected OraxenBlockDamageEvent(@NotNull M mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player);
    }

    @NotNull
    @Override
    public Block getBlock() {
        return super.getBlock();
    }
}
