package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockBreakEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockPlaceEvent;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.GenericGameEvent;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class ChorusBlockSoundListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        // Run at HIGH priority (before HIGHEST physics listener) to capture mechanics
        // before blocks are set to AIR. We only play sounds here, no block modification.
        for (Block block : event.getBlocks()) {
            if (block.getType() != Material.CHORUS_PLANT) continue;
            final ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            if (mechanic == null || !mechanic.hasBlockSounds()) continue;
            // Skip immovable blocks - they won't be destroyed
            if (mechanic.isImmovable()) continue;

            BlockSounds blockSounds = mechanic.getBlockSounds();
            if (blockSounds.hasBreakSound())
                BlockHelpers.playCustomBlockSound(block.getLocation(), blockSounds.getBreakSound(),
                        blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceChorusBlock(OraxenChorusBlockPlaceEvent event) {
        final ChorusBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getPlaceSound(),
                    blockSounds.getPlaceVolume(), blockSounds.getPlacePitch());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakChorusBlock(OraxenChorusBlockBreakEvent event) {
        final ChorusBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasBreakSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getBreakSound(),
                    blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        Location entityLoc = entity.getLocation();
        if (entityLoc == null || !isLoaded(entityLoc)) return;

        GameEvent gameEvent = event.getEvent();
        if (gameEvent == null) return;
        Block block = entityLoc.getBlock();
        EntityDamageEvent cause = entity.getLastDamageCause();

        if (gameEvent == GameEvent.HIT_GROUND && cause != null
                && cause.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
        String sound;
        float volume;
        float pitch;

        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (gameEvent == GameEvent.STEP && blockSounds.hasStepSound()) {
            sound = blockSounds.getStepSound();
            volume = blockSounds.getStepVolume();
            pitch = blockSounds.getStepPitch();
        } else if (gameEvent == GameEvent.HIT_GROUND && blockSounds.hasFallSound()) {
            sound = blockSounds.getFallSound();
            volume = blockSounds.getFallVolume();
            pitch = blockSounds.getFallPitch();
        } else return;
        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

}
