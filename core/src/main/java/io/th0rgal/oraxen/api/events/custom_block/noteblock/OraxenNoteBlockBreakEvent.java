package io.th0rgal.oraxen.api.events.custom_block.noteblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

public class OraxenNoteBlockBreakEvent extends OraxenBlockBreakEvent implements Cancellable {

    /**
     * @param mechanic The CustomBlockMechanic of this block
     * @param block    The block that was damaged
     * @param player   The player who damaged this block
     */
    public OraxenNoteBlockBreakEvent(@NotNull NoteBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
        super(mechanic, block, player);
    }

    /**
     * @return The NoteBlockMechanic of this block
     */
    @NotNull
    @Override
    public NoteBlockMechanic getMechanic() {
        return (NoteBlockMechanic) super.getMechanic();
    }

}
