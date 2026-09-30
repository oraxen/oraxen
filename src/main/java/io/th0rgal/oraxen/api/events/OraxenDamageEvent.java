package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A player damaged a custom block or furniture.
 */
public abstract class OraxenDamageEvent<M extends Mechanic> extends OraxenMechanicEvent<M> {

    protected OraxenDamageEvent(@NotNull M mechanic, @Nullable Block block, @NotNull Player player) {
        super(mechanic, block, player);
    }
}
