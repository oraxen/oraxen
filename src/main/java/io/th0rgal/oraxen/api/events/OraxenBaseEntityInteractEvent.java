package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interact event for furniture, which also carries the base entity.
 */
public abstract class OraxenBaseEntityInteractEvent<M extends Mechanic> extends OraxenInteractEvent<M> {

    private final Entity baseEntity;

    protected OraxenBaseEntityInteractEvent(@NotNull M mechanic, @NotNull Entity baseEntity, @NotNull Player player,
                                            @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand,
                                            @Nullable Block block, @Nullable BlockFace blockFace) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
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
