package io.th0rgal.oraxen.api.events.custom_block.stringblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockInteractEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock.StringBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OraxenStringBlockInteractEvent extends OraxenBlockInteractEvent implements Cancellable {

    public OraxenStringBlockInteractEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Player player, @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand, @NotNull Block block, @NotNull BlockFace blockFace) {
        super(mechanic, player, itemInHand, hand, block, blockFace);
    }

    public OraxenStringBlockInteractEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Player player, @Nullable ItemStack itemInHand, @NotNull EquipmentSlot hand, @NotNull Block block, @NotNull BlockFace blockFace, @NotNull Action action) {
        super(mechanic, player, itemInHand, hand, block, blockFace, action);
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    @NotNull
    public StringBlockMechanic getMechanic() {
        return (StringBlockMechanic) super.getMechanic();
    }
}