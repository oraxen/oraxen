package io.th0rgal.oraxen.api.events.furniture;

import io.th0rgal.oraxen.api.events.OraxenBaseEntityPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OraxenFurniturePlaceEvent extends OraxenBaseEntityPlaceEvent<FurnitureMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenFurniturePlaceEvent(@NotNull FurnitureMechanic mechanic, @NotNull Block block, @NotNull Entity baseEntity,
                                     @NotNull Player player, @NotNull ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, baseEntity, player, itemInHand, hand);
    }

    @NotNull
    @Override
    public ItemStack getItemInHand() {
        return super.getItemInHand();
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
