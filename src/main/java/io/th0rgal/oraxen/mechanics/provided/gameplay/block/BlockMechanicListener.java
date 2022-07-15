package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
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
        if (block.getType() != Material.MUSHROOM_STEM || !event.isDropItems())
            return;

        final MultipleFacing blockFacing = (MultipleFacing) block.getBlockData();
        final BlockMechanic blockMechanic = BlockMechanicFactory.getBlockMechanic(BlockMechanic.getCode(blockFacing));
        if (blockMechanic == null)
            return;
        if (blockMechanic.hasBreakSound())
            block.getWorld().playSound(block.getLocation(), blockMechanic.getBreakSound(), 1.0f, 0.8f);
        blockMechanic.getDrop().spawns(block.getLocation(), event.getPlayer().getInventory().getItemInMainHand());
        event.setDropItems(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingMushroomBlock(final BlockPlaceEvent event) {

        if (event.getBlockPlaced().getType() != Material.MUSHROOM_STEM
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        final Block block = event.getBlock();
        final BlockData blockData = block.getBlockData();
        BlockMechanic.setBlockFacing((MultipleFacing) blockData, 15);
        block.setBlockData(blockData, false);
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
        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(event.getBlockFace());
            if (target.getType() != Material.AIR && target.getType() != Material.WATER
                    && target.getType() != Material.CAVE_AIR)
                return;
        }
        if (isStandingInside(player, target)
                || !ProtectionLib.canBuild(player, target.getLocation()))
            return;

        // determines the old informations of the block
        final BlockData curentBlockData = target.getBlockData();
        final BlockState currentBlockState = target.getState();

        // determines the new block data of the block
        final MultipleFacing newBlockData = (MultipleFacing) Bukkit.createBlockData(Material.MUSHROOM_STEM);
        final BlockMechanic mechanic = ((BlockMechanic) factory.getMechanic(itemID));
        final int customVariation = mechanic.getCustomVariation();
        BlockMechanic.setBlockFacing(newBlockData, customVariation);
        Utils.sendAnimation(player, event.getHand());

        // set the new block
        target.setBlockData(newBlockData); // false to cancel physic
        final BlockPlaceEvent blockPlaceEvent =
                new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, event.getHand());

        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return;
        }
        if (mechanic.hasPlaceSound())
            target.getWorld().playSound(target.getLocation(), mechanic.getPlaceSound(), 1.0f, 0.8f);
        event.setCancelled(true);
        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStep(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Block below = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        SoundGroup soundGroup = below.getBlockData().getSoundGroup();

        if (event.getEvent() != GameEvent.STEP) return;
        if (!(below.getBlockData() instanceof final MultipleFacing blockFacing)) return;
        if (below.getType() != Material.MUSHROOM_STEM) return;

        final BlockMechanic mechanic = BlockMechanicFactory.getBlockMechanic(BlockMechanic.getCode(blockFacing));
        if (mechanic == null) return;
        below.getWorld().playSound(below.getLocation(), mechanic.getStepSound(), soundGroup.getVolume(), soundGroup.getPitch());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Block below = entity.getLocation().getBlock().getRelative(BlockFace.DOWN);
        SoundGroup soundGroup = below.getBlockData().getSoundGroup();

        if (event.getEvent() != GameEvent.HIT_GROUND) return;
        if (!(below.getBlockData() instanceof final MultipleFacing blockFacing)) return;
        if (below.getType() != Material.MUSHROOM_STEM) return;

        final BlockMechanic mechanic = BlockMechanicFactory.getBlockMechanic(BlockMechanic.getCode(blockFacing));
        if (mechanic == null) return;
        below.getWorld().playSound(below.getLocation(), mechanic.getFallSound(), soundGroup.getVolume(), soundGroup.getPitch());
    }

    private boolean isStandingInside(final Player player, final Block block) {
        final Location playerLocation = player.getLocation();
        final Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

}
