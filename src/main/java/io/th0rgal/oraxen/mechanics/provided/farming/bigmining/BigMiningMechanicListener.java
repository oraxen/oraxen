package io.th0rgal.oraxen.mechanics.provided.farming.bigmining;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Constants;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BigMiningMechanicListener implements Listener {

    private final MechanicFactory factory;
    private int blocksToProcess = 0;

    public BigMiningMechanicListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final ItemStack item = player.getInventory().getItemInMainHand();

        if (blocksToProcess > 0) {
            blocksToProcess -= 1;
            return;
        }

        final List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 5);
        final BigMiningMechanic mechanic = (BigMiningMechanic) factory.getMechanic(item);
        if (mechanic == null || lastTwoTargetBlocks.size() < 2) return;

        final Block nearestBlock = lastTwoTargetBlocks.get(0);
        final Block secondBlock = lastTwoTargetBlocks.get(1);
        final BlockFace blockFace = secondBlock.getFace(nearestBlock);
        final Location secondMinusNearest = secondBlock.getLocation().subtract(nearestBlock.getLocation());
        final int modifier = secondMinusNearest.getBlockX() + secondMinusNearest.getBlockY() + secondMinusNearest.getBlockZ();

        final Location initialLocation = event.getBlock().getLocation();

        Location tempLocation;
        for (double relativeX = -mechanic.getRadius(); relativeX <= mechanic.getRadius(); relativeX++)
            for (double relativeY = -mechanic.getRadius(); relativeY <= mechanic.getRadius(); relativeY++)
                for (double relativeDepth = 0; relativeDepth < mechanic.getDepth(); relativeDepth++) {
                    tempLocation = transpose(initialLocation, blockFace, relativeX, relativeY,
                            relativeDepth * modifier);
                    if (tempLocation.equals(initialLocation))
                        continue;
                    breakBlock(player, tempLocation.getBlock(), item);
                }
    }

    private void breakBlock(final Player player, final Block block, final ItemStack itemStack) {
        if (block.isLiquid()
                || Constants.UNBREAKABLE_BLOCKS.contains(block.getType())
                || !ProtectionLib.canBreak(player, block.getLocation()))
            return;
        blocksToProcess += 1; // to avoid this method to call itself <- need other way to handle players using
        // the same tool at the same time
        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(blockBreakEvent);
        if (!blockBreakEvent.isCancelled()) {
            if (blockBreakEvent.isDropItems())
                block.breakNaturally(itemStack);
            else block.setType(Material.AIR);
        }
    }

    /*
     * It converts a relative location in 2d into another location in 3d on a
     * certain axis
     */
    private Location transpose(Location loc, final BlockFace face, final double relX, final double relY,
                               final double relativeDepth) {
        loc = loc.clone();
        return switch (face) {
            case WEST, EAST ->  loc.add(relativeDepth, relX, relY);
            case UP, DOWN -> loc.add(relX, relativeDepth, relY);
            default -> loc.add(relX, relY, relativeDepth);
        };
    }
}
