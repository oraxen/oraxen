package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockDamageEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockBreaking;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockDurability;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drives custom block-breaking (custom hardness, furniture barriers, bedrock-break) from
 * Paper's {@link BlockDamageEvent}/{@link BlockDamageAbortEvent} and renders the break
 * animation through {@link Player#sendBlockDamage(Location, float, int)}. Both events fire
 * on the owning region thread, so this is Folia-safe without any packet-library dependency.
 */
public class BreakerSystem implements Listener {

    // Re-populated by mechanic factories on every reload while block-damage
    // handlers iterate it concurrently on region threads (Folia).
    public static final List<HardnessModifier> MODIFIERS = new CopyOnWriteArrayList<>();
    // Use thread-safe collections for Folia compatibility (concurrent region thread access)
    private final Set<Location> breakerLocations = ConcurrentHashMap.newKeySet();
    private final Map<Location, SchedulerUtil.ScheduledTask> breakerTasks = new ConcurrentHashMap<>();
    private final Map<Location, SchedulerUtil.ScheduledTask> breakerPlaySound = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockDamage(final BlockDamageEvent event) {
        handleEvent(event.getPlayer(), event.getBlock(), event.getBlockFace(), () -> event.setCancelled(true), true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockDamageAbort(final BlockDamageAbortEvent event) {
        handleEvent(event.getPlayer(), event.getBlock(), BlockFace.UP, () -> {
        }, false);
    }

    private void sendBlockBreak(final Player player, final Location location, final int stage) {
        // Paper maps progress 0.0 to the "clear animation" stage; (stage + 1) / 10 maps each
        // crack stage 0-9 back to itself ((int) (9 * progress)) without ever colliding with 0.
        final float progress = stage < 0 || stage > 9 ? 0f : (stage + 1) / 10f;
        player.sendBlockDamage(location, progress, location.hashCode());
    }

    private void handleEvent(Player player, Block block, BlockFace blockFace, Runnable cancel, boolean startedDigging) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

        final Location location = block.getLocation();
        final World world = block.getWorld();

        final ItemStack item = player.getInventory().getItemInMainHand();

        HardnessModifier triggeredModifier = null;
        for (final HardnessModifier modifier : MODIFIERS) {
            if (modifier.isTriggered(player, block, item)) {
                triggeredModifier = modifier;
                break;
            }
        }
        if (triggeredModifier == null) return;
        final long period = triggeredModifier.getPeriod(player, block, item);
        if (period == 0) return;

        NoteBlockMechanic noteMechanic = OraxenBlocks.getNoteBlockMechanic(block);
        StringBlockMechanic stringMechanic = OraxenBlocks.getStringMechanic(block);
        ChorusBlockMechanic chorusMechanic = OraxenBlocks.getChorusMechanic(block);
        ShapedBlockMechanic shapedMechanic = OraxenBlocks.getShapedMechanic(block);
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (block.getType() == Material.NOTE_BLOCK && noteMechanic == null) return;
        if (block.getType() == Material.TRIPWIRE && stringMechanic == null) return;
        if (block.getType() == Material.BARRIER && furnitureMechanic == null) return;

        if (CustomBlockMiningListener.isSupported()
                && (noteMechanic != null || stringMechanic != null || shapedMechanic != null
                || chorusMechanic != null)) {
            return;
        }

        cancel.run();

        if (startedDigging) {
            // Get these when block is started being broken to minimize checks & allow for proper damage checks later
            final Drop drop;
            final BlockBreaking.DurabilityAction durabilityAction;
            if (furnitureMechanic != null) {
                drop = furnitureMechanic.getDrop() != null ? furnitureMechanic.getDrop() : Drop.emptyDrop();
                durabilityAction = null;
            } else if (noteMechanic != null) {
                drop = noteMechanic.getDrop(item) != null ? noteMechanic.getDrop(item) : Drop.emptyDrop();
                durabilityAction = noteMechanic.getDurabilityAction(item);
            } else if (stringMechanic != null) {
                drop = stringMechanic.getDrop(item) != null ? stringMechanic.getDrop(item) : Drop.emptyDrop();
                durabilityAction = stringMechanic.getDurabilityAction(item);
            } else if (chorusMechanic != null) {
                drop = chorusMechanic.getDrop(item) != null ? chorusMechanic.getDrop(item) : Drop.emptyDrop();
                durabilityAction = chorusMechanic.getDurabilityAction(item);
            } else if (shapedMechanic != null) {
                drop = shapedMechanic.getDrop(item) != null ? shapedMechanic.getDrop(item) : Drop.emptyDrop();
                durabilityAction = shapedMechanic.getDurabilityAction(item);
            } else {
                drop = null;
                durabilityAction = null;
            }

            if (breakerLocations.contains(location)) {
                SchedulerUtil.ScheduledTask existingTask = breakerTasks.remove(location);
                if (existingTask != null) existingTask.cancel();
            }

            breakerLocations.add(location);

            // Defer the rest to the next tick so the PlayerInteractEvent and Oraxen damage
            // events fire after BlockDamageEvent processing has fully completed.
            final HardnessModifier modifier = triggeredModifier;
            SchedulerUtil.runAtLocation(location, () -> {
                // Fire PlayerInteractEvent for plugin support (cancellation state is ignored)
                final PlayerInteractEvent playerInteractEvent =
                    new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
                playerInteractEvent.callEvent();

                // If the relevant damage event is cancelled, stop the breaker
                if (blockDamageEventCancelled(block, player)) {
                    stopBlockBreaker(location);
                    return;
                }

                // Methods for sending multi-barrier block-breaks
                final List<Location> furnitureBarrierLocations = furnitureBarrierLocations(furnitureMechanic, block);
                startBlockHitSound(location);

                // Vanilla per-tick dig progress the client makes on this block (1.0 = finished).
                // Unbreakable blocks (furniture barriers, bedrock) yield 0, so the guard below
                // never triggers for them.
                final float clientBreakSpeed = block.getBreakSpeed(player);

                final int[] valueHolder = {0};
                final float[] clientProgress = {0f};
                SchedulerUtil.ScheduledTask breakerTask = SchedulerUtil.runAtLocationTimer(location, period, period, () -> {
                    if (!breakerLocations.contains(location)) {
                        stopBlockBreaker(location);
                        stopBlockHitSound(location);
                        return;
                    }

                    if (item.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY) >= 5)
                        valueHolder[0] = 10;

                    // Replaces the old STOP_DESTROY_BLOCK packet handling: once the client's own
                    // (vanilla-speed) dig completes, it stops digging without ever sending the abort
                    // that fires BlockDamageAbortEvent, so this timer would otherwise keep running
                    // and break the block by itself after the player let go. Stop once the client
                    // must have finished (or quit); if the player is still holding the button, the
                    // client re-starts digging and the new BlockDamageEvent restarts this breaker.
                    clientProgress[0] += clientBreakSpeed * period;
                    if (valueHolder[0] < 10 && (!player.isOnline() || clientProgress[0] >= 1f)) {
                        stopBlockBreaker(location);
                        stopBlockHitSound(location);
                        resetBlockBreakAnimations(world, Collections.singletonList(location));
                        return;
                    }

                    sendBlockBreakToViewers(world, location,
                            furnitureMechanic != null ? furnitureBarrierLocations : Collections.singletonList(location),
                            valueHolder[0]);

                    if (valueHolder[0]++ < 10) return;
                    BlockDurability.setSuppressVanillaDamageCancellation(true);
                    boolean canBreak;
                    try {
                        canBreak = new BlockBreakEvent(block, player).callEvent() && AntiGriefLib.canBreak(player, location);
                    } finally {
                        BlockDurability.setSuppressVanillaDamageCancellation(false);
                    }
                    if (canBreak) {
                        // Damage item with properties identified earlier, unless the block mechanic configured durability itself.
                        if (durabilityAction == null) ItemUtils.damageItem(player, drop, item);
                        modifier.breakBlock(player, block, item);
                    } else stopBlockHitSound(location);

                    stopBlockBreaker(location);
                    stopBlockHitSound(location);
                    sendBlockBreakToViewers(world, location,
                            furnitureMechanic != null ? furnitureBarrierLocations : Collections.singletonList(location),
                            valueHolder[0]);
                });
                breakerTasks.put(location, breakerTask);
            });
        } else {
            // Cancel the breaker immediately to prevent race conditions.
            // This must happen synchronously before any scheduled tasks.
            stopBlockBreaker(location);
            stopBlockHitSound(location);

            // Use entity scheduler for player operations on Folia (player may move to different region)
            SchedulerUtil.runForEntity(player, () -> {
                if (!AntiGriefLib.canBreak(player, location))
                    player.sendBlockChange(location, block.getBlockData());
            });

            resetBlockBreakAnimations(world, Collections.singletonList(location));
        }
    }

    private void resetBlockBreakAnimations(World world, List<Location> locations) {
        if (locations.isEmpty()) return;

        for (Location resetLocation : locations)
            SchedulerUtil.runAtLocation(resetLocation, () ->
                    sendBlockBreakToViewers(world, resetLocation, Collections.singletonList(resetLocation), 10));
    }

    private void sendBlockBreakToViewers(World world, Location origin, List<Location> breakLocations, int stage) {
        if (breakLocations.isEmpty()) return;

        if (!VersionUtil.isFoliaServer()) {
            for (final Entity entity : world.getNearbyEntities(origin, 16, 16, 16)) {
                if (entity instanceof Player viewer) {
                    for (Location breakLocation : breakLocations)
                        sendBlockBreak(viewer, breakLocation, stage);
                }
            }
            return;
        }

        // Folia does not allow arbitrary nearby-entity scans from a region thread.
        // Hop to each player's entity scheduler before reading their location/world.
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runForEntity(viewer, () -> {
                if (!viewer.isOnline() || !viewer.getWorld().equals(world)) return;
                if (viewer.getLocation().distanceSquared(origin) > 16 * 16) return;
                for (Location breakLocation : breakLocations)
                    sendBlockBreak(viewer, breakLocation, stage);
            }, null);
        }
    }

    private List<Location> furnitureBarrierLocations(FurnitureMechanic furnitureMechanic, Block block) {
        // Get base entity directly - we're already on the correct thread context
        // (main thread for Bukkit, region thread for Folia) from block damage events.
        if (furnitureMechanic == null) return Collections.singletonList(block.getLocation());

        Entity furnitureBaseEntity = furnitureMechanic.getBaseEntity(block);
        if (furnitureBaseEntity == null) return Collections.singletonList(block.getLocation());

        return furnitureMechanic.getLocations(
                FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity),
                furnitureBaseEntity.getLocation(),
                furnitureMechanic.getBarriers());
    }

    private boolean blockDamageEventCancelled(Block block, Player player) {
        if (!breakerLocations.contains(block.getLocation())) return false;
        if (OraxenBlocks.getShapedMechanic(block) != null) return false;

        // Events must be dispatched synchronously to check cancellation status.
        // This is called from a scheduled task on the main/region thread.
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                event.callEvent();
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                event.callEvent();
                return event.isCancelled();
            }
            case CHORUS_PLANT -> {
                ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic == null) return true;
                OraxenChorusBlockDamageEvent event = new OraxenChorusBlockDamageEvent(mechanic, block, player);
                event.callEvent();
                return event.isCancelled();
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null) return true;
                Entity baseEntity = mechanic.getBaseEntity(block);
                if (baseEntity == null) return true;
                OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                event.callEvent();
                return event.isCancelled();
            }
            case BEDROCK -> { // For BedrockBreakMechanic
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    private void stopBlockBreaker(Location location) {
        breakerLocations.remove(location);
        SchedulerUtil.ScheduledTask task = breakerTasks.remove(location);
        if (task != null) task.cancel();
    }

    private void startBlockHitSound(Location location) {
        BlockSounds blockSounds = getBlockSounds(location.getBlock());

        if (!breakerLocations.contains(location) || blockSounds == null || !blockSounds.hasHitSound()) {
            stopBlockHitSound(location);
            return;
        }

        stopBlockHitSound(location);
        breakerPlaySound.put(location, SchedulerUtil.runAtLocationTimer(location, 0L, 4L,
                () -> BlockHelpers.playCustomBlockSound(location, getHitSound(location.getBlock()), blockSounds.getHitVolume(), blockSounds.getHitPitch())));
    }

    private void stopBlockHitSound(Location location) {
        Optional.ofNullable(breakerPlaySound.remove(location)).ifPresent(SchedulerUtil.ScheduledTask::cancel);
    }

    private BlockSounds getBlockSounds(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!BlockSounds.isBlockSoundEnabled(soundSection)) return null;
                else return mechanic.getBlockSounds();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!BlockSounds.isStringBlockSoundEnabled(soundSection)) return null;
                else return mechanic.getBlockSounds();
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!BlockSounds.isFurnitureSoundEnabled(soundSection)) return null;
                else return mechanic.getBlockSounds();
            }
            default -> {
                ShapedBlockMechanic mechanic = OraxenBlocks.getShapedMechanic(block);
                return mechanic != null && mechanic.hasBlockSounds() && BlockSounds.isBlockSoundEnabled(soundSection)
                        ? mechanic.getBlockSounds()
                        : null;
            }
        }
    }

    private String getHitSound(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        BlockSounds sounds = getBlockSounds(block);
        if (sounds == null) return null;
        return switch (block.getType()) {
            case NOTE_BLOCK -> sounds.hasHitSound() ? sounds.getHitSound() : "required.wood.hit";
            case TRIPWIRE -> sounds.hasHitSound() ? sounds.getHitSound() : "block.tripwire.detach";
            case BARRIER -> sounds.hasHitSound() ? sounds.getHitSound() : "required.stone.hit";
            default -> sounds.hasHitSound() ? sounds.getHitSound() : block.getBlockData().getSoundGroup().getHitSound().getKey().toString();
        };
    }
}
