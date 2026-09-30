package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockDataListener implements Listener {

    private final JavaPlugin plugin;

    public BlockDataListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!BlockHelpers.isPDCDirty(event.getBlock()) && hasData(event.getBlock()))
            BlockHelpers.removePDC(event.getBlock(), plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getTo() != event.getBlock().getType()) remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        remove(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        remove(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        if (event.getBlock().getType() != Material.FIRE
                && event.getNewState().getType() != event.getBlock().getType()) {
            remove(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        removeStates(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        removeStates(event.getBlocks());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        move(event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        move(event.getBlocks(), event);
    }

    private void move(List<Block> blocks, BlockPistonEvent event) {
        List<Block> sources = new ArrayList<>(blocks);
        Collections.reverse(sources);

        for (Block source : sources) {
            if (!hasData(source)) continue;
            // Furniture barriers belong to an entity, so moving only their block data
            // would leave the entity and its other barriers behind.
            if (OraxenFurniture.hasFurnitureBlockMarker(source)) {
                remove(source);
                continue;
            }
            if (source.getPistonMoveReaction() == PistonMoveReaction.BREAK) {
                remove(source);
                continue;
            }

            PersistentDataContainer sourceData = BlockHelpers.getPDC(source, plugin);
            Block destination = source.getRelative(event.getDirection());
            sourceData.copyTo(BlockHelpers.getPDC(destination, plugin), true);
            BlockHelpers.removePDC(source, plugin);
        }
    }

    private void removeStates(List<BlockState> states) {
        for (BlockState state : states) remove(state.getBlock());
    }

    private void remove(List<Block> blocks) {
        for (Block block : blocks) remove(block);
    }

    private void remove(Block block) {
        // Run furniture cleanup while the barrier marker can still resolve its base
        // entity. Removing the PDC first would strand the entity and its other blocks.
        if (OraxenFurniture.hasFurnitureBlockMarker(block))
            OraxenFurniture.remove(block.getLocation(), null);
        else if (OraxenBlocks.getOraxenBlock(block.getLocation()) != null)
            OraxenBlocks.remove(block.getLocation(), null);
        if (hasData(block)) BlockHelpers.removePDC(block, plugin);
    }

    private boolean hasData(Block block) {
        return BlockHelpers.hasPDC(block, plugin);
    }
}
