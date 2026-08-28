package io.th0rgal.oraxen.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the piston path of {@link BlockDataListener}: Paper's
 * CraftPersistentDataContainer#copyTo hard-casts its argument to the Craft implementation, so the
 * block-PDC wrapper returned by {@link BlockHelpers#getPDC} must unwrap the target before
 * delegating. Before the fix, every piston move of a block carrying Oraxen block data threw
 * ClassCastException and aborted the MONITOR handler mid-loop.
 */
class BlockPdcCopyTest {

    @BeforeAll
    static void setUpServer() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        Server server = (Server) serverField.get(null);
        if (server == null) {
            server = mock(Server.class);
            when(server.getLogger()).thenReturn(Logger.getLogger("TestBukkit"));
            when(server.getVersion()).thenReturn("1.21.11-R0.1-SNAPSHOT");
            when(server.getBukkitVersion()).thenReturn("1.21.11-R0.1-SNAPSHOT");
            when(server.getMinecraftVersion()).thenReturn("1.21.11");
            when(server.getUnsafe()).thenReturn(mock(UnsafeValues.class));
            serverField.set(null, server);
        }
        // BlockHelpers' static initialiser reads Tag.REPLACEABLE, which resolves every Tag
        // constant through Server#getTag; answer with empty tags so the class can load.
        when(server.getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of());
            when(tag.isTagged(any())).thenReturn(false);
            return tag;
        });
    }

    @Test
    void pistonMoveCopiesBlockDataWithoutClassCastException() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("oraxen");
        // NamespacedKey(Plugin, String) resolves the namespace through Plugin#namespace(),
        // a default method that Mockito would otherwise stub to null.
        when(plugin.namespace()).thenReturn("oraxen");

        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());

        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer chunkPDC = mock(PersistentDataContainer.class);
        when(chunk.getPersistentDataContainer()).thenReturn(chunkPDC);

        Block source = block(world, chunk, 1, 64, 2);
        Block destination = block(world, chunk, 1, 64, 3);
        when(source.getRelative(BlockFace.SOUTH)).thenReturn(destination);
        when(source.getPistonMoveReaction()).thenReturn(PistonMoveReaction.MOVE);

        NamespacedKey sourceKey = NamespacedKey.fromString("oraxen:x1y64z2");
        NamespacedKey destinationKey = NamespacedKey.fromString("oraxen:x1y64z3");
        PersistentDataContainer sourceData = mock(PersistentDataContainer.class);
        PersistentDataContainer destinationData = mock(PersistentDataContainer.class);
        when(chunkPDC.has(sourceKey, PersistentDataType.TAG_CONTAINER)).thenReturn(true);
        when(chunkPDC.get(sourceKey, PersistentDataType.TAG_CONTAINER)).thenReturn(sourceData);
        when(chunkPDC.get(destinationKey, PersistentDataType.TAG_CONTAINER)).thenReturn(destinationData);
        when(destinationData.isEmpty()).thenReturn(false);

        // Emulate Paper's CraftPersistentDataContainer#copyTo, which hard-casts the target to the
        // Craft implementation: only the raw containers of this test are acceptable targets.
        doAnswer(invocation -> {
            PersistentDataContainer target = invocation.getArgument(0);
            if (target != sourceData && target != destinationData)
                throw new ClassCastException(target.getClass().getName()
                        + " cannot be cast to CraftPersistentDataContainer");
            return null;
        }).when(sourceData).copyTo(any(PersistentDataContainer.class), anyBoolean());

        BlockDataListener listener = new BlockDataListener(plugin);
        Block piston = block(world, chunk, 1, 64, 1);
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(source), BlockFace.SOUTH);

        assertDoesNotThrow(() -> listener.onPistonExtend(event));

        // Data was copied into the destination's raw container, saved back to the chunk under the
        // destination's block key, and the source entry was removed.
        verify(sourceData).copyTo(destinationData, true);
        verify(chunkPDC).set(destinationKey, PersistentDataType.TAG_CONTAINER, destinationData);
        verify(chunkPDC).remove(sourceKey);
    }

    private static Block block(World world, Chunk chunk, int x, int y, int z) {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getChunk()).thenReturn(chunk);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        return block;
    }
}
