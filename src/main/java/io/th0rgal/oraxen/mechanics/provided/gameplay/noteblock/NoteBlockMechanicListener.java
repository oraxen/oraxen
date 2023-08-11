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
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.GameMode;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.th0rgal.oraxen.utils.BlockHelpers.getAnvilFacing;
import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class NoteBlockMechanicListener implements Listener {

    public NoteBlockMechanicListener() {
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
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
        // TODO try and fix these and not just cancel them
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
            final Block aboveBlock = event.getBlock().getRelative(BlockFace.UP);
            final Block belowBlock = event.getBlock().getRelative(BlockFace.DOWN);
            // If block below is NoteBlock, it will be affected by the break
            // Call updateAndCheck from it to fix vertical stack of NoteBlocks
            // if belowBlock is not a NoteBlock we must ensure the above is not, if it is call updateAndCheck from block
            if (belowBlock.getType() == Material.NOTE_BLOCK) {
                updateAndCheck(belowBlock.getLocation());
                event.setCancelled(true);
            } else if (aboveBlock.getType() == Material.NOTE_BLOCK) {
                updateAndCheck(event.getBlock().getLocation());
                event.setCancelled(true);
            }
            if (event.getBlock().getType() == Material.NOTE_BLOCK) {
                event.setCancelled(true);
                event.getBlock().getState().update(true, false);
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
            if (!VersionUtil.isSupportedVersionOrNewer(VersionUtil.v1_19_R1)) return;
        if (event.getEvent() != GameEvent.NOTE_BLOCK_PLAY) return;
        if (block.getType() != Material.NOTE_BLOCK) return;
            NoteBlock data = (NoteBlock) block.getBlockData().clone();
            Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> block.setBlockData(data, false), 1L);
        }

        public void updateAndCheck(final Location loc) {
            final Block block = loc.add(0, 1, 0).getBlock();
            if (block.getType() == Material.NOTE_BLOCK)
                block.getState().update(true, true);
            final Location nextBlock = block.getLocation().add(0, 1, 0);
            if (nextBlock.getBlock().getType() == Material.NOTE_BLOCK)
                updateAndCheck(block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void callInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock().getType() != Material.NOTE_BLOCK) return;
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        OraxenNoteBlockInteractEvent oraxenEvent = new OraxenNoteBlockInteractEvent(mechanic, event.getPlayer(), event.getItem(), event.getHand(), block, event.getBlockFace());
        io.th0rgal.oraxen.api.events.OraxenNoteBlockInteractEvent deprecatedOraxenEvent = new io.th0rgal.oraxen.api.events.OraxenNoteBlockInteractEvent(mechanic, event.getPlayer(), event.getItem(), event.getHand(), block, event.getBlockFace());
        Bukkit.getPluginManager().callEvent(oraxenEvent);
        Bukkit.getPluginManager().callEvent(deprecatedOraxenEvent);
        if (oraxenEvent.isCancelled() || deprecatedOraxenEvent.isCancelled()) event.setCancelled(true);
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

        if (limitedPlacing.isNotPlacableOn(block, blockFace)) {
            event.setCancelled(true);
        } else if (limitedPlacing.isRadiusLimited()) {
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

        if (hand == null || event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK)
            return;
        if (!player.isSneaking() && BlockHelpers.isInteractable(block)) return;

        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (mechanic.isDirectional() && !mechanic.getDirectional().isParentBlock())
            mechanic = mechanic.getDirectional().getParentMechanic();

        OraxenNoteBlockInteractEvent noteBlockInteractEvent = new OraxenNoteBlockInteractEvent(mechanic, player, item, hand, block, blockFace);
        io.th0rgal.oraxen.api.events.OraxenNoteBlockInteractEvent deprecatednoteBlockInteractEvent = new io.th0rgal.oraxen.api.events.OraxenNoteBlockInteractEvent(mechanic, player, item, hand, block, blockFace);
        Bukkit.getPluginManager().callEvent(noteBlockInteractEvent);
        Bukkit.getPluginManager().callEvent(deprecatednoteBlockInteractEvent);
        event.setUseInteractedBlock(Event.Result.DENY);
        if (noteBlockInteractEvent.isCancelled() || deprecatednoteBlockInteractEvent.isCancelled()) event.setCancelled(true);
        if (item == null) return;

        Block relative = block.getRelative(blockFace);
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (type == Material.BUCKET && relative.getBlockData() instanceof Levelled levelled && levelled.getLevel() == levelled.getMaximumLevel()) {
            final Sound sound;
            if (relative.getType() == Material.WATER) sound = Sound.ITEM_BUCKET_FILL;
            else sound = Sound.valueOf("ITEM_BUCKET_FILL_" + relative.getType());

            if (player.getGameMode() != GameMode.CREATIVE)
                item.setType(Objects.requireNonNull(Material.getMaterial(relative.getType() + "_BUCKET")));

            player.playSound(relative.getLocation(), sound, 1.0f, 1.0f);
            relative.setType(Material.AIR, true);
            return;
        }

        final boolean bucketCheck = type.toString().endsWith("_BUCKET");
        final String bucketBlock = type.toString().replace("_BUCKET", "");
        EntityType bucketEntity;
        try {
            bucketEntity = EntityType.valueOf(bucketBlock);
        } catch (IllegalArgumentException e) {
            bucketEntity = null;
        }

        if (bucketCheck && type != Material.MILK_BUCKET) {
            if (bucketEntity == null)
                type = Material.getMaterial(bucketBlock);
            else {
                type = Material.WATER;
                player.getWorld().spawnEntity(relative.getLocation().add(0.5, 0.0, 0.5), bucketEntity);
            }
        }

        if (type == null) return;
        if (type.hasGravity() && relative.getRelative(BlockFace.DOWN).getType().isAir()) {
            BlockData data = type.createBlockData();
            if (type.toString().endsWith("ANVIL")) ((Directional) data).setFacing(getAnvilFacing(event.getBlockFace()));
            BlockHelpers.playCustomBlockSound(relative.getLocation(), data.getSoundGroup().getPlaceSound().getKey().toString(), data.getSoundGroup().getVolume(), data.getSoundGroup().getPitch());
            block.getWorld().spawnFallingBlock(BlockHelpers.toCenterBlockLocation(relative.getLocation()), data);
            if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
            return;
        }
        if (!type.isBlock()) return;

        makePlayerPlaceBlock(player, event.getHand(), item, block, event.getBlockFace(), Bukkit.createBlockData(type));
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
        boolean isFalling = mechanic.isFalling();
        BlockFace face = event.getBlockFace();

        if (mechanic.isDirectional()) {
            DirectionalBlock directional = mechanic.getDirectional();
            if (!directional.isParentBlock()) {
                directional = directional.getParentMechanic().getDirectional();
            }

            customVariation = directional.getDirectionVariation(face, player);
        }

        BlockData data = NoteBlockMechanicFactory.createNoteBlockData(customVariation);
        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(), placedAgainst, face, data);
        if (placedBlock != null) {
            if (isFalling && face != BlockFace.DOWN && placedAgainst.getRelative(face).getRelative(BlockFace.DOWN).getType().isAir()) {
                // We place it above first to see if all checks for placing pass
                placedBlock.setType(Material.AIR, false);
                Location spawnLoc = BlockHelpers.toCenterBlockLocation(placedAgainst.getRelative(face).getLocation());
                player.getWorld().spawnFallingBlock(spawnLoc, data);
            } //else OraxenBlocks.place(mechanic.getItemID(), placedBlock.getLocation());
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    // If block is not a custom block, play the correct sound according to the below block or default
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(final NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO) event.setCancelled(true);
        else {
            if (instrumentMap.isEmpty()) instrumentMap = getInstrumentMap();
            String blockType = event.getBlock().getRelative(BlockFace.DOWN).getType().toString().toLowerCase();
            Instrument fakeInstrument = instrumentMap.entrySet().stream().filter(e -> e.getValue().contains(blockType)).map(Map.Entry::getKey).findFirst().orElse(Instrument.PIANO);
            // This is deprecated, but seems to be without reason
            event.setInstrument(fakeInstrument);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();

        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (OraxenBlocks.remove(block.getLocation(), event.getPlayer())) {
            event.setDropItems(false);
        }
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.NOTE_BLOCK)).toList();
        blockList.forEach(block -> {
            NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
            if (mechanic != null)
                OraxenBlocks.remove(block.getLocation(), null);
        });
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

        BlockIgniteEvent igniteEvent = new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, event.getPlayer());
        Bukkit.getPluginManager().callEvent(igniteEvent);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(final BlockIgniteEvent event) {
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
        if (block.getType() != Material.NOTE_BLOCK || mechanic == null) return;
        if (!mechanic.canIgnite()) event.setCancelled(true);

        block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
        block.getRelative(BlockFace.UP).setType(Material.FIRE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(final BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_WOOD_PLACE) return;
        if (OraxenBlocks.getNoteBlockMechanic(placed) != null || placed.getType() == Material.MUSHROOM_STEM) return;

        if (placed.getType() == Material.NOTE_BLOCK && !OraxenItems.exists(event.getItemInHand()))
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

    public Block makePlayerPlaceBlock(final Player player, final EquipmentSlot hand, final ItemStack item,
                                      final Block placedAgainst, final BlockFace face, final BlockData newBlock) {
        final Block target;
        final String sound;
        final Material type = placedAgainst.getType();
        final boolean waterloggedBefore = placedAgainst.getRelative(face).getType() == Material.WATER;

        if (BlockHelpers.isReplaceable(type))
            target = placedAgainst;
        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
        }

        final BlockData curentBlockData = target.getBlockData();
        target.setBlockData(newBlock);
        final BlockState currentBlockState = target.getState();
        final NoteBlockMechanic againstMechanic = OraxenBlocks.getNoteBlockMechanic(placedAgainst);

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (BlockHelpers.correctAllBlockStates(target, player, face, item, waterloggedBefore)) blockPlaceEvent.setCancelled(true);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (againstMechanic != null && (againstMechanic.isStorage() || againstMechanic.hasClickActions()))
            blockPlaceEvent.setCancelled(true);
        if (BlockHelpers.isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation()))
            blockPlaceEvent.setCancelled(true);
        if (!blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData);
            return null;
        }

        // This method is ran for placing on custom blocks aswell, so this should not be called for vanilla blocks
        if (OraxenBlocks.isOraxenNoteBlock(target)) {
            final OraxenNoteBlockPlaceEvent oraxenPlaceEvent = new OraxenNoteBlockPlaceEvent(OraxenBlocks.getNoteBlockMechanic(target), target, player, item, hand);
            final io.th0rgal.oraxen.api.events.OraxenNoteBlockPlaceEvent deprecatedOraxenPlaceEvent = new io.th0rgal.oraxen.api.events.OraxenNoteBlockPlaceEvent(OraxenBlocks.getNoteBlockMechanic(target), target, player, item, hand);
            Bukkit.getPluginManager().callEvent(oraxenPlaceEvent);
            Bukkit.getPluginManager().callEvent(deprecatedOraxenPlaceEvent);
            if (oraxenPlaceEvent.isCancelled() || deprecatedOraxenPlaceEvent.isCancelled()) {
                target.setBlockData(curentBlockData); // false to cancel physic
                return null;
            }
        }

        if (newBlock.getMaterial() == Material.WATER || newBlock.getMaterial() == Material.LAVA) {
            if (newBlock.getMaterial() == Material.WATER) sound = "item.bucket.empty";
            else sound = "item.bucket.empty_" + newBlock.getMaterial().toString().toLowerCase();
        } else if (!OraxenBlocks.isOraxenBlock(target)) sound = target.getBlockData().getSoundGroup().getPlaceSound().getKey().toString();
        else sound = null;

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (item.getType().toString().toLowerCase().contains("bucket")) item.setType(Material.BUCKET);
            else item.setAmount(item.getAmount() - 1);
        }

        if (sound != null) BlockHelpers.playCustomBlockSound(target.getLocation(), sound, SoundCategory.BLOCKS, 0.8f, 0.8f);
        Utils.swingHand(player, hand);

        return target;
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
