package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.api.events.OraxenBaseEntityInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenFurnitureInteractEvent extends OraxenBaseEntityInteractEvent<FurnitureMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurnitureInteractEvent(@NotNull FurnitureMechanic mechanic, @NotNull Entity baseEntity, @NotNull Player player,
                                        @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        this(mechanic, baseEntity, player, itemInHand, hand, null, null);
    }

    public OraxenFurnitureInteractEvent(@NotNull FurnitureMechanic mechanic, @NotNull Entity baseEntity, @NotNull Player player,
                                        @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand,
                                        @Nullable Block block, @Nullable BlockFace blockFace) {
        super(mechanic, baseEntity, player, itemInHand, hand, block, blockFace);
    }

    /**
     * @return the interaction entity, if this server version supports it, otherwise null.
     * The value is a normal {@link Entity} so older servers can load the class. Callers can cast it to Interaction.
     */
    @Nullable
    public Entity getInteractionEntity() {
        return getMechanic().getInteractionEntity(getBaseEntity());
    }

    @NotNull
    @Override
    public FurnitureMechanic getMechanic() {
        return super.getMechanic();
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
