package io.th0rgal.oraxen.utils.breaker;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.utils.PotionUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteBlockClientPredictionTest {

    @Test
    void fullBlockBetweenCustomNoteBlocksRequiresServerAuthoritativeBreaking() {
        final Block block = mock(Block.class);
        final Block blockAbove = mock(Block.class);
        final Block blockBelow = mock(Block.class);
        when(block.getRelative(BlockFace.UP)).thenReturn(blockAbove);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(blockBelow);

        try (MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(blockAbove)).thenReturn(true);
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(blockBelow)).thenReturn(true);

            assertTrue(BreakerSystem.hasCustomVerticalNoteBlockNeighbor(block));
        }
    }

    @Test
    void isolatedFullBlockKeepsNativeAttributeBreaking() {
        final Block block = mock(Block.class);
        final Block blockAbove = mock(Block.class);
        final Block blockBelow = mock(Block.class);
        when(block.getRelative(BlockFace.UP)).thenReturn(blockAbove);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(blockBelow);

        try (MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            assertFalse(BreakerSystem.hasCustomVerticalNoteBlockNeighbor(block));
        }
    }

    @Test
    void vanillaBlockChangeReplaysTheUnchangedCustomNeighborState() {
        final Block changedBlock = mock(Block.class);
        final Block blockAbove = mock(Block.class);
        final Block blockBelow = mock(Block.class);
        final Location aboveLocation = mock(Location.class);
        final BlockData aboveData = mock(BlockData.class);
        when(changedBlock.getRelative(BlockFace.UP)).thenReturn(blockAbove);
        when(changedBlock.getRelative(BlockFace.DOWN)).thenReturn(blockBelow);
        when(blockAbove.getLocation()).thenReturn(aboveLocation);
        when(blockAbove.getBlockData()).thenReturn(aboveData);

        try (MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(blockAbove)).thenReturn(true);
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(blockBelow)).thenReturn(false);

            final Map<Location, BlockData> updates =
                    AdjacentNoteBlockUpdateHelper.customVerticalNeighborStates(changedBlock);

            assertEquals(1, updates.size());
            assertSame(aboveData, updates.get(aboveLocation));
        }
    }

    @Test
    void suppressesClientMiningWithoutChangingServerPotionEffects() {
        final Player player = mock(Player.class);
        final ArgumentCaptor<PotionEffect> effects = ArgumentCaptor.forClass(PotionEffect.class);

        ClientSideBlockBreakSuppressor.suppress(player);

        verify(player, org.mockito.Mockito.times(2)).sendPotionEffectChange(org.mockito.ArgumentMatchers.eq(player), effects.capture());
        final List<PotionEffectType> effectTypes = effects.getAllValues().stream().map(PotionEffect::getType).toList();
        assertTrue(effectTypes.stream().anyMatch(type -> type.getKey().getKey().equals("mining_fatigue")));
        assertTrue(effectTypes.stream().anyMatch(type -> type.getKey().getKey().equals("haste")));
        verify(player, org.mockito.Mockito.never()).addPotionEffect(org.mockito.ArgumentMatchers.any(PotionEffect.class));
    }

    @Test
    void restoresThePlayersRealPotionEffectAfterBreaking() {
        final Player player = mock(Player.class);
        final PotionEffectType miningFatigue = PotionUtils.getEffectType("mining_fatigue");
        final PotionEffect realEffect = new PotionEffect(miningFatigue, 200, 1);
        when(player.getPotionEffect(argThat(type -> type.getKey().getKey().equals("mining_fatigue"))))
                .thenReturn(realEffect);

        ClientSideBlockBreakSuppressor.restore(player);

        verify(player).sendPotionEffectChangeRemove(org.mockito.ArgumentMatchers.eq(player),
                argThat(type -> type.getKey().getKey().equals("mining_fatigue")));
        verify(player).sendPotionEffectChange(player, realEffect);
    }
}
