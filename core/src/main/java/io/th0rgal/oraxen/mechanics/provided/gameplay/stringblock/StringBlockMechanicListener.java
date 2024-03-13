package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockInteractEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.apache.commons.lang3.Range;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class StringBlockMechanicListener implements Listener {

    private final MechanicFactory factory;

    public StringBlockMechanicListener(final StringBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    public static class StringBlockMechanicPaperListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEnteringTripwire(EntityInsideBlockEvent event) {
            if (event.getBlock().getType() == Material.TRIPWIRE)
                event.setCancelled(true);
        }
    }

    public static class StringBlockMechanicPhysicsListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void tripwireEvent(BlockPhysicsEvent event) {
            Block block = event.getBlock();
            if (event.getChangedType() != Material.TRIPWIRE) return;
            if (event.getSourceBlock() == event.getBlock()) return;
            event.setCancelled(true);

            for (BlockFace f : BlockFace.values()) {
                if (!f.isCartesian() || f.getModY() != 0 || f == BlockFace.SELF) continue; // Only take N/S/W/E
                final Block changed = block.getRelative(f);
                if (changed.getType() != Material.TRIPWIRE) continue;

                final BlockData data = changed.getBlockData().clone();
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        changed.setBlockData(data, false), 1L);
            }

            // Stores the pre-change blockdata and applies it on next tick to prevent the block from updating
            final BlockData blockData = block.getBlockData().clone();
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable -> {
                if (block.getType().isAir()) return;
                block.setBlockData(blockData, false);
            }, 1L);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPistonPush(BlockPistonExtendEvent event) {
            List<Block> tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

            for (Block block : tripwireList) {
                final StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return;

                block.setType(Material.AIR, false);

                if (mechanic.hasLight())
                    mechanic.getLight().removeBlockLight(block);
                mechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBreak(BlockBreakEvent event) {
            final Block block = event.getBlock();
            final Block blockAbove = block.getRelative(BlockFace.UP);
            final Player player = event.getPlayer();

            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF && !face.isCartesian()) continue;
                if (block.getType() == Material.TRIPWIRE || block.getType() == Material.NOTE_BLOCK) break;
                if (block.getType() == Material.BARRIER && OraxenFurniture.isFurniture(block)) break;
                if (block.getRelative(face).getType() == Material.TRIPWIRE) {
                    if (player.getGameMode() != GameMode.CREATIVE) block.breakNaturally(player.getInventory().getItemInMainHand(), true);
                    else block.setType(Material.AIR);
                    if (BlockHelpers.isReplaceable(blockAbove.getType())) blockAbove.breakNaturally(true);
                    Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                            fixClientsideUpdate(block.getLocation()), 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
        if (mechanic == null) return;

        if (!EventUtils.callEvent(new OraxenStringBlockInteractEvent(mechanic, event.getPlayer(), event.getItem(), event.getHand(), block, event.getBlockFace())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlacingString(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.STRING) return;
        if (StringBlockMechanicFactory.getInstance().disableVanillaString) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingString(final BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType() != Material.TRIPWIRE || OraxenItems.exists(event.getItemInHand()))
            return;
        // Placing string, meant for the first blockstate as invisible string
        placedBlock.setBlockData(Material.TRIPWIRE.createBlockData(), false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing()) return;

        if (!event.getPlayer().isSneaking() && BlockHelpers.isInteractable(block)) return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(block, blockFace)) event.setCancelled(true);
        else if (limitedPlacing.isRadiusLimited()) {
            LimitedPlacing.RadiusLimitation radiusLimitation = limitedPlacing.getRadiusLimitation();
            int rad = radiusLimitation.getRadius();
            int amount = radiusLimitation.getAmount();
            int count = 0;
            for (int x = -rad; x <= rad; x++) for (int y = -rad; y <= rad; y++) for (int z = -rad; z <= rad; z++) {
                StringBlockMechanic relativeMechanic = OraxenBlocks.getStringMechanic(block.getRelative(x, y, z));
                if (relativeMechanic == null || !relativeMechanic.getItemID().equals(mechanic.getItemID())) continue;
                count++;
            }
            if (count >= amount) event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.ALLOW) {
            if (!limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        } else if (limitedPlacing.getType() == LimitedPlacing.LimitedPlacingType.DENY) {
            if (limitedPlacing.checkLimitedMechanic(belowPlaced))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Block placedAgainst = event.getClickedBlock();
        final Player player = event.getPlayer();
        StringBlockMechanic mechanic = (StringBlockMechanic) factory.getMechanic(itemID);

        if (mechanic == null || placedAgainst == null) return;
        if (!event.getPlayer().isSneaking() && BlockHelpers.isInteractable(placedAgainst)) return;

        if (item != null && item.getType().isBlock() && !factory.isNotImplementedIn(itemID)) {
            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian() || face.getModZ() != 0) continue;
                final Block relative = placedAgainst.getRelative(face);
                if (OraxenBlocks.getNoteBlockMechanic(relative) == null) continue;
                if (OraxenBlocks.getStringMechanic(relative) == null) continue;
                if (item.getItemMeta() instanceof BlockStateMeta) continue;
                if (item.getType().hasGravity()) continue;
                if (item.getType().toString().endsWith("SLAB")) continue;

                makePlayerPlaceBlock(player, event.getHand(), item, placedAgainst, event.getBlockFace(), Bukkit.createBlockData(item.getType()));
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), Runnable ->
                        fixClientsideUpdate(placedAgainst.getLocation()), 1L);
            }
        }

        int customVariation = mechanic.getCustomVariation();
        if (mechanic.hasRandomPlace()) {
            List<String> randomList = mechanic.getRandomPlaceBlock();
            String randomBlock = randomList.get(new Random().nextInt(randomList.size()));
            customVariation = ((StringBlockMechanic) factory.getMechanic(randomBlock)).getCustomVariation();
        }

        BlockData data = StringBlockMechanicFactory.createTripwireData(customVariation);
        makePlayerPlaceBlock(player, event.getHand(), item, placedAgainst, event.getBlockFace(), data);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final Block block = event.getClickedBlock();
        EquipmentSlot hand = event.getHand();
        if (hand == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (block == null || block.getType() != Material.TRIPWIRE) return;

        // Call the event
        StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
        if (stringBlockMechanic == null) return;
        if (!EventUtils.callEvent(new OraxenStringBlockInteractEvent(stringBlockMechanic, event.getPlayer(), event.getItem(), event.getHand(), block, event.getBlockFace())))
            event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Block blockBelow = block.getRelative(BlockFace.DOWN);
        final Player player = event.getPlayer();

        if (block.getType() == Material.TRIPWIRE) {
            StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(blockBelow);
            if (OraxenBlocks.isOraxenStringBlock(block)) {
                event.setCancelled(true);
                OraxenBlocks.remove(block.getLocation(), player);
                event.setDropItems(false);
            } else if (mechanicBelow != null && mechanicBelow.isTall()) {
                event.setCancelled(true);
                OraxenBlocks.remove(blockBelow.getLocation(), player);
                event.setDropItems(false);
            }
        } else if (blockAbove.getType() == Material.TRIPWIRE) {
            if (!OraxenBlocks.isOraxenStringBlock(blockAbove)) return;
            OraxenBlocks.remove(blockAbove.getLocation(), player);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();
        blockList.forEach(block -> {
            final StringBlockMechanic stringBlockMechanic = OraxenBlocks.getStringMechanic(block);
            if (stringBlockMechanic == null) return;

            final Block blockAbove = block.getRelative(BlockFace.UP);
            final Block blockBelow = block.getRelative(BlockFace.DOWN);
            if (block.getType() == Material.TRIPWIRE) {
                StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(blockBelow);
                if (OraxenBlocks.isOraxenStringBlock(block)) {
                    OraxenBlocks.remove(block.getLocation(), null, true);
                    event.blockList().remove(block);
                }
                else if (mechanicBelow != null && mechanicBelow.isTall()) {
                    OraxenBlocks.remove(blockBelow.getLocation(), null, true);
                    event.blockList().remove(block);
                }
            } else {
                if (!OraxenBlocks.isOraxenStringBlock(blockAbove)) return;

                OraxenBlocks.remove(blockAbove.getLocation(), null, true);
                event.blockList().remove(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterUpdate(final BlockFromToEvent event) {
        final Block changed = event.getToBlock();
        final Block changedBelow = changed.getRelative(BlockFace.DOWN);
        if (!event.getBlock().isLiquid() || changed.getType() != Material.TRIPWIRE) return;

        event.setCancelled(true);
        StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(changedBelow);
        if (OraxenBlocks.isOraxenStringBlock(changed))
            OraxenBlocks.remove(changed.getLocation(), null, true);
        else if (mechanicBelow != null && mechanicBelow.isTall())
            OraxenBlocks.remove(changedBelow.getLocation(), null, true);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.STRING) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;

            StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            if (mechanic == null) {
                StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(block.getRelative(BlockFace.DOWN));
                if (mechanicBelow == null || !mechanicBelow.isTall()) return;
                mechanic = mechanicBelow;
            }
            ItemStack item = OraxenItems.getItemById(mechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), mechanic.getItemID())) {
                    player.getInventory().setHeldItemSlot(i);
                    event.setCancelled(true);
                    return;
                }
            }
            event.setCursor(item);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void updateLightOnBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        //if (!OraxenBlocks.isOraxenStringBlock(block)) LightMechanic.refreshBlockLight(block);
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.TRIPWIRE)
                    return false;
                final StringBlockMechanic tripwireMechanic = OraxenBlocks.getStringMechanic(block);
                return tripwireMechanic != null && tripwireMechanic.hasHardness();
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                final StringBlockMechanic tripwireMechanic = OraxenBlocks.getStringMechanic(block);
                if (tripwireMechanic == null) return 0;
                final long period = tripwireMechanic.getHardness();
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

    private void makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                       final Block placedAgainst, final BlockFace face, final BlockData newData) {
        final Block target;
        final Material type = placedAgainst.getType();
        if (BlockHelpers.isReplaceable(type)) target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return;
        }

        StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(newData);
        // Store oldData incase event(s) is cancelled, set the target blockData
        final BlockData oldData = target.getBlockData();
        target.setBlockData(newData);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, hand);

        Range<Integer> worldHeightRange = Range.between(target.getWorld().getMinHeight(), target.getWorld().getMaxHeight() - 1);
        Block blockAbove = target.getRelative(BlockFace.UP);
        if (mechanic != null && mechanic.isTall()) {
            if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(blockAbove.getType()) || !worldHeightRange.contains(blockAbove.getY()))
                blockPlaceEvent.setCancelled(true);
            else blockAbove.setType(Material.TRIPWIRE);
        }
        if (!ProtectionLib.canBuild(player, target.getLocation())) blockPlaceEvent.setCancelled(true);
        //if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (!worldHeightRange.contains(target.getY()))
            blockPlaceEvent.setCancelled(true);

        // Call the event and check if it is cancelled, if so reset BlockData
        if (!EventUtils.callEvent(blockPlaceEvent) || !blockPlaceEvent.canBuild()) {
            target.setBlockData(oldData);
            return;
        }

        final String sound;
        if (mechanic != null) {
            OraxenBlocks.place(mechanic.getItemID(), target.getLocation());

            OraxenStringBlockPlaceEvent oraxenPlaceEvent = new OraxenStringBlockPlaceEvent(mechanic, target, player, item, hand);
            if (!EventUtils.callEvent(oraxenPlaceEvent)) {
                target.setBlockData(oldData);
                return;
            }

            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            Utils.swingHand(player, hand);
        } else {
            target.setType(Material.AIR);
            BlockHelpers.correctAllBlockStates(placedAgainst, player, hand, face, item, newData);
        }
        target.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, target.getLocation().toVector());
    }

    public static void fixClientsideUpdate(Location loc) {
        List<Entity> players =
                Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, 20, 20, 20)
                        .stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        for (double x = loc.getX() - 10; x < loc.getX() + 10; x++) {
            for (double y = loc.getY() - 4; y < loc.getY() + 4; y++) {
                for (double z = loc.getZ() - 10; z < loc.getZ() + 10; z++) {
                    if (loc.getBlock().getType() == Material.TRIPWIRE) {
                        Location newLoc = new Location(loc.getWorld(), x, y, z);
                        for (Entity e : players) {
                            ((Player) e).sendBlockChange(newLoc, newLoc.getBlock().getBlockData());
                        }
                    }
                }
            }
        }
    }
}
