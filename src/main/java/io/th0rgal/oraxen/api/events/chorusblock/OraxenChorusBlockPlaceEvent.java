package io.th0rgal.oraxen.api.events.chorusblock;

import io.th0rgal.oraxen.api.events.OraxenPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenChorusBlockPlaceEvent extends OraxenPlaceEvent<ChorusBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenChorusBlockPlaceEvent(@NotNull ChorusBlockMechanic mechanic, @NotNull Block block, @NotNull Player player,
                                       @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player, itemInHand, hand);
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
