package io.th0rgal.oraxen.mechanics.provided.bigmining;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BigMiningMechanicsListener implements Listener {

    private final MechanicFactory factory;
    private int blocksToProcess = 0;

    public BigMiningMechanicsListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockbreak(BlockBreakEvent event) {

        if (blocksToProcess > 0) {
            blocksToProcess -= 1;
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item == null)
            return;

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        BigMiningMechanic mechanic = (BigMiningMechanic)factory.getMechanic(itemID);

        Player player = event.getPlayer();
        List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 5);
        BlockFace blockFace = lastTwoTargetBlocks.get(1).getFace(lastTwoTargetBlocks.get(0));

        for (double relativeX = -mechanic.getRadius(); relativeX <= mechanic.getRadius(); relativeX++)
            for (double relativeY = -mechanic.getRadius(); relativeY <= mechanic.getRadius(); relativeY++) {
                if (relativeX == 0 && relativeY == 0)
                    continue;
                Location location = event.getBlock().getLocation();
                transpose(location, blockFace, relativeX, relativeY);
                blocksToProcess += 1; // to avoir this method to call itself
                Block block = location.getBlock();
                BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
                Bukkit.getPluginManager().callEvent(blockBreakEvent);
                if (!blockBreakEvent.isCancelled())
                    block.breakNaturally();
            }
    }

    /*
        It converts a relative location in 2d into another location in 3d on a certain axis
     */
    private void transpose(Location location, BlockFace blockFace, double relative_x, double relative_y) {
        if (blockFace == BlockFace.WEST || blockFace == BlockFace.EAST)  // WEST_EAST axis != X
            location.add(0, relative_x, relative_y);
        else if (blockFace == BlockFace.DOWN || blockFace == BlockFace.UP)  // DOWN_UP axis != Y
            location.add(relative_x, 0, relative_y);
        else  // NORTH_SOUTH axis != Z
            location.add(relative_x, relative_y, 0);
    }
}
