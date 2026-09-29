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

import java.util.Objects;


public class OraxenFurniturePlaceEvent extends OraxenBaseEntityPlaceEvent<FurnitureMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemStack heldItem;

    public OraxenFurniturePlaceEvent(@NotNull FurnitureMechanic mechanic, @NotNull Block block, @NotNull Entity baseEntity,
                                     @NotNull Player player, @NotNull ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, baseEntity, player, Objects.requireNonNull(itemInHand), hand);
        this.heldItem = itemInHand;
    }

    @NotNull
    @Override
    public ItemStack getItemInHand() {
        return heldItem;
    }

    @NotNull
    @Override
    public FurnitureMechanic getMechanic() {
        return super.getMechanic();
    }

    @NotNull
    @Override
    public Block getBlock() {
        return Objects.requireNonNull(super.getBlock());
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
