package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.events.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.events.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.world.GenericGameEvent;

import java.util.List;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class StringBlockSoundListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        List<Block> tripwireList = event.getBlocks().stream().filter(block -> block.getType().equals(Material.TRIPWIRE)).toList();

        for (Block block : tripwireList) {
            final StringBlockMechanic mechanic = StringBlockMechanicListener.getStringMechanic(block);
            block.setType(Material.AIR, false);
            if (mechanic == null) return;
            BlockSounds blockSounds = mechanic.getBlockSounds();
            OraxenStringBlockBreakEvent stringBlockBreakEvent = new OraxenStringBlockBreakEvent(mechanic, block, null);
            OraxenPlugin.get().getServer().getPluginManager().callEvent(stringBlockBreakEvent);
            if (stringBlockBreakEvent.isCancelled()) return;

            if (mechanic.hasBlockSounds() && blockSounds.hasBreakSound())
                BlockHelpers.playCustomBlockSound(block.getLocation(), blockSounds.getBreakSound(), blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceString(OraxenStringBlockPlaceEvent event) {
        final StringBlockMechanic mechanic = event.getStringBlockMechanic();
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getPlaceSound(), blockSounds.getPlaceVolume(), blockSounds.getPlacePitch());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakString(OraxenStringBlockBreakEvent event) {
        final StringBlockMechanic mechanic = event.getStringBlockMechanic();
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (blockSounds.hasBreakSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getBreakSound(), blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        if (!isLoaded(event.getLocation())) return;
        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        StringBlockMechanic mechanic = StringBlockMechanicListener.getStringMechanic(block);
        String sound;
        float volume;
        float pitch;

        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        if (gameEvent == GameEvent.STEP && blockSounds.hasStepSound()) {
            sound = blockSounds.getStepSound();
            volume = blockSounds.getStepVolume();
            pitch = blockSounds.getStepPitch();
        }
        else if (gameEvent == GameEvent.HIT_GROUND && blockSounds.hasStepSound()) {
            sound = blockSounds.getFallSound();
            volume = blockSounds.getFallVolume();
            pitch = blockSounds.getFallPitch();
        }
        else return;
        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

}
