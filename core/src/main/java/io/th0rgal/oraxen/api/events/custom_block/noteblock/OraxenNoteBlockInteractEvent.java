package io.th0rgal.oraxen.api.events.custom_block.noteblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenNoteBlockInteractEvent extends OraxenBlockInteractEvent implements Cancellable {

    public OraxenNoteBlockInteractEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Player player, @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand, @NotNull Block block, @NotNull BlockFace blockFace) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
    }

    public OraxenNoteBlockInteractEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Player player, @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand, @NotNull Block block, @NotNull BlockFace blockFace, @NotNull Action action) {
        super(mechanic, player, itemInHand, hand, block, blockFace, action);
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    @NotNull
    public NoteBlockMechanic getMechanic() {
        return (NoteBlockMechanic) super.getMechanic();
    }

}
