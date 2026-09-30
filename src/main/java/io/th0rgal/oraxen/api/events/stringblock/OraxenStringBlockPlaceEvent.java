package io.th0rgal.oraxen.api.events.stringblock;

import io.th0rgal.oraxen.api.events.OraxenPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OraxenStringBlockPlaceEvent extends OraxenPlaceEvent<StringBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenStringBlockPlaceEvent(@NotNull StringBlockMechanic mechanic, @NotNull Block block, @NotNull Player player,
                                       @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player, itemInHand, hand);
    }

    @NotNull
    @Override
    public StringBlockMechanic getMechanic() {
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
