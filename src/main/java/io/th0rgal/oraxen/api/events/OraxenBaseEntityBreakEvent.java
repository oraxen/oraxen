package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Break event for furniture, which also carries the base entity.
 */
public abstract class OraxenBaseEntityBreakEvent<M extends Mechanic> extends OraxenBreakEvent<M> {

    private final Entity baseEntity;

    protected OraxenBaseEntityBreakEvent(@NotNull M mechanic, @NotNull Entity baseEntity, @NotNull Player player,
                                         @Nullable Block block, @NotNull Drop drop) {
        super(mechanic, block, player, drop);
        this.baseEntity = baseEntity;
    }

    /**
     * @return the furniture base entity
     */
    @NotNull
    public Entity getBaseEntity() {
        return baseEntity;
    }
}
