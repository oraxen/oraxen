package io.th0rgal.oraxen.api;

import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OraxenBlocksTest {

    @Test
    void creativeBreakSuppressesNonForcedDrop() {
        Player player = mock(Player.class);
        Drop drop = mock(Drop.class);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        assertNull(OraxenBlocks.resolveDropAfterEvent(player, drop, false));
    }

    @Test
    void creativeBreakKeepsForcedDrop() {
        Player player = mock(Player.class);
        Drop drop = mock(Drop.class);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);

        assertSame(drop, OraxenBlocks.resolveDropAfterEvent(player, drop, true));
    }

    @Test
    void survivalBreakKeepsEventDrop() {
        Player player = mock(Player.class);
        Drop drop = mock(Drop.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);

        assertSame(drop, OraxenBlocks.resolveDropAfterEvent(player, drop, false));
    }
}
