package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockDamageEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockDamageEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.PotionUtils;
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
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanicFactory.getBlockMechanic;

public abstract class BreakerSystem {

    public static final List<HardnessModifier> MODIFIERS = new ArrayList<>();
    private final Map<Location, BukkitScheduler> breakerPerLocation = new HashMap<>();
    private final Map<Location, BukkitTask> breakerPlaySound = new HashMap<>();

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

            Bukkit.getScheduler().runTask(OraxenPlugin.get(), () ->
                player.addPotionEffect(new PotionEffect(PotionUtils.getEffectType("mining_fatigue"),
                    (int) (period * 11),
                    Integer.MAX_VALUE,
                    false, false, false)));

            if (breakerPerLocation.containsKey(location))
                breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());

            final BukkitScheduler scheduler = Bukkit.getScheduler();
            // Cancellation state is being ignored.
            // However still needs to be called for plugin support.
            final PlayerInteractEvent playerInteractEvent =
                new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), block, blockFace, EquipmentSlot.HAND);
            scheduler.runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(playerInteractEvent));

            breakerPerLocation.put(location, scheduler);

            // If the relevant damage event is cancelled, return
            if (blockDamageEventCancelled(block, player)) {
                stopBlockBreaker(location);
                return;
            }

            // Methods for sending multi-barrier block-breaks
            final List<Location> furnitureBarrierLocations = furnitureBarrierLocations(furnitureMechanic, block);
            final HardnessModifier modifier = triggeredModifier;
            startBlockHitSound(location);

            scheduler.runTaskTimer(OraxenPlugin.get(), new Consumer<>() {
                int value = 0;

                @Override
                public void accept(final BukkitTask bukkitTask) {
                    if (!breakerPerLocation.containsKey(location)) {
                        bukkitTask.cancel();
                        stopBlockHitSound(location);
                        return;
                    }

                    if (item.getEnchantmentLevel(EnchantmentWrapper.EFFICIENCY) >= 5)
                        value = 10;

                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                        if (entity instanceof Player viewer) {
                            if (furnitureMechanic != null) for (Location barrierLoc : furnitureBarrierLocations)
                                sendBlockBreak(viewer, barrierLoc, value);
                            else sendBlockBreak(viewer, location, value);
                        }
                    }

                    if (value++ < 10) return;
                    if (EventUtils.callEvent(new BlockBreakEvent(block, player)) && ProtectionLib.canBreak(player, location)) {
                        // Damage item with properties identified earlier
                        ItemUtils.damageItem(player, drop, item);
                        modifier.breakBlock(player, block, item);
                    } else stopBlockHitSound(location);

                    scheduler.runTask(OraxenPlugin.get(), () ->
                        player.removePotionEffect(PotionUtils.getEffectType("mining_fatigue")));

                    stopBlockBreaker(location);
                    stopBlockHitSound(location);
                    for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16)) {
                        if (entity instanceof Player viewer) {
                            if (furnitureMechanic != null) for (Location barrierLoc : furnitureBarrierLocations)
                                sendBlockBreak(viewer, barrierLoc, value);
                            else sendBlockBreak(viewer, location, value);
                        }
                    }
                    bukkitTask.cancel();
                }
            }, period, period);
        } else {
            Bukkit.getScheduler().runTask(OraxenPlugin.get(), () -> {
                player.removePotionEffect(PotionUtils.getEffectType("mining_fatigue"));
                if (!ProtectionLib.canBreak(player, location))
                    player.sendBlockChange(location, block.getBlockData());

                for (final Entity entity : world.getNearbyEntities(location, 16, 16, 16))
                    if (entity instanceof Player viewer)
                        sendBlockBreak(viewer, location, 10);
                stopBlockBreaker(location);
                stopBlockHitSound(location);
            });
        }
    }

    private List<Location> furnitureBarrierLocations(FurnitureMechanic furnitureMechanic, Block block) {
        BukkitScheduler scheduler = breakerPerLocation.get(block.getLocation());
        if (scheduler == null) return List.of(block.getLocation());

        AtomicReference<Entity> furnitureBaseEntity = new AtomicReference<>();
        scheduler.runTask(OraxenPlugin.get(), () -> furnitureBaseEntity.set(furnitureMechanic != null ? furnitureMechanic.getBaseEntity(block) : null));
        return furnitureMechanic != null && furnitureBaseEntity.get() != null
                ? furnitureMechanic.getLocations(FurnitureMechanic.getFurnitureYaw(furnitureBaseEntity.get()),
                furnitureBaseEntity.get().getLocation(), furnitureMechanic.getBarriers())
                : Collections.singletonList(block.getLocation());
    }

    private boolean blockDamageEventCancelled(Block block, Player player) {
        BukkitScheduler scheduler = breakerPerLocation.get(block.getLocation());
        if (scheduler == null) return false;

        switch (block.getType()) {
            case NOTE_BLOCK -> {
                NoteBlockMechanic mechanic = OraxenBlocks.getNoteBlockMechanic(block);
                if (mechanic == null) return true;
                OraxenNoteBlockDamageEvent event = new OraxenNoteBlockDamageEvent(mechanic, block, player);
                scheduler.runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case TRIPWIRE -> {
                StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
                if (mechanic == null) return true;
                OraxenStringBlockDamageEvent event = new OraxenStringBlockDamageEvent(mechanic, block, player);
                scheduler.runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(event));
                return event.isCancelled();
            }
            case BARRIER -> {
                try {
                    return scheduler.callSyncMethod(OraxenPlugin.get(), () -> {
                        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(block);
                        if (mechanic == null) return true;
                        Entity baseEntity = mechanic.getBaseEntity(block);
                        if (baseEntity == null) return true;
                        OraxenFurnitureDamageEvent event = new OraxenFurnitureDamageEvent(mechanic, baseEntity, player, block);
                        scheduler.runTask(OraxenPlugin.get(), () -> Bukkit.getPluginManager().callEvent(event));
                        return event.isCancelled();
                    }).get();
                } catch (Exception e) {
                    return false;
                }
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
        if (breakerPerLocation.containsKey(location)) {
            breakerPerLocation.get(location).cancelTasks(OraxenPlugin.get());
            breakerPerLocation.remove(location);
        }
    }

    private void startBlockHitSound(Location location) {
        BukkitScheduler scheduler = breakerPerLocation.get(location);
        BlockSounds blockSounds = getBlockSounds(location.getBlock());

        if (scheduler == null || blockSounds == null || !blockSounds.hasHitSound()) {
            stopBlockHitSound(location);
            return;
        }

        breakerPlaySound.put(location, scheduler.runTaskTimer(OraxenPlugin.get(),
                () -> BlockHelpers.playCustomBlockSound(location, getHitSound(location.getBlock()), blockSounds.getHitVolume(), blockSounds.getHitPitch())
                , 0L, 4L));
    }

    private void stopBlockHitSound(Location location) {
        Optional.ofNullable(breakerPlaySound.get(location)).ifPresent(BukkitTask::cancel);
        breakerPlaySound.remove(location);
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
