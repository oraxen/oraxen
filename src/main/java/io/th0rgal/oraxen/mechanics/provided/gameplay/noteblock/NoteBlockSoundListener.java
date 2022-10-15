package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.events.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.events.OraxenNoteBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.BlockHelpers;
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

public class NoteBlockSoundListener implements Listener {
    private final MechanicFactory factory;
    private final Map<Block, BukkitTask> breakerPlaySound = new HashMap<>();

    public NoteBlockSoundListener(final NoteBlockMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlacingWood(final BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getBlockData().getSoundGroup().getPlaceSound() != Sound.BLOCK_WOOD_PLACE) return;
        if (NoteBlockMechanicListener.getNoteBlockMechanic(placed) != null || placed.getType() == Material.MUSHROOM_STEM) return;

        // Play sound for wood
        BlockHelpers.playCustomBlockSound(placed.getLocation(), VANILLA_WOOD_PLACE);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreakingWood(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (block.getBlockData().getSoundGroup().getBreakSound() != Sound.BLOCK_WOOD_BREAK) return;
        if (NoteBlockMechanicListener.getNoteBlockMechanic(block) != null || block.getType() == Material.MUSHROOM_STEM) return;

        if (breakerPlaySound.containsKey(block)) {
            breakerPlaySound.get(block).cancel();
            breakerPlaySound.remove(block);
        }

        if (!event.isCancelled() && ProtectionLib.canBreak(event.getPlayer(), block.getLocation()))
            BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_WOOD_BREAK);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHitWood(final BlockDamageEvent event) {
        Block block = event.getBlock();
        SoundGroup soundGroup = block.getBlockData().getSoundGroup();

        if (block.getType() == Material.NOTE_BLOCK || block.getType() == Material.MUSHROOM_STEM) {
            if (event.getInstaBreak()) Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                block.setType(Material.AIR, false);
            }, 1);
            return;
        }
        if (soundGroup.getHitSound() != Sound.BLOCK_WOOD_HIT) return;
        if (breakerPlaySound.containsKey(block)) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), () ->
                BlockHelpers.playCustomBlockSound(block.getLocation(), VANILLA_WOOD_HIT), 2L, 4L);
        breakerPlaySound.put(block, task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStopHittingWood(final BlockDamageAbortEvent event) {
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
        if (!isLoaded(eLoc)) return;

        GameEvent gameEvent = event.getEvent();
        Block block = entity.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);

        if (blockBelow.getBlockData().getSoundGroup().getStepSound() != Sound.BLOCK_WOOD_STEP) return;
        if (!BlockHelpers.REPLACEABLE_BLOCKS.contains(block.getType()) || block.getType() == Material.TRIPWIRE) return;
        NoteBlockMechanic mechanic = NoteBlockMechanicListener.getNoteBlockMechanic(blockBelow);
        if (mechanic != null && mechanic.isDirectional())
            mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);

        String sound;
        if (gameEvent == GameEvent.STEP) {
            sound = (blockBelow.getType() == Material.NOTE_BLOCK && mechanic != null && mechanic.hasStepSound())
                    ? mechanic.getStepSound() : VANILLA_WOOD_STEP;
        } else if (gameEvent == GameEvent.HIT_GROUND) {
            sound = (blockBelow.getType() == Material.NOTE_BLOCK && mechanic != null && mechanic.hasFallSound())
                    ? mechanic.getFallSound() : VANILLA_WOOD_FALL;
        } else return;

        BlockHelpers.playCustomBlockSound(entity.getLocation(), sound, SoundCategory.PLAYERS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlacing(final OraxenNoteBlockPlaceEvent event) {
        NoteBlockMechanic mechanic = event.getNoteBlockMechanic();
        if (mechanic != null && mechanic.isDirectional())
            mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);

        if (mechanic != null && mechanic.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), mechanic.getPlaceSound());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreaking(final OraxenNoteBlockBreakEvent event) {
        NoteBlockMechanic mechanic = event.getNoteBlockMechanic();
        if (mechanic != null && mechanic.isDirectional())
            mechanic = mechanic.getDirectional().getParentBlockMechanic(mechanic);
        if (mechanic != null && mechanic.hasPlaceSound())
            BlockHelpers.playCustomBlockSound(event.getBlock().getLocation(), mechanic.getBreakSound());
    }
}
