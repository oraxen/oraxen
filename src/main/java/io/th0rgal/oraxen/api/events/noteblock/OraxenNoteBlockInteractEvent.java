package io.th0rgal.oraxen.api.events.noteblock;

import io.th0rgal.oraxen.api.events.OraxenInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenNoteBlockInteractEvent extends OraxenInteractEvent<NoteBlockMechanic> {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Action action;

    @Deprecated
    public OraxenNoteBlockInteractEvent(@NotNull NoteBlockMechanic mechanic, @NotNull Player player,
                                        @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand,
                                        @NotNull Block block, @NotNull BlockFace blockFace) {
        this(mechanic, player, itemInHand, hand, block, blockFace, Action.RIGHT_CLICK_BLOCK);
    }

    public OraxenNoteBlockInteractEvent(@NotNull NoteBlockMechanic mechanic, @NotNull Player player,
                                        @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand,
                                        @NotNull Block block, @NotNull BlockFace blockFace, @NotNull Action action) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
        this.action = action;
    }

    /**
     * @return the type of interaction
     */
    @NotNull
    public Action getAction() {
        return action;
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
