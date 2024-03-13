package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;
import static io.th0rgal.oraxen.utils.blocksounds.BlockSounds.*;

public class FurnitureSoundListener implements Listener {

    private final Map<Location, BukkitTask> breakerPlaySound = new HashMap<>();

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (Map.Entry<Location, BukkitTask> entry : breakerPlaySound.entrySet()) {
            if (entry.getKey().isWorldLoaded() || entry.getValue().isCancelled()) continue;
            entry.getValue().cancel();
            breakerPlaySound.remove(entry.getKey());
        }
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingStone(final BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (OraxenBlocks.isOraxenStringBlock(block)) return;
        if (block.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_STONE_PLACE) return;
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_PLACE, VANILLA_PLACE_VOLUME, VANILLA_PLACE_PITCH);
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreakingStone(final BlockBreakEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        StringBlockMechanic mechanicBelow = OraxenBlocks.getStringMechanic(block.getRelative(BlockFace.DOWN));

        if (breakerPlaySound.containsKey(location)) {
            breakerPlaySound.get(location).cancel();
            breakerPlaySound.remove(location);
        }

        if (OraxenBlocks.isOraxenStringBlock(block) || block.getType() == Material.TRIPWIRE && mechanicBelow != null && mechanicBelow.isTall()) return;
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_STONE_BREAK) return;
        if (OraxenFurniture.isFurniture(block) && block.getType() == Material.BARRIER || block.isEmpty()) return;

        if (!event.isCancelled() && ProtectionLib.canBreak(event.getPlayer(), location))
            BlockHelpers.playCustomBlockSound(location, VANILLA_STONE_BREAK, VANILLA_BREAK_VOLUME, VANILLA_BREAK_PITCH);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitStone(final BlockDamageEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (event.getInstaBreak()) return;
        if (block.getType() == Material.BARRIER || soundGroup.getHitSound() != Sound.BLOCK_STONE_HIT) return;
        if (breakerPlaySound.containsKey(location)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(location, VANILLA_STONE_HIT, VANILLA_HIT_VOLUME, VANILLA_HIT_PITCH), 2L, 4L);
        breakerPlaySound.put(location, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStopHittingStone(final BlockDamageAbortEvent event) {
        Location location = event.getBlock().getLocation();
        if (breakerPlaySound.containsKey(location)) {
            breakerPlaySound.get(location).cancel();
            breakerPlaySound.remove(location);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        if (!isLoaded(entity.getLocation())) return;

        GameEvent gameEvent = event.getEvent();
        Block blockStandingOn = BlockHelpers.getBlockStandingOn(entity);
        EntityDamageEvent cause = entity.getLastDamageCause();

        if (blockStandingOn == null || blockStandingOn.getType().isAir()) return;
        SoundGroup soundGroup = blockStandingOn.getBlockData().getSoundGroup();

        if (soundGroup.getStepSound() != Sound.BLOCK_STONE_STEP) return;
        if (gameEvent == GameEvent.HIT_GROUND && cause != null && cause.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (blockStandingOn.getType() == Material.TRIPWIRE) return;
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(blockStandingOn);

        String sound;
        float volume;
        float pitch;
        if (gameEvent == GameEvent.STEP) {
            boolean check = blockStandingOn.getType() == Material.BARRIER && mechanic != null && mechanic.hasBlockSounds() && mechanic.getBlockSounds().hasStepSound();
            sound = (check) ? mechanic.getBlockSounds().getStepSound() : VANILLA_STONE_STEP;
            volume = (check) ? mechanic.getBlockSounds().getStepVolume() : VANILLA_STEP_VOLUME;
            pitch = (check) ? mechanic.getBlockSounds().getStepPitch() : VANILLA_STEP_PITCH;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            boolean check = (blockStandingOn.getType() == Material.BARRIER && mechanic != null && mechanic.hasBlockSounds() && mechanic.getBlockSounds().hasFallSound());
            sound = (check) ? mechanic.getBlockSounds().getFallSound() : VANILLA_STONE_FALL;
            volume = (check) ? mechanic.getBlockSounds().getFallVolume() : VANILLA_FALL_VOLUME;
            pitch = (check) ? mechanic.getBlockSounds().getFallPitch() : VANILLA_FALL_PITCH;
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlacingFurniture(final OraxenFurniturePlaceEvent event) {
        final FurnitureMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBaseEntity().getLocation(), blockSounds.getPlaceSound(), blockSounds.getPlaceVolume(), blockSounds.getPlacePitch());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakingFurniture(final OraxenFurnitureBreakEvent event) {
        Location loc = event.getBlock() != null ? event.getBlock().getLocation() : event.getBaseEntity().getLocation();
        final FurnitureMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasBreakSound())
            BlockHelpers.playCustomBlockSound(loc, blockSounds.getBreakSound(), blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
    }
}
