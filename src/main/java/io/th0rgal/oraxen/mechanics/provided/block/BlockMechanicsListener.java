package io.th0rgal.oraxen.mechanics.provided.block;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class BlockMechanicsListener implements Listener {

    private final MechanicFactory factory;

    public BlockMechanicsListener(BlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMushroomPhysics(BlockPhysicsEvent event) {
        if (event.getChangedType() == Material.MUSHROOM_STEM) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreakingCustomBlock(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.MUSHROOM_STEM)
            return;

        MultipleFacing blockFacing = (MultipleFacing) block.getBlockData();
        BlockMechanicFactory
                .getBlockMechanic(Utils.getCode(blockFacing))
                .getDrop()
                .spawns(block.getLocation(),
                        event.getPlayer().getInventory()
                                .getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingMushroomBlock(BlockPlaceEvent event) {

        if (event.getBlockPlaced().getType() != Material.MUSHROOM_STEM
                || OraxenItems.isAnItem(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        Block block = event.getBlock();
        BlockData blockData = block.getBlockData();
        Utils.setBlockFacing((MultipleFacing) blockData, 15);
        block.setBlockData(blockData, false);
    }

    // not static here because only instanciated once I think
    private final List<Material> replaceableBlocks = Arrays.asList(Material.SNOW, Material.VINE, Material.GRASS, Material.TALL_GRASS, Material.SEAGRASS, Material.FERN, Material.LARGE_FERN);

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(PlayerInteractEvent event) {
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
        if (replaceableBlocks.contains(type))
            target = placedAgainst;
        else
            target = placedAgainst.getRelative(event.getBlockFace());

        Location playerLocation = player.getLocation();
        if (target.getType() != Material.AIR || isStandingInside(player, target))
            return;

        // determines the old informations of the block
        BlockData curentBlockData = target.getBlockData();
        BlockState currentBlockState = target.getState();

        // determines the new block data of the block
        MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
        int customVariation = ((BlockMechanic) factory.getMechanic(itemID)).getCustomVariation();
        Utils.setBlockFacing(newBlockData, customVariation);

        //set the new block
        target.setBlockData(newBlockData); // false to cancel physic

        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, event.getHand());
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
        }
        event.setCancelled(true);
        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

    }

    private boolean isStandingInside(Player player, Block block) {
        Location playerLocation = player.getLocation();
        Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY() || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

}
