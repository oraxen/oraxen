package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.world.GenericGameEvent;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class NoteBlockMechanicPhysicsListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        final Block block = event.getBlock();
        final Block aboveBlock = block.getRelative(BlockFace.UP);
        final Block belowBlock = block.getRelative(BlockFace.DOWN);
        // If block below is NoteBlock, it will be affected by the break
        // Call updateAndCheck from it to fix vertical stack of NoteBlocks
        // if belowBlock is not a NoteBlock we must ensure the above is not, if it is call updateAndCheck from block
        if (belowBlock.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(belowBlock);
        } else if (aboveBlock.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(aboveBlock);
        }
        if (block.getType() == Material.NOTE_BLOCK) {
            event.setCancelled(true);
            updateAndCheck(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNoteblockPowered(final GenericGameEvent event) {
        Block block = event.getLocation().getBlock();
        Location eLoc = block.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        if (event.getEvent() != GameEvent.NOTE_BLOCK_PLAY) return;
        if (block.getType() != Material.NOTE_BLOCK) return;
        NoteBlock data = (NoteBlock) block.getBlockData().clone();
        OraxenPlugin.get().getScheduler().runRegionTaskLater(block.getLocation(), () -> block.setBlockData(data, false), 1L);
    }

    public void updateAndCheck(Block block) {
        final Block blockAbove = block.getRelative(BlockFace.UP);
        if (blockAbove.getType() == Material.NOTE_BLOCK)
            blockAbove.getState().update(true, true);
        Block nextBlock = blockAbove.getRelative(BlockFace.UP);
        if (nextBlock.getType() == Material.NOTE_BLOCK) updateAndCheck(blockAbove);
    }
}
