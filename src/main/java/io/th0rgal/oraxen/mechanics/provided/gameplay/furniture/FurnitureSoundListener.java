package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.events.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.events.OraxenFurniturePlaceEvent;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import io.th0rgal.protectionlib.ProtectionLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

import static io.th0rgal.oraxen.utils.BlockHelpers.*;

public class FurnitureSoundListener implements Listener {

    private final Map<Block, BukkitTask> breakerPlaySound = new HashMap<>();

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlacingStone(final BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.TRIPWIRE) return;
        if (block.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_STONE_PLACE) return;
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_PLACE, 0.8f, 0.8f);
    }

    // Play sound due to furniture/barrier custom sound replacing stone
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreakingStone(final BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.TRIPWIRE) return;
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_STONE_BREAK) return;
        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }

        if (!event.isCancelled() && ProtectionLib.canBreak(event.getPlayer(), block.getLocation()))
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), VANILLA_STONE_BREAK, 0.8f, 0.8f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitStone(final BlockDamageEvent event) {
        Block block = event.getBlock();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (event.getInstaBreak()) return;
        if (block.getType() == Material.BARRIER || soundGroup.getHitSound() != Sound.BLOCK_STONE_HIT) return;
        if (breakerPlaySound.containsKey(block)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_STONE_HIT, 0.8f, 0.8f), 2L, 4L);
        breakerPlaySound.put(block, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStopHittingStone(final BlockDamageAbortEvent event) {
        Block block = event.getBlock();
        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Location eLoc = entity.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);
        SoundGroup soundGroup = blockBelow.getBlockData().getSoundGroup();

        // Apparently water and air use stone sounds
        // Seems stone is the generic one so might be used in alot of places we don't want this to play
        if (blockBelow.getType() == Material.WATER || blockBelow.getType() == Material.AIR) return;
        if (soundGroup.getStepSound() != Sound.BLOCK_STONE_STEP) return;
        if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(block.getType()) || block.getType() == Material.TRIPWIRE) return;
        FurnitureMechanic mechanic = FurnitureListener.getFurnitureMechanic(blockBelow);

        String sound;
        boolean hasBlockSound = mechanic != null && mechanic.hasBlockSounds();
        if (gameEvent == GameEvent.STEP) {
            sound = (blockBelow.getType() == Material.BARRIER && hasBlockSound && mechanic.getBlockSounds().hasStepSound())
                    ? mechanic.getBlockSounds().getStepSound() : VANILLA_STONE_STEP;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            sound = (blockBelow.getType() == Material.BARRIER && hasBlockSound && mechanic.getBlockSounds().hasFallSound())
                    ? mechanic.getBlockSounds().getFallSound() : VANILLA_STONE_FALL;
        } else return;

        float volume = hasBlockSound ? mechanic.getBlockSounds().getVolume() : 0.8f;
        float pitch = hasBlockSound ? mechanic.getBlockSounds().getPitch() : 0.8f;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlacingFurniture(final OraxenFurniturePlaceEvent event) {
        final FurnitureMechanic mechanic = event.getFurnitureMechanic();
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getItemFrame().getLocation(), blockSounds.getPlaceSound(), blockSounds.getVolume(), blockSounds.getPitch());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakingFurniture(final OraxenFurnitureBreakEvent event) {
        Location loc = event.getBlock() != null ? event.getBlock().getLocation() : event.getItemFrame().getLocation();
        final FurnitureMechanic mechanic = event.getFurnitureMechanic();
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasBreakSound())
            BlockHelpers.playCustomBlockSound(loc, blockSounds.getBreakSound(), blockSounds.getVolume(), blockSounds.getPitch());
    }
}
