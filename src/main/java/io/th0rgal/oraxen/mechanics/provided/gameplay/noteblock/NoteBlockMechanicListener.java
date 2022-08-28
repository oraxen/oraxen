package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.lightapi.WrappedLightAPI;
import io.th0rgal.oraxen.events.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.events.OraxenNoteBlockInteractEvent;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.breaker.BreakerSystem;
import io.th0rgal.oraxen.utils.breaker.HardnessModifier;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic.FARMBLOCK_KEY;
import static io.th0rgal.oraxen.utils.BlockHelpers.*;

public class NoteBlockMechanicListener implements Listener {
    private final MechanicFactory factory;
    private final Map<Block, BukkitTask> breakerPlaySound = new HashMap<>();

    public NoteBlockMechanicListener(final NoteBlockMechanicFactory factory) {
        this.factory = factory;
        BreakerSystem.MODIFIERS.add(getHardnessModifier());
    }

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
        if (aboveBlock.getType() == Material.NOTE_BLOCK) {
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
        NamespacedKey eventKey = NamespacedKey.minecraft("note_block_play");
        Location eLoc = block.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        // This GameEvent only exists in 1.19
        // If server is 1.18 check if its there and if not return
        // If 1.19 we can check if this event is fired
        if (!GameEvent.values().contains(GameEvent.getByKey(eventKey))) return;
        if (block.getType() != Material.NOTE_BLOCK || event.getEvent() != GameEvent.getByKey(eventKey)) return;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.NOTE_BLOCK)
            return;
        if (block.getType().isInteractable() && block.getType() != Material.NOTE_BLOCK) return;

        NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
        if (noteBlockMechanic == null) return;
        if (noteBlockMechanic.isDirectional())
            noteBlockMechanic = (NoteBlockMechanic) factory.getMechanic(noteBlockMechanic.getDirectional().getParentBlock());

        OraxenNoteBlockInteractEvent noteBlockInteractEvent = new OraxenNoteBlockInteractEvent(noteBlockMechanic, block, event.getItem(), event.getPlayer());
        OraxenPlugin.get().getServer().getPluginManager().callEvent(noteBlockInteractEvent);
        if (noteBlockInteractEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (noteBlockMechanic.hasClickActions())
            noteBlockMechanic.runClickActions(player);

        event.setCancelled(true);
        if (item == null) return;

        Block relative = block.getRelative(event.getBlockFace());
        Material type = item.getType();
        if (type == Material.AIR) return;
        if (type == Material.BUCKET && relative.isLiquid()) {
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
        if (type.hasGravity() && relative.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
            BlockData data = Bukkit.createBlockData(type);
            if (type.toString().endsWith("ANVIL")) {
                ((Directional) data).setFacing(getAnvilFacing(event.getBlockFace()));
            }
            block.getWorld().spawnFallingBlock(relative.getLocation().add(0.5, 0, 0.5), data);
            return;
        }

        if (type.isBlock())
            makePlayerPlaceBlock(player, event.getHand(), item, block, event.getBlockFace(), Bukkit.createBlockData(type));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNotePlayed(final NotePlayEvent event) {
        if (event.getInstrument() != Instrument.PIANO)
            event.setCancelled(true);
    }

    /*@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHitBlock(final BlockDamageEvent event) {
        final Block block = event.getBlock();
        if (block.getBlockData().getSoundGroup().getHitSound() != Sound.BLOCK_WOOD_HIT) return;
        if (getNoteBlockMechanic(block) != null || block.getType() == Material.MUSHROOM_STEM) return;
        BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_WOOD_HIT);
    }*/

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakingCustomBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        if (block.getType() != Material.NOTE_BLOCK || !event.isDropItems()) return;

        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (block.getType() != Material.NOTE_BLOCK || !event.isDropItems()) return;
        if (mechanic.isDirectional())
            mechanic = (NoteBlockMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock());

        OraxenNoteBlockBreakEvent noteBlockBreakEvent = new OraxenNoteBlockBreakEvent(mechanic, block, player);
        OraxenPlugin.get().getServer().getPluginManager().callEvent(noteBlockBreakEvent);
        if (noteBlockBreakEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        BlockHelpers.playCustomBlockSound(block.getLocation(), mechanic.hasBreakSound() ? mechanic.getBreakSound() : VANILLA_WOOD_BREAK);
        if (mechanic.getLight() != -1)
            WrappedLightAPI.removeBlockLight(block.getLocation());
        if (player.getGameMode() != GameMode.CREATIVE)
            mechanic.getDrop().spawns(block.getLocation(), player.getInventory().getItemInMainHand());
        event.setDropItems(false);
    }

    @EventHandler
    public void onExplosionDestroy(EntityExplodeEvent event) {
        List<Block> blockList = event.blockList().stream().filter(block -> block.getType().equals(Material.NOTE_BLOCK)).toList();
        blockList.forEach(block -> {
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
            if (noteBlockMechanic == null) return;
            if (noteBlockMechanic.isDirectional())
                noteBlockMechanic = (NoteBlockMechanic) factory.getMechanic(noteBlockMechanic.getDirectional().getParentBlock());

            noteBlockMechanic.getDrop().spawns(block.getLocation(), new ItemStack(Material.AIR));
            block.setType(Material.AIR, false);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrePlacingCustomBlock(final PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Player player = event.getPlayer();
        final Block placedAgainst = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || placedAgainst == null) return;
        if (factory.isNotImplementedIn(itemID)) return;
        if (placedAgainst.getType().isInteractable() && placedAgainst.getType() != Material.NOTE_BLOCK) return;

        // determines the new block data of the block
        NoteBlockMechanic mechanic = (NoteBlockMechanic) factory.getMechanic(itemID);
        int customVariation = mechanic.getCustomVariation();
        BlockFace face = event.getBlockFace();

        if (mechanic.isDirectional() && mechanic.getDirectional().isParentBlock()) {
            DirectionalBlock directional = mechanic.getDirectional();

            if (face == BlockFace.NORTH || face == BlockFace.SOUTH)
                customVariation = ((NoteBlockMechanic) factory.getMechanic(directional.getXBlock())).getCustomVariation();
            else if (face == BlockFace.WEST || face == BlockFace.EAST)
                customVariation = ((NoteBlockMechanic) factory.getMechanic(directional.getZBlock())).getCustomVariation();
            else if (face == BlockFace.UP || face == BlockFace.DOWN)
                customVariation = ((NoteBlockMechanic) factory.getMechanic(directional.getYBlock())).getCustomVariation();
        }

        Block placedBlock = makePlayerPlaceBlock(player, event.getHand(), event.getItem(), placedAgainst, face, NoteBlockMechanicFactory.createNoteBlockData(customVariation));
        if (placedBlock != null) {
            if (mechanic.getLight() != -1)
                WrappedLightAPI.createBlockLight(placedBlock.getLocation(), mechanic.getLight());

            if (mechanic.hasDryout() && mechanic.getDryout().isFarmBlock()) {
                final PersistentDataContainer customBlockData = new CustomBlockData(placedBlock, OraxenPlugin.get());
                customBlockData.set(FARMBLOCK_KEY, PersistentDataType.STRING, mechanic.getItemID());
            }

            BlockHelpers.playCustomBlockSound(placedBlock.getLocation(), mechanic.hasPlaceSound() ? mechanic.getPlaceSound() : VANILLA_WOOD_PLACE);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSetFire(final PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.NOTE_BLOCK) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getBlockFace() != BlockFace.UP) return;
        if (item == null) return;

        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (mechanic == null) return;
        if (mechanic.isDirectional())
            mechanic = (NoteBlockMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock());

        if (!mechanic.canIgnite()) return;
        if (item.getType() != Material.FLINT_AND_STEEL && item.getType() != Material.FIRE_CHARGE) return;
        BlockIgniteEvent igniteEvent = new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, event.getPlayer());
        Bukkit.getPluginManager().callEvent(igniteEvent);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatchFire(final BlockIgniteEvent event) {
        Block block = event.getBlock();
        NoteBlockMechanic mechanic = getNoteBlockMechanic(block);
        if (block.getType() != Material.NOTE_BLOCK || mechanic == null) return;
        if (!mechanic.canIgnite()) event.setCancelled(true);

        block.getWorld().playSound(block.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1, 1);
        block.getRelative(BlockFace.UP).setType(Material.FIRE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitWood(final BlockDamageEvent event) {
        Block block = event.getBlock();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (block.getType() == Material.NOTE_BLOCK || block.getType() == Material.MUSHROOM_STEM)return;
        if (event.getInstaBreak() || soundGroup.getHitSound() != Sound.BLOCK_WOOD_HIT) return;
        if (breakerPlaySound.containsKey(block)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_WOOD_HIT), 3L, 6L);
        breakerPlaySound.put(block, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStopHittingWood(final BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingBlock(final BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_WOOD_PLACE) return;
        if (getNoteBlockMechanic(placed) != null || placed.getType() == Material.MUSHROOM_STEM) return;

        // Play sound for wood
        BlockHelpers.playCustomBlockSound(placed.getLocation(), VANILLA_WOOD_PLACE);
        if (placed.getType() == Material.NOTE_BLOCK && !OraxenItems.exists(event.getItemInHand()))
            placed.setBlockData(Bukkit.createBlockData(Material.NOTE_BLOCK), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreakingBlock(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_WOOD_BREAK) return;
        if (getNoteBlockMechanic(block) != null || block.getType() == Material.MUSHROOM_STEM) return;

        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }

        BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_WOOD_BREAK);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Location eLoc = entity.getLocation();
        if (!isLoaded(eLoc)) return;

        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);
        SoundGroup soundGroup = blockBelow.getBlockData().getSoundGroup();

        if (soundGroup.getStepSound() != Sound.BLOCK_WOOD_STEP) return;
        if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(block.getType()) || block.getType() == Material.TRIPWIRE) return;

        NoteBlockMechanic mechanic = getNoteBlockMechanic(blockBelow);
        if (mechanic != null && mechanic.isDirectional())
            mechanic = ((NoteBlockMechanic) factory.getMechanic(mechanic.getDirectional().getParentBlock()));

        String sound;
        if (gameEvent == GameEvent.STEP) {
            sound = (blockBelow.getType() == Material.NOTE_BLOCK && mechanic != null && mechanic.hasStepSound())
                    ? mechanic.getStepSound() : VANILLA_WOOD_STEP;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            sound = (blockBelow.getType() == Material.NOTE_BLOCK && mechanic != null && mechanic.hasFallSound())
                    ? mechanic.getFallSound() : VANILLA_WOOD_FALL;
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS);
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
            NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
            if (noteBlockMechanic == null) return;

            ItemStack item;
            if (noteBlockMechanic.isDirectional())
                item = OraxenItems.getItemById(noteBlockMechanic.getDirectional().getParentBlock()).build();
            else
                item = OraxenItems.getItemById(noteBlockMechanic.getItemID()).build();
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

    private HardnessModifier getHardnessModifier() {
        return new HardnessModifier() {

            @Override
            public boolean isTriggered(final Player player, final Block block, final ItemStack tool) {
                if (block.getType() != Material.NOTE_BLOCK)
                    return false;

                NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
                if (noteBlockMechanic == null) return false;

                if (noteBlockMechanic.isDirectional())
                    noteBlockMechanic = (NoteBlockMechanic) factory.getMechanic(noteBlockMechanic.getDirectional().getParentBlock());

                return noteBlockMechanic.hasHardness;
            }

            @Override
            public void breakBlock(final Player player, final Block block, final ItemStack tool) {
                block.setType(Material.AIR);
            }

            @Override
            public long getPeriod(final Player player, final Block block, final ItemStack tool) {
                NoteBlockMechanic noteBlockMechanic = getNoteBlockMechanic(block);
                if (noteBlockMechanic == null) return 0;
                if (noteBlockMechanic.isDirectional())
                    noteBlockMechanic = (NoteBlockMechanic) factory.getMechanic(noteBlockMechanic.getDirectional().getParentBlock());

                final long period = noteBlockMechanic.getPeriod();
                double modifier = 1;
                if (noteBlockMechanic.getDrop().canDrop(tool)) {
                    modifier *= 0.4;
                    final int diff = noteBlockMechanic.getDrop().getDiff(tool);
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
        final String sound;
        final Material type = placedAgainst.getType();

        if (BlockHelpers.REPLACEABLE_BLOCKS.contains(type))
            target = placedAgainst;

        else {
            target = placedAgainst.getRelative(face);
            if (!target.getType().isAir() && !target.isLiquid() && target.getType() != Material.LIGHT) return null;
        }

        if (isStandingInside(player, target) || !ProtectionLib.canBuild(player, target.getLocation())) return null;

        final BlockData curentBlockData = target.getBlockData();
        final boolean isFlowing = (newBlock.getMaterial() == Material.WATER || newBlock.getMaterial() == Material.LAVA);
        target.setBlockData(newBlock, isFlowing);
        final BlockState currentBlockState = target.getState();

        final BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(target, currentBlockState, placedAgainst, item, player, true, hand);
        Bukkit.getPluginManager().callEvent(blockPlaceEvent);

        if (BlockHelpers.correctAllBlockStates(target, player, face, item)) blockPlaceEvent.setCancelled(true);
        if (player.getGameMode() == GameMode.ADVENTURE) blockPlaceEvent.setCancelled(true);
        if (!ProtectionLib.canBuild(player, target.getLocation()) || !blockPlaceEvent.canBuild() || blockPlaceEvent.isCancelled()) {
            target.setBlockData(curentBlockData, false); // false to cancel physic
            return null;
        }

        if (isFlowing) {
            if (newBlock.getMaterial() == Material.WATER) sound = "item.bucket.empty";
            else sound = "item.bucket.empty_" + newBlock.getMaterial().toString().toLowerCase();
        } else sound = newBlock.getSoundGroup().getPlaceSound().getKey().toString();

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            if (item.getType().toString().toLowerCase().contains("bucket")) item.setType(Material.BUCKET);
            else item.setAmount(item.getAmount() - 1);
        }

        BlockHelpers.playCustomBlockSound(target.getLocation(), sound, SoundCategory.BLOCKS);
        Utils.sendAnimation(player, hand);

        return target;
    }

    public static NoteBlockMechanic getNoteBlockMechanic(Block block) {
        if (block.getType() != Material.NOTE_BLOCK) return null;
        final NoteBlock noteBlok = (NoteBlock) block.getBlockData();
        return NoteBlockMechanicFactory
                .getBlockMechanic((noteBlok.getInstrument().getType()) * 25
                        + noteBlok.getNote().getId() + (noteBlok.isPowered() ? 400 : 0) - 26);
    }
}
