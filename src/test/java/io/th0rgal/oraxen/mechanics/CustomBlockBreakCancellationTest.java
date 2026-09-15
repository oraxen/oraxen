package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockBreakEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener;
import io.th0rgal.oraxen.packets.PacketAdapter;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomBlockBreakCancellationTest extends MechanicTestSupport {

    @Test
    void cancelledNoteBlockBreakEventCancelsVanillaBreak() {
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        Player player = playerWithTool();
        NoteBlockMechanic mechanic = mock(NoteBlockMechanic.class);
        when(block.getLocation()).thenReturn(location);
        when(location.getBlock()).thenReturn(block);
        when(mechanic.getDrop(any())).thenReturn(mock(Drop.class));
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        cancelOraxenBreakEvents(OraxenNoteBlockBreakEvent.class);

        try (MockedStatic<OraxenPlugin> plugin = mockPlugin();
                MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(block)).thenReturn(true);
            blocks.when(() -> OraxenBlocks.getNoteBlockMechanic(block)).thenReturn(mechanic);
            blocks.when(() -> OraxenBlocks.remove(location, player)).thenCallRealMethod();
            blocks.when(() -> OraxenBlocks.remove(location, player, (Drop) null)).thenCallRealMethod();

            new NoteBlockMechanicListener().onBreakingCustomBlock(event);
        }

        verify(block, never()).setType(Material.AIR);
        assertTrue(event.isCancelled());
        assertFalse(event.isDropItems());
    }

    @Test
    void vanillaBreakIsNotCancelledByNoteBlockListener() {
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        Player player = mock(Player.class);
        when(block.getLocation()).thenReturn(location);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        try (MockedStatic<OraxenPlugin> plugin = mockPlugin();
                MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.isOraxenNoteBlock(block)).thenReturn(false);

            new NoteBlockMechanicListener().onBreakingCustomBlock(event);

            blocks.verify(() -> OraxenBlocks.remove(location, player), never());
        }

        assertFalse(event.isCancelled());
        assertTrue(event.isDropItems());
    }

    @Test
    void cancelledStringBlockBreakDoesNotDropStorageOrBreakBlock() {
        Block block = mock(Block.class);
        Block blockAbove = mock(Block.class);
        Location location = mock(Location.class);
        Player player = playerWithTool();
        StringBlockMechanic mechanic = mock(StringBlockMechanic.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getRelative(BlockFace.UP)).thenReturn(blockAbove);
        when(location.getBlock()).thenReturn(block);
        when(mechanic.isStorage()).thenReturn(true);
        when(mechanic.getDrop(any())).thenReturn(mock(Drop.class));
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        cancelOraxenBreakEvents(OraxenStringBlockBreakEvent.class);

        try (MockedStatic<OraxenPlugin> plugin = mockPlugin();
                MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.getStringMechanic(block)).thenReturn(mechanic);
            blocks.when(() -> OraxenBlocks.isOraxenStringBlock(block)).thenReturn(true);
            blocks.when(() -> OraxenBlocks.remove(location, player)).thenCallRealMethod();
            blocks.when(() -> OraxenBlocks.remove(location, player, (Drop) null)).thenCallRealMethod();

            new StringBlockMechanicListener().onBreakingCustomBlock(event);
        }

        verify(mechanic, never()).getStorage();
        verify(block, never()).setType(Material.AIR);
        assertTrue(event.isCancelled());
        assertFalse(event.isDropItems());
    }

    @Test
    void cancelledBreakBelowStringCancelsVanillaBreak() {
        Block block = mock(Block.class);
        Block blockAbove = mock(Block.class);
        Block blockBelow = mock(Block.class);
        Location location = mock(Location.class);
        Player player = mock(Player.class);
        StringBlockMechanic mechanic = mock(StringBlockMechanic.class);
        when(block.getRelative(BlockFace.UP)).thenReturn(blockAbove);
        when(block.getRelative(BlockFace.DOWN)).thenReturn(blockBelow);
        when(block.getType()).thenReturn(Material.STONE);
        when(blockAbove.getType()).thenReturn(Material.TRIPWIRE);
        when(blockAbove.getLocation()).thenReturn(location);
        when(mechanic.isFalling()).thenReturn(false);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        try (MockedStatic<OraxenPlugin> plugin = mockPlugin();
                MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            blocks.when(() -> OraxenBlocks.getStringMechanic(block)).thenReturn(null);
            blocks.when(() -> OraxenBlocks.getStringMechanic(blockBelow)).thenReturn(null);
            blocks.when(() -> OraxenBlocks.isOraxenStringBlock(blockAbove)).thenReturn(true);
            blocks.when(() -> OraxenBlocks.getStringMechanic(blockAbove)).thenReturn(mechanic);
            blocks.when(() -> OraxenBlocks.remove(location, player)).thenReturn(false);

            new StringBlockMechanicListener().onBreakingCustomBlock(event);
        }

        assertTrue(event.isCancelled());
    }

    private static Player playerWithTool() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mock(ItemStack.class));
        return player;
    }

    /**
     * Routes {@code Event#callEvent} through a mocked PluginManager that cancels the given
     * Oraxen break event, so the real {@code OraxenBlocks.remove} path is exercised and
     * must bail out before dropping storage or setting the block to air.
     */
    private static void cancelOraxenBreakEvents(Class<? extends Cancellable> eventType) {
        PluginManager pluginManager = mock(PluginManager.class);
        when(Bukkit.getServer().getPluginManager()).thenReturn(pluginManager);
        doAnswer(invocation -> {
            Object called = invocation.getArgument(0);
            if (eventType.isInstance(called)) eventType.cast(called).setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any());
    }

    private static MockedStatic<OraxenPlugin> mockPlugin() {
        OraxenPlugin plugin = mock(OraxenPlugin.class);
        when(plugin.getPacketAdapter()).thenReturn(new PacketAdapter.EmptyAdapter());
        MockedStatic<OraxenPlugin> mockedPlugin = mockStatic(OraxenPlugin.class);
        mockedPlugin.when(OraxenPlugin::get).thenReturn(plugin);
        return mockedPlugin;
    }
}
