package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockMechanicsListener implements Listener {

    private MechanicFactory factory;

    public BlockMechanicsListener(BlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBreakingCustomBlock(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.MUSHROOM_STEM)
            return;

        MultipleFacing blockFacing = (MultipleFacing)event.getBlock().getBlockData();
        //Logs.log("code:" + Utils.getCode(blockFacing));


    }

    //todo: improve performances
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlacingMushroomBlock(BlockPlaceEvent event) {

        if (event.getBlockPlaced().getType() != Material.MUSHROOM_STEM
                || OraxenItems.isAnItem(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        Set<Block> browseStartingPoint = new HashSet<>();
        Block block = event.getBlock();
        BlockData blockData = block.getBlockData();
        Utils.setBlockFacing((MultipleFacing) blockData, 15);
        block.setBlockData(blockData);
        browseStartingPoint.add(block);
        Map<Block, BlockData> blocksToFix = browse(browseStartingPoint, new HashMap<>());

        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> {
            for (Map.Entry<Block, BlockData> blockDataEntry : blocksToFix.entrySet())
                blockDataEntry.getKey().setBlockData(blockDataEntry.getValue());
        }, 0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPrePlacingCustomBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        Player player = event.getPlayer();
        Block placedAgainst = event.getClickedBlock();
        Block target;
        Material type = placedAgainst.getType();
        if (type.equals(Material.SNOW)
                || type.equals(Material.VINE)
                || type.equals(Material.GRASS)
                || type.equals(Material.TALL_GRASS)
                || type.equals(Material.SEAGRASS))
            target = placedAgainst;
        else
            target = placedAgainst.getRelative(event.getBlockFace());

        if (target.getLocation().distance(player.getLocation()) > 1 && target.getLocation().distance(player.getLocation()) > 1) {
            BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, event.getHand());
            Bukkit.getPluginManager().callEvent(blockPlaceEvent);
            if (blockPlaceEvent.canBuild() && !blockPlaceEvent.isCancelled()) {
                event.setCancelled(true);
                MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
                int customVariation = ((BlockMechanic) factory.getMechanic(itemID)).getCustomVariation();
                Utils.setBlockFacing(newBlockData, customVariation);

                Set<Block> browseStartingPoint = new HashSet<>();
                browseStartingPoint.add(target);
                Map<Block, BlockData> blocksToFix = browse(browseStartingPoint, new HashMap<>());

                target.setBlockData(newBlockData);

                for (Map.Entry<Block, BlockData> blockDataEntry : blocksToFix.entrySet())
                    blockDataEntry.getKey().setBlockData(blockDataEntry.getValue());

                if (!player.getGameMode().equals(GameMode.CREATIVE))
                    item.setAmount(item.getAmount() - 1);
            }
        }
    }

    public Map<Block, BlockData> browse(Set<Block> input, Map<Block, BlockData> output) {

        BlockFace[] adjacents = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
                BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST};

        Set<Block> nextInput = new HashSet<>();

        for (Block inputBlock : input) {
            for (BlockFace adjacentBlockFace : adjacents) {
                Block adjacentBlock = inputBlock.getRelative(adjacentBlockFace);
                if (adjacentBlock.getType() == Material.MUSHROOM_STEM
                        && !output.containsKey(adjacentBlock))
                    nextInput.add(adjacentBlock);
            }
        }
        //todo: improve logic
        for (Block block : nextInput)
            output.put(block, block.getBlockData());

        if (nextInput.isEmpty())
            return output;
        else
            return browse(nextInput, output);

    }

}