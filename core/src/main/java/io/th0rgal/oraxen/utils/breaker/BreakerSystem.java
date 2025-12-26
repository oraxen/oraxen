package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockDamageEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock.ChorusBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import io.th0rgal.protectionlib.ProtectionLib;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;

public abstract class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    // Use thread-safe collections for Folia compatibility (concurrent region thread access)
    private final Set<Location> breakerLocations = ConcurrentHashMap.newKeySet();
    private final Map<Location, SchedulerUtil.ScheduledTask> breakerTasks = new ConcurrentHashMap<>();
    private final Map<Location, SchedulerUtil.ScheduledTask> breakerPlaySound = new ConcurrentHashMap<>();

    public abstract void registerListener();

    protected abstract void sendBlockBreak(final Player player, final Location location, final int stage) ;

    protected void handleEvent(Player player, Block block, Location location, BlockFace blockFace, World world, Runnable cancel, boolean startedDigging) {
        if (player.getGameMode() == GameMode.CREATIVE) return;

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
        FurnitureMechanic furnitureMechanic = OraxenFurniture.getFurnitureMechanic(block);
        if (block.getType() == Material.NOTE_BLOCK && noteMechanic == null) return;
        if (block.getType() == Material.TRIPWIRE && stringMechanic == null) return;
        if (block.getType() == Material.BARRIER && furnitureMechanic == null) return;

        cancel.run();

        if (startedDigging) {
            // Get these when block is started being broken to minimize checks & allow for proper damage checks later
            final Drop drop;
            if (furnitureMechanic != null)
                drop = furnitureMechanic.getDrop() != null ? furnitureMechanic.getDrop() : Drop.emptyDrop();
            else if (noteMechanic != null)
                drop = noteMechanic.getDrop() != null ? noteMechanic.getDrop() : Drop.emptyDrop();
            else if (stringMechanic != null)
                drop = stringMechanic.getDrop() != null ? stringMechanic.getDrop() : Drop.emptyDrop();
            else drop = null;

            // Use entity scheduler for player operations on Folia (player may move to different region)
            SchedulerUtil.runForEntity(player, () ->
                player.addPotionEffect(new PotionEffect(PotionUtils.getEffectType("mining_fatigue"),
                    (int) (period * 11),
                    Integer.MAX_VALUE,
                    false, false, false)));

            if (breakerLocations.contains(location)) {
                SchedulerUtil.ScheduledTask existingTask = breakerTasks.remove(location);
                if (existingTask != null) existingTask.cancel();
            }

            breakerLocations.add(location);

            // Schedule the rest on main/region thread - packet listeners run on Netty thread
            // and Bukkit events must be called synchronously on the main thread
            final HardnessModifier modifier = triggeredModifier;
            SchedulerUtil.runAtLocation(location, () -> {
                // Fire PlayerInteractEvent for plugin support (cancellation state is ignored)
                final PlayerInteractEvent playerInteractEvent =
                    new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(playerInteractEvent);

                // If the relevant damage event is cancelled, stop the breaker
                if (blockDamageEventCancelled(block, player)) {
                    stopBlockBreaker(location);
                    return;
                }

                // Methods for sending multi-barrier block-breaks
                final List<Location> furnitureBarrierLocations = furnitureBarrierLocations(furnitureMechanic, block);
                startBlockHitSound(location);

                final int[] valueHolder = {0};
                SchedulerUtil.ScheduledTask breakerTask = SchedulerUtil.runAtLocationTimer(location, period, period, () -> {
                    if (!breakerLocations.contains(location)) {
                        stopBlockBreaker(location);
                        stopBlockHitSound(location);
                        return;
                    }

                    if (item.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY) >= 5)
                        valueHolder[0] = 10;

                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                        if (entity instanceof Player viewer) {
                            if (furnitureMechanic != null) for (Location barrierLoc : furnitureBarrierLocations)
                                sendBlockBreak(viewer, barrierLoc, valueHolder[0]);
                            else sendBlockBreak(viewer, location, valueHolder[0]);
                        }
                    }

                    if (valueHolder[0]++ < 10) return;
                    if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
                        // Damage item with properties identified earlier
                        ItemUtils.damageItem(player, drop, item);
                        modifier.breakBlock(player, block, item);
                    } else stopBlockHitSound(location);

                    // Use entity scheduler for player operations on Folia (player may move to different region)
                    SchedulerUtil.runForEntity(player, () ->
                        player.removePotionEffect(PotionUtils.getEffectType("mining_fatigue")));

                    stopBlockBreaker(location);
                    stopBlockHitSound(location);
                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                        if (entity instanceof Player viewer) {
                            // Send block break animation to each viewer on their own thread
                            SchedulerUtil.runForEntity(viewer, () -> {
                                if (furnitureMechanic != null) {
                                    for (Location barrierLoc : furnitureBarrierLocations) {
                                        sendBlockBreak(viewer, barrierLoc, valueHolder[0]);
                                    }
                                } else {
                                    sendBlockBreak(viewer, location, valueHolder[0]);
                                }
                            });
                        }
                    }
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
                player.removePotionEffect(PotionUtils.getEffectType("mining_fatigue"));
                if (!ProtectionLib.canBreak(player, location))
                    player.sendBlockChange(location, block.getBlockData());
            });

            SchedulerUtil.runAtLocation(location, () -> {
                for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                    if (entity instanceof Player viewer) {
                        SchedulerUtil.runForEntity(viewer, () -> sendBlockBreak(viewer, location, 10));
                    }
                }
            });
        }
    }

    private List<Location> furnitureBarrierLocations(FurnitureMechanic furnitureMechanic, Block block) {
        if (!breakerLocations.contains(block.getLocation())) return List.of(block.getLocation());

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

        // Events must be dispatched synchronously to check cancellation status.
        // This is called from a scheduled task on the main/region thread.
        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                Bukkit.getPluginManager().callEvent(event);
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                Bukkit.getPluginManager().callEvent(event);
                return event.isCancelled();
            }
            case CHORUS_PLANT -> {
                ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
                if (mechanic == null) return true;
                OraxenChorusBlockDamageEvent event = new OraxenChorusBlockDamageEvent(mechanic, block, player);
                Bukkit.getPluginManager().callEvent(event);
                return event.isCancelled();
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null) return true;
                Entity baseEntity = mechanic.getBaseEntity(block);
                if (baseEntity == null) return true;
                OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                Bukkit.getPluginManager().callEvent(event);
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
                if (!soundSection.getBoolean("noteblock_and_block")) return null;
                else return mechanic.getBlockSounds();
            }
            case MUSHROOM_STEM -> {
                BlockMechanic mechanic = getBlockMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("noteblock_and_block")) return null;
                else return mechanic.getBlockSounds();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("stringblock_and_furniture")) return null;
                else return mechanic.getBlockSounds();
            }
            case BARRIER -> {
                FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                if (mechanic == null || !mechanic.hasBlockSounds()) return null;
                if (!soundSection.getBoolean("stringblock_and_furniture")) return null;
                else return mechanic.getBlockSounds();
            }
            default -> {
                return null;
            }
        }
    }

    private String getHitSound(Block block) {
        ConfigurationSection soundSection = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds");
        if (soundSection == null) return null;
        BlockSounds sounds = getBlockSounds(block);
        if (sounds == null) return null;
        return switch (block.getType()) {
            case NOTE_BLOCK, MUSHROOM_STEM -> sounds.hasHitSound() ? sounds.getHitSound() : "required.wood.hit";
            case TRIPWIRE -> sounds.hasHitSound() ? sounds.getHitSound() : "block.tripwire.detach";
            case BARRIER -> sounds.hasHitSound() ? sounds.getHitSound() : "required.stone.hit";
            default -> block.getBlockData().getSoundGroup().getHitSound().getKey().toString();
        };
    }
}
