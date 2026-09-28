package io.th0rgal.oraxen.api.events.noteblock;

import io.th0rgal.oraxen.api.events.OraxenPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OraxenNoteBlockPlaceEvent extends OraxenPlaceEvent<NoteBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemStack heldItem;

    public OraxenNoteBlockPlaceEvent(@NotNull NoteBlockMechanic mechanic, @NotNull Block block, @NotNull Player player,
                                     @NotNull ItemStack itemInHand, @NotNull EquipmentSlot hand) {
        super(mechanic, block, player, Objects.requireNonNull(itemInHand), hand);
        this.heldItem = itemInHand;
    }

    @NotNull
    @Override
    public ItemStack getItemInHand() {
        return heldItem;
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
