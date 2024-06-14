package io.th0rgal.oraxen.api.events.custom_block.noteblock;

import io.th0rgal.oraxen.api.events.custom_block.OraxenCustomBlockDropLootEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.utils.drops.DroppedLoot;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OraxenNoteBlockDropLootEvent extends OraxenCustomBlockDropLootEvent {
    public OraxenNoteBlockDropLootEvent(@NotNull NoteBlockMechanic mechanic, @NotNull Block block, @NotNull Player player, @NotNull List<DroppedLoot> loots) {
        super(mechanic, block, player, loots);
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
