package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory.getBlockMechanic;
import static io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory.getCode;

public class StringBlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public StringBlockMechanicListener(final StringBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void tripwireEvent(BlockPhysicsEvent event) {
        if (event.getChangedType() == Material.TRIPWIRE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnteringTripwire(EntityInsideBlockEvent event) {
        if (event.getBlock().getType() == Material.TRIPWIRE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingString(final BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.STRING
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;
        if (event.getBlockAgainst().getType() == Material.TRIPWIRE)
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                    fixClientsideUpdate(event.getBlockAgainst().getLocation()), 1L);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        var tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

        for (Block block : tripwireList) {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            final StringBlockMechanic stringBlockMechanic = getBlockMechanic(getCode(tripwire));

            block.setType(Material.AIR, false);

            if (stringBlockMechanic == null) return;
            if (stringBlockMechanic.hasBreakSound())
                block.getWorld().playSound(block.getLocation(), stringBlockMechanic.getBreakSound(), 1.0f, 0.8f);
            if (stringBlockMechanic.getLight() != -1)
                WrappedLightAPI.removeBlockLight(block.getLocation());
            stringBlockMechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && block != null
                && block.getType() == Material.TRIPWIRE) {
            final ItemStack clicked = event.getItem();
            event.setCancelled(true);
            if (clicked == null)
                return;
            Material type = clicked.getType();
            if (type == null || clicked.getType().isInteractable())
                return;
            if (type == Material.LAVA_BUCKET)
                type = Material.LAVA;
            if (type == Material.WATER_BUCKET)
                type = Material.WATER;
            if (type.isBlock()) makePlayerPlaceBlock(event.getPlayer(), event.getHand(), event.getItem(), block,
                    event.getBlockFace(), Bukkit.createBlockData(type));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        Block blockAbove = event.getBlock().getRelative(BlockFace.UP);

        if (block.getType() == Material.TRIPWIRE) {
            final StringBlockMechanic stringBlockMechanic = StringBlockMechanicFactory
                    .getBlockMechanic(StringBlockMechanicFactory.getCode((Tripwire) block.getBlockData()));
            if (stringBlockMechanic == null) return;
            event.setCancelled(true);
            breakStringBlock(block, stringBlockMechanic, event.getPlayer().getInventory().getItemInMainHand());
            event.setDropItems(false);
        }

        else if (blockAbove.getType() == Material.TRIPWIRE) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            final StringBlockMechanic stringBlockMechanic = StringBlockMechanicFactory
                    .getBlockMechanic(StringBlockMechanicFactory.getCode(((Tripwire) blockAbove.getBlockData())));
            if (stringBlockMechanic == null) return;
            event.setCancelled(true);
            block.breakNaturally(item);
            breakStringBlock(blockAbove, stringBlockMechanic, item);
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();
        blockList.forEach(block -> {
            final Tripwire tripwire = (Tripwire) block.getBlockData();
            final StringBlockMechanic stringBlockMechanic = StringBlockMechanicFactory
                    .getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));
            if (stringBlockMechanic == null)
                return;

            stringBlockMechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            block.setType(Material.AIR, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(final BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.TRIPWIRE
                || OraxenItems.exists(OraxenItems.getIdByItem(event.getItemInHand())))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();

        // determines the new block data of the block
        StringBlockMechanic mechanic = (StringBlockMechanic) factory.getMechanic(itemID);
        final int customVariation = mechanic.getCustomVariation();

        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(),
                placedAgainst, event.getBlockFace(),
                StringBlockMechanicFactory.createTripwireData(customVariation));
        if (placedBlock == null)
            return;
        if (mechanic.hasPlaceSound())
            placedBlock.getWorld().playSound(placedBlock.getLocation(), mechanic.getPlaceSound(), 1.0f, 0.8f);
        if (placedBlock != null && mechanic.getLight() != -1)
            WrappedLightAPI.createBlockLight(placedBlock.getLocation(), mechanic.getLight());
        event.setCancelled(true);
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.TRIPWIRE)
                    return false;
                final Tripwire tripwire = (Tripwire) block.getBlockData();
                final int code = StringBlockMechanicFactory.getCode(tripwire);
                final StringBlockMechanic tripwireMechanic = StringBlockMechanicFactory
                        .getBlockMechanic(code);
                return tripwireMechanic != null && tripwireMechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                final Tripwire tripwire = (Tripwire) block.getBlockData();
                final StringBlockMechanic tripwireMechanic = StringBlockMechanicFactory
                        .getBlockMechanic(StringBlockMechanicFactory.getCode(tripwire));

                final long period = tripwireMechanic.getPeriod();
                double modifier = 1;
                if (tripwireMechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = tripwireMechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    private boolean isStandingInside(final Player player, final Block block) {
        final Location playerLocation = player.getLocation();
        final Location blockLocation = block.getLocation();
        return playerLocation.getBlockX() == blockLocation.getBlockX()
                && (playerLocation.getBlockY() == blockLocation.getBlockY()
                || playerLocation.getBlockY() + 1 == blockLocation.getBlockY())
                && playerLocation.getBlockZ() == blockLocation.getBlockZ();
    }

    private Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                       final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final Material type = placedAgainst.getType();
        if (Utils.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && target.getType() != Material.WATER && target.getType() != Material.LAVA)
                return null;
        }
        if (isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            return null;

        // determines the old informations of the block
        final BlockData curentBlockData = target.getBlockData();
        target.setBlockData(newBlock, false);
        final BlockState currentBlockState = target.getState();

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player,
                true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE))
            item.setAmount(item.getAmount() - 1);

        return target;
    }

    private void breakStringBlock(Block block, StringBlockMechanic mechanic, ItemStack item) {
        if (mechanic.hasBreakSound())
            block.getWorld().playSound(block.getLocation(), mechanic.getBreakSound(), 1.0f, 0.8f);
        if (mechanic.getLight() != -1)
            WrappedLightAPI.removeBlockLight(block.getLocation());
        mechanic.getDrop().spawns(block.getLocation(), item);
        block.setType(Material.AIR, false);
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                fixClientsideUpdate(block.getLocation()), 1L);
    }

    private void fixClientsideUpdate(Location blockLoc) {
        Block blockBelow = blockLoc.clone().subtract(0, 1, 0).getBlock();
        Block blockAbove = blockLoc.clone().add(0, 1, 0).getBlock();
        Location loc = blockLoc.add(5, 0, 5);
        List<Entity> players = blockLoc.getWorld().getNearbyEntities(blockLoc, 20, 20, 20).stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        if (blockBelow.getType() == Material.TRIPWIRE) {
            for (Entity e : players)
                ((Player) e).sendBlockChange(blockBelow.getLocation(), blockBelow.getBlockData());
        }

        if (blockAbove.getType() == Material.TRIPWIRE) {
            for (Entity e : players)
                ((Player) e).sendBlockChange(blockAbove.getLocation(), blockAbove.getBlockData());
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (loc.getBlock().getType() == Material.TRIPWIRE) {
                    for (Entity e : players)
                        ((Player) e).sendBlockChange(loc, loc.getBlock().getBlockData());
                }
                loc = loc.subtract(0, 0, 1);
            }
            loc = loc.add(-1, 0, 9);
        }
    }
}
