package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockDataListenerTest {

    @BeforeAll
    static void setUpServer() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Server server = (Server) serverField.get(null);
        if (server == null) {
            server = mock(Server.class);
            serverField.set(null, server);
        }
        when(server.getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(call -> {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of());
            return tag;
        });
    }

    @Test
    void explosionRemovesFurnitureBeforeClearingBlockData() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        AtomicBoolean furnitureRemoved = new AtomicBoolean();
        when(event.blockList()).thenReturn(new ArrayList<>(List.of(block)));
        when(block.getLocation()).thenReturn(location);

        try (MockedStatic<OraxenFurniture> furniture = mockStatic(OraxenFurniture.class);
             MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class);
             MockedStatic<BlockHelpers> data = mockStatic(BlockHelpers.class)) {
            furniture.when(() -> OraxenFurniture.hasFurnitureBlockMarker(block)).thenReturn(true);
            furniture.when(() -> OraxenFurniture.remove(location, null)).thenAnswer(call -> {
                furnitureRemoved.set(true);
                return true;
            });
            data.when(() -> BlockHelpers.hasPDC(block, plugin)).thenReturn(true);
            data.when(() -> BlockHelpers.removePDC(block, plugin)).thenAnswer(call -> {
                assertTrue(furnitureRemoved.get());
                return null;
            });

            new BlockDataListener(plugin).onEntityExplode(event);

            furniture.verify(() -> OraxenFurniture.remove(location, null));
            data.verify(() -> BlockHelpers.removePDC(block, plugin));
        }
    }

    @Test
    void pistonDoesNotCopyFurnitureDataToDestination() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        BlockPistonExtendEvent event = mock(BlockPistonExtendEvent.class);
        when(event.getBlocks()).thenReturn(List.of(block));
        when(block.getLocation()).thenReturn(location);

        try (MockedStatic<OraxenFurniture> furniture = mockStatic(OraxenFurniture.class);
             MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class);
             MockedStatic<BlockHelpers> data = mockStatic(BlockHelpers.class)) {
            furniture.when(() -> OraxenFurniture.hasFurnitureBlockMarker(block)).thenReturn(true);
            data.when(() -> BlockHelpers.hasPDC(block, plugin)).thenReturn(true);

            new BlockDataListener(plugin).onPistonExtend(event);

            furniture.verify(() -> OraxenFurniture.remove(location, null));
            data.verify(() -> BlockHelpers.removePDC(block, plugin));
            data.verify(() -> BlockHelpers.getPDC(block, plugin), never());
        }
    }

    @Test
    void explosionUsesCustomBlockRemovalBeforeClearingData() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        AtomicBoolean blockRemoved = new AtomicBoolean();
        when(event.blockList()).thenReturn(new ArrayList<>(List.of(block)));
        when(block.getLocation()).thenReturn(location);

        try (MockedStatic<OraxenFurniture> furniture = mockStatic(OraxenFurniture.class);
             MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class);
             MockedStatic<BlockHelpers> data = mockStatic(BlockHelpers.class)) {
            blocks.when(() -> OraxenBlocks.getOraxenBlock(location)).thenReturn(mock(Mechanic.class));
            blocks.when(() -> OraxenBlocks.remove(location, null)).thenAnswer(call -> {
                blockRemoved.set(true);
                return true;
            });
            data.when(() -> BlockHelpers.hasPDC(block, plugin)).thenReturn(true);
            data.when(() -> BlockHelpers.removePDC(block, plugin)).thenAnswer(call -> {
                assertTrue(blockRemoved.get());
                return null;
            });

            new BlockDataListener(plugin).onEntityExplode(event);

            blocks.verify(() -> OraxenBlocks.remove(location, null));
            data.verify(() -> BlockHelpers.removePDC(block, plugin));
        }
    }
}
