package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.utils.blocksounds.BlockAudioEngine;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.world.GenericGameEvent;

public class StringBlockSoundListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (!StringBlockMechanicFactory.isEnabled() || !StringBlockMechanicFactory.areCustomSoundsEnabled()) return;
        BlockAudioEngine.playPistonBreakSounds(
                event,
                Material.TRIPWIRE,
                OraxenBlocks::getStringMechanic,
                StringBlockMechanic::isImmovable,
                StringBlockMechanic::getBlockSounds
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceString(OraxenStringBlockPlaceEvent event) {
        if (!StringBlockMechanicFactory.isEnabled() || !StringBlockMechanicFactory.areCustomSoundsEnabled()) return;
        final StringBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockAudioEngine.playPlaceSound(event.getBlock().getLocation(), mechanic.getBlockSounds());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakString(OraxenStringBlockBreakEvent event) {
        if (!StringBlockMechanicFactory.isEnabled() || !StringBlockMechanicFactory.areCustomSoundsEnabled()) return;
        final StringBlockMechanic mechanic = event.getMechanic();
        if (!mechanic.hasBlockSounds()) return;
        BlockAudioEngine.playBreakSound(event.getBlock().getLocation(), mechanic.getBlockSounds());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        if (!StringBlockMechanicFactory.isEnabled() || !StringBlockMechanicFactory.areCustomSoundsEnabled()) return;
        BlockAudioEngine.handleBlockStepOrFall(event, block -> {
            StringBlockMechanic mechanic = OraxenBlocks.getStringMechanic(block);
            return (mechanic != null && mechanic.hasBlockSounds()) ? mechanic.getBlockSounds() : null;
        });
    }

}
