package io.th0rgal.oraxen.mechanics.provided.bigmining;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
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

    public BigMiningMechanicListener(MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockbreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (OraxenPlugin.WorldGuard()) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionContainer container = OraxenPlugin.worldGuardPlugin().getPlatform().getRegionContainer();
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(event.getBlock().getLocation());
            if (player.hasPermission("oraxen.worldguard.bypass") ||
                    player.hasPermission("oraxen.worldguard.*") ||
                    !container.createQuery().testState(loc, localPlayer, Flags.BLOCK_BREAK)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("Your are not allowed to break this block !");
                return;
            }
        }
        if (blocksToProcess > 0) {
            blocksToProcess -= 1;
            return;
        }

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        BigMiningMechanic mechanic = (BigMiningMechanic) factory.getMechanic(itemID);
        List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 5);
        if (lastTwoTargetBlocks.size() < 2)
            return;
        Block nearestBlock = lastTwoTargetBlocks.get(0);
        Block secondBlock = lastTwoTargetBlocks.get(1);
        BlockFace blockFace = secondBlock.getFace(nearestBlock);
        Location secondMinusNearest = secondBlock.getLocation().subtract(nearestBlock.getLocation());
        int modifier = secondMinusNearest.getBlockX() + secondMinusNearest.getBlockY() + secondMinusNearest.getBlockZ();

        Location initialLocation = event.getBlock().getLocation();

        Location tempLocation;
        for (double relativeX = -mechanic.getRadius(); relativeX <= mechanic.getRadius(); relativeX++)
            for (double relativeY = -mechanic.getRadius(); relativeY <= mechanic.getRadius(); relativeY++)
                for (double relativeDepth = 0; relativeDepth < mechanic.getDepth(); relativeDepth++) {
                    tempLocation = transpose(initialLocation, blockFace, relativeX, relativeY, relativeDepth * modifier);
                    if (tempLocation.equals(initialLocation))
                        continue;
                    breakBlock(player, tempLocation.getBlock(), item);
                }
    }

    private void breakBlock(Player player, Block block, ItemStack itemStack) {
        if (block.isLiquid() || block.getType() == Material.BEDROCK || block.getType() == Material.BARRIER)
            return;
        blocksToProcess += 1; // to avoid this method to call itself <- need other way to handle players using the same tool at the same time
        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(blockBreakEvent);
        if (!blockBreakEvent.isCancelled())
            if (blockBreakEvent.isDropItems())
                block.breakNaturally(itemStack);
            else
                block.setType(Material.AIR);
    }

    /*
        It converts a relative location in 2d into another location in 3d on a certain axis
     */
    private Location transpose(Location location, BlockFace blockFace, double relativeX, double relativeY, double relativeDepth) {
        location = location.clone();
        if (blockFace == BlockFace.WEST || blockFace == BlockFace.EAST)  // WEST_EAST axis != X
            location.add(relativeDepth, relativeX, relativeY);
        else if (blockFace == BlockFace.DOWN || blockFace == BlockFace.UP)  // DOWN_UP axis != Y
            location.add(relativeX, relativeDepth, relativeY);
        else  // NORTH_SOUTH axis != Z
            location.add(relativeX, relativeY, relativeDepth);
        return location;
    }
}
