package io.th0rgal.oraxen.api.events.custom_block.noteblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.CustomBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired right before a player damages a NoteBlock.
 * If cancelled, the block will not be damaged.
 * @see NoteBlockMechanic
 */
public class OraxenNoteBlockDamageEvent extends OraxenBlockDamageEvent implements Cancellable {


    /**
     * @param mechanic The CustomBlockMechanic of this block
     * @param block    The block that was damaged
     * @param player   The player who damaged this block
     */
    public OraxenNoteBlockDamageEvent(@NotNull CustomBlockMechanic mechanic, @NotNull Block block, @NotNull Player player) {
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
