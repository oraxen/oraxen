package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Damage event for furniture, which also carries the base entity.
 */
public abstract class OraxenBaseEntityDamageEvent<M extends Mechanic> extends OraxenDamageEvent<M> {

    private final Entity baseEntity;

    protected OraxenBaseEntityDamageEvent(@NotNull M mechanic, @NotNull Entity baseEntity, @NotNull Player player,
                                          @Nullable Block block) {
        super(mechanic, block, player);
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
