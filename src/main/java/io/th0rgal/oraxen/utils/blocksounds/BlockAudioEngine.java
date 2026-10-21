package io.th0rgal.oraxen.utils.blocksounds;

import io.th0rgal.oraxen.utils.BlockHelpers;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared audio engine for custom block place, break, piston-break, step, and fall sounds,
 * eliminating duplication across NoteBlock, StringBlock, and ChorusBlock sound listeners.
 */
public final class BlockAudioEngine {

    private BlockAudioEngine() {
    }

    public static void playPlaceSound(@NotNull Location location, @Nullable BlockSounds blockSounds) {
        if (blockSounds != null && blockSounds.hasPlaceSound()) {
            BlockHelpers.playCustomBlockSound(
                    location,
                    blockSounds.getPlaceSound(),
                    blockSounds.getPlaceVolume(),
                    blockSounds.getPlacePitch()
            );
        }
    }

    public static void playBreakSound(@NotNull Location location, @Nullable BlockSounds blockSounds) {
        if (blockSounds != null && blockSounds.hasBreakSound()) {
            BlockHelpers.playCustomBlockSound(
                    location,
                    blockSounds.getBreakSound(),
                    blockSounds.getBreakVolume(),
                    blockSounds.getBreakPitch()
            );
        }
    }

    public static <M> void playPistonBreakSounds(
            @NotNull BlockPistonExtendEvent event,
            @NotNull Material targetMaterial,
            @NotNull Function<Block, M> mechanicLookup,
            @NotNull Predicate<M> isImmovable,
            @NotNull Function<M, BlockSounds> soundsExtractor
    ) {
        for (Block block : event.getBlocks()) {
            if (block.getType() != targetMaterial) continue;
            M mechanic = mechanicLookup.apply(block);
            if (mechanic == null || isImmovable.test(mechanic)) continue;
            BlockSounds sounds = soundsExtractor.apply(mechanic);
            playBreakSound(block.getLocation(), sounds);
        }
    }

    public static void handleBlockStepOrFall(
            @NotNull GenericGameEvent event,
            @NotNull Function<Block, BlockSounds> soundsLookup
    ) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        GameEvent gameEvent = event.getEvent();
        if (gameEvent != GameEvent.STEP && gameEvent != GameEvent.HIT_GROUND) return;

        Location entityLoc = entity.getLocation();
        if (entityLoc == null || !entityLoc.isWorldLoaded() || !entityLoc.isChunkLoaded()) return;

        EntityDamageEvent cause = entity.getLastDamageCause();
        if (gameEvent == GameEvent.HIT_GROUND
                && cause != null
                && cause.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Block block = entityLoc.getBlock();
        BlockSounds blockSounds = soundsLookup.apply(block);
        if (blockSounds == null) return;

        if (gameEvent == GameEvent.STEP && blockSounds.hasStepSound()) {
            BlockHelpers.playCustomBlockSound(
                    entityLoc,
                    blockSounds.getStepSound(),
                    SoundCategory.PLAYERS,
                    blockSounds.getStepVolume(),
                    blockSounds.getStepPitch()
            );
        } else if (gameEvent == GameEvent.HIT_GROUND && blockSounds.hasFallSound()) {
            BlockHelpers.playCustomBlockSound(
                    entityLoc,
                    blockSounds.getFallSound(),
                    SoundCategory.PLAYERS,
                    blockSounds.getFallVolume(),
                    blockSounds.getFallPitch()
            );
        }
    }
}
