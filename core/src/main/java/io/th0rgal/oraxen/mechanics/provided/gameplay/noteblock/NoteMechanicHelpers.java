package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

public class NoteMechanicHelpers {

    // Not exclusively in NoteBlockMechanic as future blocks might want to support this aswell
    public static void handleFallingOraxenBlockAbove(Block block) {
        Block blockAbove = block.getRelative(BlockFace.UP);
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(blockAbove);
        if (mechanic == null || !mechanic.isFalling()) return;
        Location fallingLocation = BlockHelpers.toCenterBlockLocation(blockAbove.getLocation());
        BlockData fallingData = OraxenBlocks.getOraxenBlockData(mechanic.getItemID());
        if (fallingData == null) return;
        OraxenBlocks.remove(blockAbove.getLocation(), null);
        blockAbove.getWorld().spawnFallingBlock(fallingLocation, fallingData);
        handleFallingOraxenBlockAbove(blockAbove);
    }
}
