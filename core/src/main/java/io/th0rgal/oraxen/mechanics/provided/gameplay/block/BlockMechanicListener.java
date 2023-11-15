package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public BlockMechanicListener(final BlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMushroomPhysics(final BlockPhysicsEvent event) {
        if (event.getChangedType() == Material.MUSHROOM_STEM) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getType() != Material.MUSHROOM_STEM || !event.isDropItems()) return;

        final BlockMechanic blockMechanic = OraxenBlocks.getBlockMechanic(block);
        if (blockMechanic == null) return;

        blockMechanic.getDrop().spawns(block.getLocation(), event.getPlayer().getInventory().getItemInMainHand());
        event.setDropItems(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingMushroomBlock(final BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.MUSHROOM_STEM
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        final Block block = event.getBlock();
        final MultipleFacing blockData = (MultipleFacing) block.getBlockData();
        BlockMechanic.setBlockFacing(blockData, 15);
        block.setBlockData(blockData, false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block.getType() != Material.NOTE_BLOCK) return;
        BlockMechanic mechanic = (BlockMechanic) factory.getMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(block, blockFace)) {
            event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        }
    }

    // not static here because only instanciated once I think
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getHand() == null) return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Block placedAgainst = event.getClickedBlock();
        if (factory.isNotImplementedIn(itemID) || placedAgainst == null) return;

        final Player player = event.getPlayer();
        final Block target;
        final Material type = placedAgainst.getType();
        if (BlockHelpers.isReplaceable(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(event.getBlockFace());
            if (target.getType() != Material.AIR && target.getType() != Material.WATER
                    && target.getType() != Material.CAVE_AIR)
                return;
        }
        if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            return;

        // determines the old information of the block
        final BlockData curentBlockData = target.getBlockData();
        final BlockState currentBlockState = target.getState();

        // determines the new block data of the block
        final MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
        final BlockMechanic mechanic = ((BlockMechanic) factory.getMechanic(itemID));
        final int customVariation = mechanic.getCustomVariation();
        BlockMechanic.setBlockFacing(newBlockData, customVariation);
        Utils.swingHand(player, event.getHand());

        // set the new block
        target.setBlockData(newBlockData); // false to cancel physic
        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, event.getHand());
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (!EventUtils.callEvent(blockPlaceEvent) || !blockPlaceEvent.canBuild()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return;
        }

        event.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE)
            item.setAmount(item.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetFire(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.MUSHROOM_STEM) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() != BlockFace.UP) return;
        if (item == null) return;

        BlockMechanic mechanic = OraxenBlocks.getBlockMechanic(block);
        if (mechanic == null || !mechanic.canIgnite()) return;
        if (item.getType() != Material.FLINT_AND_STEEL && item.getType() != Material.FIRE_CHARGE) return;
        EventUtils.callEvent(new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(final BlockIgniteEvent event) {
        Block block = event.getBlock();
        BlockMechanic mechanic = OraxenBlocks.getBlockMechanic(block);
        if (block.getType() != Material.MUSHROOM_STEM || mechanic == null) return;
        if (!mechanic.canIgnite()) event.setCancelled(true);

        block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
        block.getRelative(BlockFace.UP).setType(Material.FIRE);
    }
}
