package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockInteractEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.storage.StorageMechanic;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.apache.commons.lang3.Range;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ChorusBlockMechanicListener implements Listener {

    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "chorus_seat");

    public ChorusBlockMechanicListener() {
        if (OraxenPlugin.get().getPacketAdapter().isEnabled())
            BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    public static class ChorusBlockMechanicPaperListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEnteringChorusPlant(EntityInsideBlockEvent event) {
            if (event.getBlock().getType() == Material.CHORUS_PLANT)
                event.setCancelled(true);
        }
    }

    public static class ChorusBlockMechanicPhysicsListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void chorusPlantPhysics(BlockPhysicsEvent event) {
            Block block = event.getBlock();
            if (event.getChangedType() != Material.CHORUS_PLANT)
                return;
            if (event.getSourceBlock() == event.getBlock())
                return;

            // If this is a custom ChorusBlock, cancel the event to prevent any changes
            if (OraxenBlocks.isOraxenChorusBlock(block)) {
                event.setCancelled(true);
                return;
            }

            // Only handle vanilla chorus plant updates
            for (BlockFace f : BlockFace.values()) {
                if (!f.isCartesian() || f == BlockFace.SELF)
                    continue;
                final Block changed = block.getRelative(f);
                if (changed.getType() != Material.CHORUS_PLANT || OraxenBlocks.isOraxenChorusBlock(changed))
                    continue;

                final BlockData data = changed.getBlockData().clone();
                SchedulerUtil.runAtLocationLater(changed.getLocation(), 1L, () -> changed.setBlockData(data, false));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPistonPush(BlockPistonExtendEvent event) {
            List<Block> chorusList = event.getBlocks().stream()
                    .filter(block -> block.getType().equals(Material.CHORUS_PLANT)).toList();

            // First pass: check for immovable blocks before modifying anything
            for (Block block : chorusList) {
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic != null && mechanic.isImmovable()) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Second pass: destroy blocks now that we know none are immovable
            for (Block block : chorusList) {
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic == null)
                    continue;

                block.setType(Material.AIR, false);

                if (mechanic.hasLight())
                    mechanic.getLight().removeBlockLight(block);
                mechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPistonPull(BlockPistonRetractEvent event) {
            List<Block> chorusList = event.getBlocks().stream()
                    .filter(block -> block.getType().equals(Material.CHORUS_PLANT)).toList();

            // First pass: check for immovable blocks before modifying anything
            for (Block block : chorusList) {
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic != null && mechanic.isImmovable()) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Second pass: destroy blocks now that we know none are immovable
            for (Block block : chorusList) {
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic == null)
                    continue;

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
                if (face == BlockFace.SELF && !face.isCartesian())
                    continue;
                if (block.getType() == Material.CHORUS_PLANT)
                    break;
                if (block.getRelative(face).getType() == Material.CHORUS_PLANT) {
                    if (player.getGameMode() != GameMode.CREATIVE)
                        block.breakNaturally(player.getInventory().getItemInMainHand(), true);
                    else
                        block.setType(Material.AIR);
                    if (BlockHelpers.isReplaceable(blockAbove.getType()))
                        blockAbove.breakNaturally(true);
                    SchedulerUtil.runAtLocationLater(block.getLocation(), 1L,
                            () -> fixClientsideUpdate(block.getLocation()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getClickedBlock().getType() != Material.CHORUS_PLANT)
            return;
        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
        if (mechanic == null)
            return;

        OraxenChorusBlockInteractEvent interactEvent = new OraxenChorusBlockInteractEvent(
                mechanic, event.getPlayer(), event.getItem(),
                event.getHand(), block, event.getBlockFace());
        if (!EventUtils.callEvent(interactEvent)) {
            event.setCancelled(true);
            return;
        }

        // Handle click actions
        if (mechanic.hasClickActions()) {
            mechanic.runClickActions(event.getPlayer());
        }

        // Handle storage interaction
        if (mechanic.isStorage()) {
            handleStorageInteraction(mechanic, block, event.getPlayer());
            event.setCancelled(true);
            return;
        }

        // Handle seat interaction
        if (mechanic.hasSeat()) {
            handleSeatInteraction(mechanic, block, event.getPlayer());
            event.setCancelled(true);
        }
    }

    private void handleStorageInteraction(ChorusBlockMechanic mechanic, Block block, Player player) {
        StorageMechanic storage = mechanic.getStorage();
        if (storage == null) return;

        switch (storage.getStorageType()) {
            case STORAGE, SHULKER -> storage.openStorage(block, player);
            case PERSONAL -> storage.openPersonalStorage(player, block.getLocation(), null);
            case DISPOSAL -> storage.openDisposal(player, block.getLocation(), null);
            case ENDERCHEST -> player.openInventory(player.getEnderChest());
        }
    }

    private void handleSeatInteraction(ChorusBlockMechanic mechanic, Block block, Player player) {
        // Check if there's already a seat at this location
        ArmorStand existingSeat = getSeat(block);
        if (existingSeat != null) {
            // Player is already sitting or someone else is
            if (!existingSeat.getPassengers().isEmpty()) {
                return;
            }
            // Seat exists but is empty, seat the player
            existingSeat.addPassenger(player);
            return;
        }

        // Create a new seat
        UUID seatUuid = spawnSeat(block, mechanic);
        if (seatUuid != null) {
            Entity seat = block.getWorld().getEntity(seatUuid);
            if (seat != null) {
                seat.addPassenger(player);
            }
        }
    }

    private ArmorStand getSeat(Block block) {
        Location seatLoc = BlockHelpers.toCenterBlockLocation(block.getLocation());
        if (block.getWorld() == null) return null;
        for (Entity entity : block.getWorld().getNearbyEntities(seatLoc, 0.5, 2, 0.5)) {
            if (!(entity instanceof ArmorStand armorStand)) continue;
            PersistentDataContainer pdc = armorStand.getPersistentDataContainer();
            if (pdc.has(SEAT_KEY, PersistentDataType.STRING)) {
                return armorStand;
            }
        }
        return null;
    }

    private UUID spawnSeat(Block block, ChorusBlockMechanic mechanic) {
        // For blocks, seatHeight is relative to block top. ArmorStand passengers sit ~0.3 blocks above the stand.
        // A small ArmorStand is ~0.5 blocks tall. To sit ON the block (Y+1), we need stand at ~Y+0.7
        // With seatHeight=0 meaning "on top of block", formula: Y + 0.5 + seatHeight (where 0.5 accounts for offset)
        Location seatLoc = block.getLocation().add(0.5, mechanic.getSeatHeight(), 0.5);
        float yaw = mechanic.hasSeatYaw() ? mechanic.getSeatYaw() : 0;

        ArmorStand seat = EntityUtils.spawnEntity(seatLoc, ArmorStand.class, (ArmorStand stand) -> {
            stand.setVisible(false);
            stand.setRotation(yaw, 0);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, mechanic.getItemID());
        });

        // Store seat UUID in block PDC
        PersistentDataContainer blockPdc = BlockHelpers.getPDC(block);
        blockPdc.set(SEAT_KEY, DataType.UUID, seat.getUniqueId());

        return seat.getUniqueId();
    }

    private void removeSeat(Block block) {
        ArmorStand seat = getSeat(block);
        if (seat != null) {
            seat.getPassengers().forEach(seat::removePassenger);
            if (!seat.isDead()) seat.remove();
        }
        PersistentDataContainer blockPdc = BlockHelpers.getPDC(block);
        blockPdc.remove(SEAT_KEY);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND)
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(OraxenItems.getIdByItem(item));
        if (mechanic == null || !mechanic.hasLimitedPlacing())
            return;

        if (!event.getPlayer().isSneaking() && BlockHelpers.isInteractable(block))
            return;

        LimitedPlacing limitedPlacing = mechanic.getLimitedPlacing();
        Block belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN);

        if (limitedPlacing.isNotPlacableOn(block, blockFace))
            event.setCancelled(true);
        else if (limitedPlacing.isRadiusLimited()) {
            LimitedPlacing.RadiusLimitation radiusLimitation = limitedPlacing.getRadiusLimitation();
            int rad = radiusLimitation.getRadius();
            int amount = radiusLimitation.getAmount();
            int count = 0;
            for (int x = -rad; x <= rad; x++)
                for (int y = -rad; y <= rad; y++)
                    for (int z = -rad; z <= rad; z++) {
                        ChorusBlockMechanic relativeMechanic = OraxenBlocks
                                .getChorusMechanic(block.getRelative(x, y, z));
                        if (relativeMechanic == null || !relativeMechanic.getItemID().equals(mechanic.getItemID()))
                            continue;
                        count++;
                    }
            if (count >= amount)
                event.setCancelled(true);
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Block placedAgainst = event.getClickedBlock();
        final Player player = event.getPlayer();
        ChorusBlockMechanic mechanic = ChorusBlockMechanicFactory.getInstance() != null
                ? ChorusBlockMechanicFactory.getInstance().getMechanic(itemID)
                : null;

        if (mechanic == null || placedAgainst == null)
            return;
        if (!event.getPlayer().isSneaking() && BlockHelpers.isInteractable(placedAgainst))
            return;

        BlockData data = ChorusBlockMechanicFactory.createChorusData(mechanic.getCustomVariation());
        makePlayerPlaceBlock(player, event.getHand(), item, placedAgainst, event.getBlockFace(), data, mechanic);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Block blockAbove = block.getRelative(BlockFace.UP);
        final Player player = event.getPlayer();

        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
        if (mechanic != null) {
            event.setCancelled(true);
            if (!ProtectionLib.canBreak(player, block.getLocation()))
                return;
            // Drop storage contents before removing block
            if (mechanic.isStorage()) {
                mechanic.getStorage().dropStorageContent(block);
            }
            // Remove seat if present
            if (mechanic.hasSeat()) {
                removeSeat(block);
            }
            OraxenBlocks.remove(block.getLocation(), player);
            event.setDropItems(false);
            return;
        }

        // Handle breaking block below chorus
        if (blockAbove.getType() == Material.CHORUS_PLANT && OraxenBlocks.isOraxenChorusBlock(blockAbove)) {
            ChorusBlockMechanic aboveMechanic = OraxenBlocks.getChorusMechanic(blockAbove);
            if (aboveMechanic != null) {
                if (aboveMechanic.isStorage()) {
                    aboveMechanic.getStorage().dropStorageContent(blockAbove);
                }
                if (aboveMechanic.hasSeat()) {
                    removeSeat(blockAbove);
                }
                // Handle falling blocks - spawn falling block instead of just removing
                if (aboveMechanic.isFalling()) {
                    BlockData aboveBlockData = blockAbove.getBlockData();
                    Location fallingLocation = BlockHelpers.toCenterBlockLocation(blockAbove.getLocation());

                    OraxenBlocks.remove(blockAbove.getLocation(), null);

                    if (fallingLocation.getNearbyEntitiesByType(FallingBlock.class, 0.25).isEmpty()) {
                        FallingBlock fallingBlock = blockAbove.getWorld().spawnFallingBlock(fallingLocation, aboveBlockData);
                        fallingBlock.setDropItem(false);
                    }
                    return;
                }
            }
            OraxenBlocks.remove(blockAbove.getLocation(), player);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream()
                .filter(block -> block.getType().equals(Material.CHORUS_PLANT))
                .toList();
        blockList.forEach(block -> {
            final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            if (mechanic == null)
                return;

            // Respect blast resistance
            if (mechanic.isBlastResistant()) {
                event.blockList().remove(block);
                return;
            }

            final Block blockAbove = block.getRelative(BlockFace.UP);
            if (block.getType() == Material.CHORUS_PLANT) {
                if (OraxenBlocks.isOraxenChorusBlock(block)) {
                    OraxenBlocks.remove(block.getLocation(), null, true);
                    event.blockList().remove(block);
                }
            } else {
                if (!OraxenBlocks.isOraxenChorusBlock(blockAbove))
                    return;

                OraxenBlocks.remove(blockAbove.getLocation(), null, true);
                event.blockList().remove(block);
            }
        });
    }

    @EventHandler
    public void onBlockExplosionDestroy(BlockExplodeEvent event) {
        List<Block> blockList = event.blockList().stream()
                .filter(block -> block.getType().equals(Material.CHORUS_PLANT))
                .toList();
        blockList.forEach(block -> {
            final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            if (mechanic == null)
                return;

            // Respect blast resistance
            if (mechanic.isBlastResistant()) {
                event.blockList().remove(block);
                return;
            }

            final Block blockAbove = block.getRelative(BlockFace.UP);
            if (block.getType() == Material.CHORUS_PLANT) {
                if (OraxenBlocks.isOraxenChorusBlock(block)) {
                    OraxenBlocks.remove(block.getLocation(), null, true);
                    event.blockList().remove(block);
                }
            } else {
                if (!OraxenBlocks.isOraxenChorusBlock(blockAbove))
                    return;

                OraxenBlocks.remove(blockAbove.getLocation(), null, true);
                event.blockList().remove(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWaterUpdate(final BlockFromToEvent event) {
        final Block changed = event.getToBlock();
        if (!event.getBlock().isLiquid() || changed.getType() != Material.CHORUS_PLANT)
            return;

        if (OraxenBlocks.isOraxenChorusBlock(changed)) {
            event.setCancelled(true);
            OraxenBlocks.remove(changed.getLocation(), null, true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE)
            return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null)
            return;
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() != Material.CHORUS_FRUIT)
            return;

        final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
        if (rayTraceResult == null)
            return;
        final Block block = rayTraceResult.getHitBlock();
        if (block == null)
            return;

        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
        if (mechanic == null)
            return;

        ItemStack item = OraxenItems.getItemById(mechanic.getItemID()).build();
        for (int i = 0; i <= 8; i++) {
            ItemStack hotbarItem = player.getInventory().getItem(i);
            if (hotbarItem == null)
                continue;
            if (Objects.equals(OraxenItems.getIdByItem(hotbarItem), mechanic.getItemID())) {
                player.getInventory().setHeldItemSlot(i);
                event.setCancelled(true);
                return;
            }
        }
        event.setCursor(item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock))
            return;
        if (fallingBlock.getBlockData().getMaterial() != Material.CHORUS_PLANT)
            return;

        BlockData blockData = fallingBlock.getBlockData();
        if (!(blockData instanceof MultipleFacing multipleFacing))
            return;

        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(blockData);
        if (mechanic == null)
            return;

        // Cancel the event to prevent Minecraft from placing vanilla chorus plant
        event.setCancelled(true);

        // Place the block at the landing location
        Block block = event.getBlock();
        block.setBlockData(multipleFacing, false);
        OraxenBlocks.place(mechanic.getItemID(), block.getLocation());

        if (mechanic.hasLight()) {
            mechanic.getLight().createBlockLight(block);
        }
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.CHORUS_PLANT)
                    return false;
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                return mechanic != null && mechanic.hasHardness();
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic == null)
                    return 0;
                final long hardness = mechanic.getHardness();
                double modifier = 1;
                if (mechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = mechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                long period = (long) (hardness * modifier);
                return period == 0 && mechanic.hasHardness() ? 1 : period;
            }
        };
    }

    private void makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
            final Block placedAgainst, final BlockFace face, final BlockData newData,
            final ChorusBlockMechanic mechanic) {
        final Block target;
        final Material type = placedAgainst.getType();
        if (BlockHelpers.isReplaceable(type))
            target = placedAgainst;
        else
            target = placedAgainst.getRelative(face);
        if (!BlockHelpers.isReplaceable(target.getType()))
            return;

        // Store oldData in case event(s) is cancelled, set the target blockData
        final BlockData oldData = target.getBlockData();
        target.setBlockData(newData);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item,
                player, true, hand);

        Range<Integer> worldHeightRange = Range.between(target.getWorld().getMinHeight(),
                target.getWorld().getMaxHeight() - 1);
        if (!ProtectionLib.canBuild(player, target.getLocation()))
            blockPlaceEvent.setCancelled(true);
        if (!worldHeightRange.contains(target.getY()))
            blockPlaceEvent.setCancelled(true);

        // Call the event and check if it is cancelled, if so reset BlockData
        if (!EventUtils.callEvent(blockPlaceEvent) || !blockPlaceEvent.canBuild()) {
            target.setBlockData(oldData);
            return;
        }

        OraxenBlocks.place(mechanic.getItemID(), target.getLocation());

        OraxenChorusBlockPlaceEvent oraxenPlaceEvent = new OraxenChorusBlockPlaceEvent(mechanic, target, player,
                item, hand);
        if (!EventUtils.callEvent(oraxenPlaceEvent)) {
            target.setBlockData(oldData);
            return;
        }

        // Handle falling blocks
        if (mechanic.isFalling()) {
            Block below = target.getRelative(BlockFace.DOWN);
            if (below.getType() == Material.AIR || BlockHelpers.isReplaceable(below.getType())) {
                target.setType(Material.AIR, false);
                FallingBlock fallingBlock = target.getWorld().spawnFallingBlock(
                        target.getLocation().add(0.5, 0, 0.5), newData);
                fallingBlock.setDropItem(false);
            }
        }

        if (player.getGameMode() != GameMode.CREATIVE)
            item.setAmount(item.getAmount() - 1);
        Utils.swingHand(player, hand);

        if (VersionUtil.isPaperServer())
            target.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, target.getLocation().toVector());
    }

    public static void fixClientsideUpdate(Location loc) {
        List<Entity> players = Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, 20, 20, 20)
                .stream().filter(entity -> entity.getType() == EntityType.PLAYER).toList();

        for (double x = loc.getX() - 10; x < loc.getX() + 10; x++) {
            for (double y = loc.getY() - 4; y < loc.getY() + 4; y++) {
                for (double z = loc.getZ() - 10; z < loc.getZ() + 10; z++) {
                    Location newLoc = new Location(loc.getWorld(), x, y, z);
                    if (newLoc.getBlock().getType() == Material.CHORUS_PLANT) {
                        for (Entity e : players) {
                            ((Player) e).sendBlockChange(newLoc, newLoc.getBlock().getBlockData());
                        }
                    }
                }
            }
        }
    }
}
