package io.th0rgal.oraxen.mechanics.provided.gameplay.chorusblock;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockBreakEvent;
import io.th0rgal.oraxen.api.events.chorusblock.OraxenChorusBlockPlaceEvent;
import io.th0rgal.oraxen.utils.blocksounds.BlockAudioEngine;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.world.GenericGameEvent;

public class ChorusBlockSoundListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (!ChorusBlockMechanicFactory.isEnabled() || !ChorusBlockMechanicFactory.areCustomSoundsEnabled()) return;
        BlockAudioEngine.playPistonBreakSounds(
                event,
                Material.CHORUS_PLANT,
                OraxenBlocks::getChorusMechanic,
                ChorusBlockMechanic::isImmovable,
                ChorusBlockMechanic::getBlockSounds
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceChorusBlock(OraxenChorusBlockPlaceEvent event) {
        if (!ChorusBlockMechanicFactory.isEnabled() || !ChorusBlockMechanicFactory.areCustomSoundsEnabled()) return;
        final ChorusBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockAudioEngine.playPlaceSound(event.getBlock().getLocation(), mechanic.getBlockSounds());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakChorusBlock(OraxenChorusBlockBreakEvent event) {
        if (!ChorusBlockMechanicFactory.isEnabled() || !ChorusBlockMechanicFactory.areCustomSoundsEnabled()) return;
        final ChorusBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockAudioEngine.playBreakSound(event.getBlock().getLocation(), mechanic.getBlockSounds());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        if (!ChorusBlockMechanicFactory.isEnabled() || !ChorusBlockMechanicFactory.areCustomSoundsEnabled()) return;
        BlockAudioEngine.handleBlockStepOrFall(event, block -> {
            ChorusBlockMechanic mechanic = OraxenBlocks.getChorusMechanic(block);
            return (mechanic != null && mechanic.hasBlockSounds()) ? mechanic.getBlockSounds() : null;
        });
    }

}
