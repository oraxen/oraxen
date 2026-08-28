package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockSoundListener;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundGroup;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockDamageEvent;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteBlockSoundListenerTest extends MechanicTestSupport {

    @Test
    void instaBreakIsLeftToTheBlockBreakPipeline() {
        Block block = mock(Block.class);
        BlockData blockData = mock(BlockData.class);
        SoundGroup soundGroup = mock(SoundGroup.class);
        Location location = mock(Location.class);
        BlockDamageEvent event = mock(BlockDamageEvent.class);
        when(event.getBlock()).thenReturn(block);
        when(event.getInstaBreak()).thenReturn(true);
        when(block.getType()).thenReturn(Material.NOTE_BLOCK);
        when(block.getBlockData()).thenReturn(blockData);
        when(blockData.getSoundGroup()).thenReturn(soundGroup);
        when(block.getLocation()).thenReturn(location);

        try (MockedStatic<NoteBlockMechanicFactory> factory = mockStatic(NoteBlockMechanicFactory.class);
                MockedStatic<SchedulerUtil> scheduler = mockStatic(SchedulerUtil.class)) {
            factory.when(NoteBlockMechanicFactory::isEnabled).thenReturn(true);
            factory.when(NoteBlockMechanicFactory::areCustomSoundsEnabled).thenReturn(true);

            new NoteBlockSoundListener().onHitWood(event);

            scheduler.verify(() -> SchedulerUtil.runAtLocationLater(eq(location), eq(1L), any(Runnable.class)), never());
        }

        verify(block, never()).setType(Material.AIR, false);
    }
}
