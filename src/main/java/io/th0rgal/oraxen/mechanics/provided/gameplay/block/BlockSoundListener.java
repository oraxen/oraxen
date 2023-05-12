package io.th0rgal.oraxen.mechanics.provided.gameplay.block;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.blocksounds.BlockSounds;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.GenericGameEvent;

import static io.th0rgal.oraxen.utils.BlockHelpers.isLoaded;

public class BlockSoundListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStepFall(final GenericGameEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;
        Location eLoc = entity.getLocation();
        if (!isLoaded(event.getLocation()) || !isLoaded(eLoc)) return;

        GameEvent gameEvent = event.getEvent();
        Block currentBlock = entity.getLocation().getBlock();
        Block blockBelow = currentBlock.getRelative(BlockFace.DOWN);
        String sound;
        float volume;
        float pitch;

        if (!BlockHelpers.isReplaceable(currentBlock.getType()) || currentBlock.getType() == Material.TRIPWIRE) return;
        if (blockBelow.getType() != Material.MUSHROOM_STEM) return;
        final BlockMechanic mechanic = OraxenBlocks.getBlockMechanic(blockBelow);
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();

        if (gameEvent == GameEvent.STEP) {
            sound = blockSounds.hasStepSound() ? blockSounds.getStepSound() : BlockSounds.VANILLA_WOOD_STEP;
            volume = blockSounds.getStepVolume();
            pitch = blockSounds.getStepPitch();
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            sound = blockSounds.hasFallSound() ? blockSounds.getFallSound() : BlockSounds.VANILLA_WOOD_FALL;
            volume = blockSounds.getFallVolume();
            pitch = blockSounds.getFallPitch();
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlacing(final BlockPlaceEvent event) {
        BlockMechanic mechanic = OraxenBlocks.getBlockMechanic(event.getBlock());
        if (mechanic == null || !mechanic.hasBlockSounds() || !mechanic.getBlockSounds().hasPlaceSound()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
        BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getPlaceSound(), blockSounds.getPlaceVolume(), blockSounds.getPlacePitch());

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreaking(final BlockBreakEvent event) {
        BlockMechanic mechanic = OraxenBlocks.getBlockMechanic(event.getBlock());
        if (mechanic == null || !mechanic.hasBlockSounds()) return;
        BlockSounds blockSounds = mechanic.getBlockSounds();
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), blockSounds.getBreakSound(), blockSounds.getBreakVolume(), blockSounds.getBreakPitch());
    }
}
