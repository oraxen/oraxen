package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
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
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
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
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.*;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class NoteBlockMechanicListener implements Listener {

    public NoteBlockMechanicListener() {
        if (PluginUtils.isEnabled("ProtocolLib")) BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

    public static class NoteBlockMechanicPaperListener implements Listener {

        @EventHandler
        public void onFallingBlockLandOnCarpet(EntityRemoveFromWorldEvent event) {
            if (!(event.getEntity() instanceof FallingBlock fallingBlock)) return;
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(fallingBlock.getBlockData());
            if (mechanic == null || Objects.equals(OraxenBlocks.getOraxenBlock(fallingBlock.getLocation()), mechanic))
                return;
            if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                mechanic = mechanic.getDirectional().getParentMechanic();

            ItemStack itemStack = OraxenItems.getItemById(mechanic.getItemID()).build();
            fallingBlock.setDropItem(false);
            fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(), itemStack);
        }
    }

    public static class NoteBlockMechanicPhysicsListener implements Listener {
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPistonPush(BlockPistonExtendEvent event) {
            if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPistonPull(BlockPistonRetractEvent event) {
            if (event.getBlocks().stream().anyMatch(block -> block.getType().equals(Material.NOTE_BLOCK)))
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onBlockPhysics(final BlockPhysicsEvent event) {
            final Block block = event.getBlock();
            final Block aboveBlock = block.getRelative(BlockFace.UP);
            final Block belowBlock = block.getRelative(BlockFace.DOWN);
            // If block below is NoteBlock, it will be affected by the break
            // Call updateAndCheck from it to fix vertical stack of NoteBlocks
            // if belowBlock is not a NoteBlock we must ensure the above is not, if it is call updateAndCheck from block
            if (belowBlock.getType() == Material.NOTE_BLOCK) {
                event.setCancelled(true);
                updateAndCheck(belowBlock);
            } else if (aboveBlock.getType() == Material.NOTE_BLOCK) {
                event.setCancelled(true);
                updateAndCheck(aboveBlock);
            }
            if (block.getType() == Material.NOTE_BLOCK) {
                event.setCancelled(true);
                updateAndCheck(block);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onNoteblockPowered(final GenericGameEvent event) {
            Block block = event.getLocation().getBlock();

            Location eLoc = block.getLocation();
            if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

            // This GameEvent only exists in 1.19
            // If server is 1.18 check if its there and if not return
            // If 1.19 we can check if this event is fired
            if (!VersionUtil.atOrAbove("1.19")) return;
            if (event.getEvent() != GameEvent.NOTE_BLOCK_PLAY) return;
            if (block.getType() != Material.NOTE_BLOCK) return;
            NoteBlock data = (NoteBlock) block.getBlockData().clone();
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> block.setBlockData(data, false), 1L);
        }

        public void updateAndCheck(Block block) {
            final Block blockAbove = block.getRelative(BlockFace.UP);
            if (blockAbove.getType() == Material.NOTE_BLOCK)
                blockAbove.getState().update(true, true);
            Block nextBlock = blockAbove.getRelative(BlockFace.UP);
            if (nextBlock.getType() == Material.NOTE_BLOCK) updateAndCheck(blockAbove);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (!EventUtils.callEvent(new OraxenNoteBlockInteractEvent(mechanic, event.getPlayer(), event.getItem(), event.getHand(), block, event.getBlockFace(), event.getAction())))
            event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLimitedPlacing(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        ItemStack item = event.getItem();

        if (item == null || block == null || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(OraxenItems.getIdByItem(item));
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
                NoteBlockMechanic relativeMechanic = OraxenBlocks.getNoteBlockMechanic(block.getRelative(x, y, z));
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractNoteBlock(OraxenNoteBlockInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = event.getMechanic();
        if (!ProtectionLib.canInteract(player, block.getLocation())) return;

        if (!player.isSneaking()) {
            if (mechanic.hasClickActions()) {
                mechanic.runClickActions(player);
                event.setCancelled(true);
            }

            if (mechanic.isStorage()) {
                StorageMechanic storageMechanic = mechanic.getStorage();
                switch (storageMechanic.getStorageType()) {
                    case STORAGE, SHULKER -> storageMechanic.openStorage(block, player);
                    case PERSONAL -> storageMechanic.openPersonalStorage(player, block.getLocation(), null);
                    case DISPOSAL -> storageMechanic.openDisposal(player, block.getLocation(), null);
                    case ENDERCHEST -> player.openInventory(player.getEnderChest());
                }
                event.setCancelled(true);
            }
        }
    }

    // TODO Make this function less of a clusterfuck and more readable
    // Make sure this isnt handling it together with above when placing CB against CB
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceAgainstNoteBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        EquipmentSlot hand = event.getHand();
        BlockFace blockFace = event.getBlockFace();

        if (hand == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (!player.isSneaking() && BlockHelpers.isInteractable(block)) return;
        if (event.useInteractedBlock() == Event.Result.DENY || !OraxenBlocks.isOraxenNoteBlock(block)) return;

        event.setUseInteractedBlock(Event.Result.DENY);
        if (OraxenBlocks.isOraxenNoteBlock(item)) return;
        if (item == null) return;

        Material type = item.getType();
        if (type == Material.AIR) return;

        BlockData newData = type.isBlock() ? type.createBlockData() : null;
        makePlayerPlaceBlock(player, event.getHand(), item, block, blockFace, newData);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || placedAgainst == null) return;
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(itemID);
        if (mechanic == null) return;
        if (!player.isSneaking() && BlockHelpers.isInteractable(placedAgainst)) return;

        // determines the new block data of the block
        int customVariation = mechanic.getCustomVariation();
        BlockFace face = event.getBlockFace();

        if (mechanic.isDirectional()) {
            DirectionalBlock directional = mechanic.getDirectional();
            if (!directional.isParentBlock()) {
                directional = directional.getParentMechanic().getDirectional();
            }

            customVariation = directional.getDirectionVariation(face, player);
        }

        BlockData data = NoteBlockMechanicFactory.createNoteBlockData(customVariation);
        makePlayerPlaceBlock(player, event.getHand(), event.getItem(), placedAgainst, face, data);
    }

    // If block is not a custom block, play the correct sound according to the below block or default
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(final NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO) event.setCancelled(true);
        else {
            if (instrumentMap.isEmpty()) instrumentMap = getInstrumentMap();
            String blockType = event.getBlock().getRelative(BlockFace.DOWN).getType().toString().toLowerCase(Locale.ROOT);
            Instrument fakeInstrument = instrumentMap.entrySet().stream().filter(e -> e.getValue().contains(blockType)).map(Map.Entry::getKey).findFirst().orElse(Instrument.PIANO);
            // This is deprecated, but seems to be without reason
            event.setInstrument(fakeInstrument);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        if (OraxenBlocks.isOraxenNoteBlock(event.getBlock())) event.setDropItems(false);
        OraxenBlocks.remove(event.getBlock().getLocation(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        if (VersionUtil.atOrAbove("1.21")) {
            ExplosionResult result = event.getExplosionResult();
            if (result != ExplosionResult.DESTROY && result != ExplosionResult.DESTROY_WITH_DECAY) return;
        }
        for (Block block : new HashSet<>(event.blockList())) {
            if (!OraxenBlocks.isOraxenNoteBlock(block)) continue;
            OraxenBlocks.remove(block.getLocation(), null);
            event.blockList().remove(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        if (VersionUtil.atOrAbove("1.21")) {
            ExplosionResult result = event.getExplosionResult();
            if (result != ExplosionResult.DESTROY && result != ExplosionResult.DESTROY_WITH_DECAY) return;
        }
        for (Block block : new HashSet<>(event.blockList())) {
            if (!OraxenBlocks.isOraxenNoteBlock(block)) continue;
            OraxenBlocks.remove(block.getLocation(), null);
            event.blockList().remove(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFallingOraxenBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fallingBlock) {
            BlockData blockData = fallingBlock.getBlockData();
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(blockData);
            if (mechanic == null) return;
            OraxenBlocks.place(mechanic.getItemID(), event.getBlock().getLocation());
            fallingBlock.setDropItem(false);
        }
    }

    @EventHandler
    public void onBreakBeneathFallingOraxenBlock(BlockBreakEvent event) {
        handleFallingOraxenBlockAbove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetFire(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() != BlockFace.UP) return;
        if (item == null) return;

        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (!mechanic.canIgnite()) return;
        if (item.getType() != Material.FLINT_AND_STEEL && item.getType() != Material.FIRE_CHARGE) return;

        EventUtils.callEvent(new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(final BlockIgniteEvent event) {
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (!mechanic.canIgnite()) event.setCancelled(true);
        else {
            block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
            block.getRelative(BlockFace.UP).setType(Material.FIRE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(final BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.NOTE_BLOCK) return;
        if (OraxenBlocks.isOraxenNoteBlock(OraxenItems.getIdByItem(event.getItemInHand()))) return;

        placed.setBlockData(Bukkit.createBlockData(Material.NOTE_BLOCK), false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMiddleClick(final InventoryCreativeEvent event) {
        if (event.getClick() != ClickType.CREATIVE) return;
        final Player player = (Player) event.getInventory().getHolder();
        if (player == null) return;
        if (event.getCursor().getType() == Material.NOTE_BLOCK) {
            final RayTraceResult rayTraceResult = player.rayTraceBlocks(6.0);
            if (rayTraceResult == null) return;
            final Block block = rayTraceResult.getHitBlock();
            if (block == null) return;
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic == null) return;

            ItemStack item;
            if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                item = OraxenItems.getItemById(mechanic.getDirectional().getParentBlock()).build();
            else
                item = OraxenItems.getItemById(mechanic.getItemID()).build();
            for (int i = 0; i <= 8; i++) {
                if (player.getInventory().getItem(i) == null) continue;
                if (Objects.equals(OraxenItems.getIdByItem(player.getInventory().getItem(i)), OraxenItems.getIdByItem(item))) {
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
        //if (!OraxenBlocks.isOraxenNoteBlock(block)) LightMechanic.refreshBlockLight(block);
    }

    //TODO Also trigger for attached blocks
    /* Make Falling Oraxen Blocks above the given block trigger causing physics changes*/
    private void handleFallingOraxenBlockAbove(Block block) {
        Block blockAbove = block.getRelative(BlockFace.UP);
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(blockAbove);
        if (mechanic == null || !mechanic.isFalling()) return;
        Location fallingLocation = BlockHelpers.toCenterBlockLocation(blockAbove.getLocation());
        BlockData fallingData = OraxenBlocks.getOraxenBlockData(mechanic.getItemID());
        if (fallingData == null) return;
        OraxenBlocks.remove(blockAbove.getLocation(), null);
        blockAbove.getWorld().spawnFallingBlock(fallingLocation, fallingData);
        handleFallingOraxenBlockAbove(blockAbove);
    }

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.NOTE_BLOCK)
                    return false;

                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return false;

                if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                    mechanic = mechanic.getDirectional().getParentMechanic();

                return mechanic.hasHardness();
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return 0;
                if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
                    mechanic = mechanic.getDirectional().getParentMechanic();

                final long period = mechanic.getHardness();
                double modifier = 1;
                if (mechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = mechanic.getDrop().getDiff(tool);
                    if (diff >= 1)
                        modifier *= Math.pow(0.9, diff);
                }
                return (long) (period * modifier);
            }
        };
    }

    public void makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                     final Block placedAgainst, final BlockFace face, final BlockData newData) {
        final Block target;
        final Material type = placedAgainst.getType();

        if (BlockHelpers.isReplaceable(type)) target = placedAgainst;
        else target = placedAgainst.getRelative(face);
        if (!BlockHelpers.isReplaceable(target.getType())) return;

        final NoteBlockMechanic againstMechanic = OraxenBlocks.getNoteBlockMechanic(placedAgainst);
        // Store oldData incase event(s) is cancelled, set the target blockData
        // newData might be null in some scenarios
        final BlockData oldData = target.getBlockData();
        if (newData != null) {
            target.setBlockData(newData);
            final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, target.getState(), placedAgainst, item, player, true, hand);
            final Material material = newData.getMaterial();

            if (againstMechanic != null && (againstMechanic.isStorage() || againstMechanic.hasClickActions()))
                blockPlaceEvent.setCancelled(true);
            if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
                blockPlaceEvent.setCancelled(true);
            if (!Range.between(target.getWorld().getMinHeight(), target.getWorld().getMaxHeight() - 1).contains(target.getY()))
                blockPlaceEvent.setCancelled(true);
            if (Tag.WOODEN_DOORS.isTagged(material) && (!target.canPlace(newData) || !target.getRelative(BlockFace.UP).isEmpty()))
                blockPlaceEvent.setCancelled(true);
            if (Tag.WOODEN_PRESSURE_PLATES.isTagged(material) && !target.canPlace(newData))
                blockPlaceEvent.setCancelled(true);

            // Call the event and check if it is cancelled, if so reset BlockData
            if (!EventUtils.callEvent(blockPlaceEvent) || !blockPlaceEvent.canBuild()) {
                target.setBlockData(oldData);
                return;
            }
        }

        // This method is run for placing on custom blocks aswell, so this should not be called for vanilla blocks
        NoteBlockMechanic targetOraxen = OraxenBlocks.getNoteBlockMechanic(newData);
        if (targetOraxen != null) {

            OraxenBlocks.place(targetOraxen.getItemID(), target.getLocation());

            OraxenNoteBlockPlaceEvent oraxenPlaceEvent = new OraxenNoteBlockPlaceEvent(targetOraxen, target, player, item, hand);
            if (!EventUtils.callEvent(oraxenPlaceEvent)) {
                target.setBlockData(oldData);
                return;
            }

            if (targetOraxen.isFalling() && target.getRelative(BlockFace.DOWN).getType().isAir()) {
                Location fallingLocation = BlockHelpers.toCenterBlockLocation(target.getLocation());
                OraxenBlocks.remove(target.getLocation(), null);
                if(fallingLocation.getNearbyEntitiesByType(FallingBlock.class, 0.25).isEmpty())
                    target.getWorld().spawnFallingBlock(fallingLocation, newData);
                handleFallingOraxenBlockAbove(target);
            }

            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            Utils.swingHand(player, hand);
        } else {
            target.setBlockData(oldData);
            BlockHelpers.correctAllBlockStates(placedAgainst, player, hand, face, item, newData);
        }
        if (VersionUtil.isPaperServer()) target.getWorld().sendGameEvent(player, GameEvent.BLOCK_PLACE, target.getLocation().toVector());
    }

    // Used to determine what instrument to use when playing a note depending on below block
    public static Map<Instrument, List<String>> instrumentMap = new HashMap<>();

    private static Map<Instrument, List<String>> getInstrumentMap() {
        Map<Instrument, List<String>> map = new HashMap<>();
        map.put(Instrument.BELL, List.of("gold_block"));
        map.put(Instrument.BASS_DRUM, Arrays.asList("stone", "netherrack", "bedrock", "observer", "coral", "obsidian", "anchor", "quartz"));
        map.put(Instrument.FLUTE, List.of("clay"));
        map.put(Instrument.CHIME, List.of("packed_ice"));
        map.put(Instrument.GUITAR, List.of("wool"));
        map.put(Instrument.XYLOPHONE, List.of("bone_block"));
        map.put(Instrument.IRON_XYLOPHONE, List.of("iron_block"));
        map.put(Instrument.COW_BELL, List.of("soul_sand"));
        map.put(Instrument.DIDGERIDOO, List.of("pumpkin"));
        map.put(Instrument.BIT, List.of("emerald_block"));
        map.put(Instrument.BANJO, List.of("hay_bale"));
        map.put(Instrument.PLING, List.of("glowstone"));
        map.put(Instrument.BASS_GUITAR, List.of("wood"));
        map.put(Instrument.SNARE_DRUM, Arrays.asList("sand", "gravel", "concrete_powder", "soul_soil"));
        map.put(Instrument.STICKS, Arrays.asList("glass", "sea_lantern", "beacon"));

        return map;
    }
}
