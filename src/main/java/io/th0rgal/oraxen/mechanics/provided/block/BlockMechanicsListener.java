package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockMechanicsListener implements Listener {

    private MechanicFactory factory;

    public BlockMechanicsListener(BlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onPlacingCustomBlock(PlayerInteractEvent event) {
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
        if (!type.equals(Material.SNOW)
                && !type.equals(Material.GRASS_BLOCK)
                && !type.equals(Material.VINE)
                && !type.equals(Material.TALL_GRASS))
            target = placedAgainst.getRelative(event.getBlockFace());
        else
            target = placedAgainst;

        BlockPlaceEvent blockBreakEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, event.getHand());
        Bukkit.getPluginManager().callEvent(blockBreakEvent);

        if (target.getLocation().distance(player.getLocation()) > 1 && target.getLocation().distance(player.getLocation()) > 1) {
            if (blockBreakEvent.canBuild() && !blockBreakEvent.isCancelled()) {

                event.setCancelled(true);
                MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
                Logs.log("face" + newBlockData.getFaces());
                newBlockData.setFace(BlockFace.UP, false);
                newBlockData.setFace(BlockFace.DOWN, false);
                newBlockData.setFace(BlockFace.NORTH, false);
                newBlockData.setFace(BlockFace.SOUTH, false);
                newBlockData.setFace(BlockFace.WEST, false);
                newBlockData.setFace(BlockFace.EAST, true);
                target.setBlockData(newBlockData);
                if (!player.getGameMode().equals(GameMode.CREATIVE))
                    item.setAmount(item.getAmount() - 1);
            }
        }
    }

}