package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.custom_block.OraxenCustomBlockDropLootEvent;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.drops.DroppedLoot;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StringBlockMechanicPhysicsListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void tripwireEvent(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (event.getChangedType() != Material.TRIPWIRE) return;
        if (event.getSourceBlock() == event.getBlock()) return;
        event.setCancelled(true);

        for (BlockFace f : BlockFace.values()) {
            if (!f.isCartesian() || f.getModY() != 0 || f == BlockFace.SELF) continue; // Only take N/S/W/E
            final Block changed = block.getRelative(f);
            if (changed.getType() != Material.TRIPWIRE) continue;

            final BlockData data = changed.getBlockData().clone();
            OraxenPlugin.get().getScheduler().runRegionTaskLater(changed.getLocation(), () ->
                    changed.setBlockData(data, false), 1L);
        }

        // Stores the pre-change blockdata and applies it on next tick to prevent the block from updating
        final BlockData blockData = block.getBlockData().clone();
        OraxenPlugin.get().getScheduler().runRegionTaskLater(block.getLocation(), () -> {
            if (block.getType().isAir()) return;
            block.setBlockData(blockData, false);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        List<Block> tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

        for (Block block : tripwireList) {
            final StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            if (mechanic == null) return;

            block.setType(Material.AIR, false);

            mechanic.breakable().drop().spawns(block.getLocation(), new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Player player = event.getPlayer();

        for (BlockFace face : BlockFace.values()) {
            if (face == BlockFace.SELF && !face.isCartesian()) continue;
            if (block.getType() == Material.TRIPWIRE || block.getType() == Material.NOTE_BLOCK) break;
            if (OraxenFurniture.isFurniture(block.getLocation())) break;
            if (block.getRelative(face).getType() == Material.TRIPWIRE) {
                if (player.getGameMode() != GameMode.CREATIVE) block.breakNaturally(player.getInventory().getItemInMainHand(), true);
                else block.setType(Material.AIR);
                if (BlockHelpers.isReplaceable(blockAbove.getType())) blockAbove.breakNaturally(true);
                OraxenPlugin.get().getScheduler().runRegionTaskLater(block.getLocation(), () ->
                        StringMechanicHelpers.fixClientsideUpdate(block.getLocation()), 1);
            }
        }
    }
}
