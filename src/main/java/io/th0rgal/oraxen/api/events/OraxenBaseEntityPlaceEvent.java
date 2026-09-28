package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Place event for furniture, which also carries the base entity.
 */
public abstract class OraxenBaseEntityPlaceEvent<M extends Mechanic> extends OraxenPlaceEvent<M> {

    private final Entity baseEntity;

    protected OraxenBaseEntityPlaceEvent(@NotNull M mechanic, @NotNull Block block, @NotNull Entity baseEntity,
                                         @NotNull Player player, @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player, itemInHand, hand);
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
