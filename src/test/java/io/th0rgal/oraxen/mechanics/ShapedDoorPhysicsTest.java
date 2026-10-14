package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockType;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.drops.Drop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShapedDoorPhysicsTest extends MechanicTestSupport {

    @BeforeAll
    static void initialiseBlockHelpers() throws Exception {
        when(Bukkit.getServer().getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of());
            return tag;
        });
        Class.forName(BlockHelpers.class.getName());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void unsupportedDoorRemovesBothHalvesAndDropsOnce(int eventSource) {
        Block lower = mock(Block.class);
        Block upper = mock(Block.class);
        Block support = mock(Block.class);
        Door lowerData = mock(Door.class);
        Door upperData = mock(Door.class);
        Location location = mock(Location.class);
        when(lower.getBlockData()).thenReturn(lowerData);
        when(lowerData.getHalf()).thenReturn(Bisected.Half.BOTTOM);
        when(lower.getRelative(BlockFace.UP)).thenReturn(upper);
        when(upper.getBlockData()).thenReturn(upperData);
        when(upperData.getHalf()).thenReturn(Bisected.Half.TOP);
        when(upper.getRelative(BlockFace.DOWN)).thenReturn(lower);
        when(support.getBlockData()).thenReturn(mock(BlockData.class));
        when(support.getRelative(BlockFace.UP)).thenReturn(lower);
        when(lower.getLocation()).thenReturn(location);
        when(location.getBlock()).thenReturn(lower);

        ShapedBlockMechanic mechanic = mock(ShapedBlockMechanic.class);
        ShapedBlockMechanicFactory factory = mock(ShapedBlockMechanicFactory.class);
        Drop drop = mock(Drop.class);
        when(factory.getMechanicFromBlock(lower)).thenReturn(mechanic);
        when(mechanic.getBlockType()).thenReturn(ShapedBlockType.DOOR);
        when(mechanic.getItemID()).thenReturn("test_door");
        when(mechanic.getDrop(any())).thenReturn(drop);
        PersistentDataContainer lowerPdc = mock(PersistentDataContainer.class);
        PersistentDataContainer upperPdc = mock(PersistentDataContainer.class);
        Block eventBlock = switch (eventSource) {
            case 1 -> support;
            case 2 -> upper;
            default -> lower;
        };
        BlockPhysicsEvent event = new BlockPhysicsEvent(eventBlock, lowerData);

        try (MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class, CALLS_REAL_METHODS);
             MockedStatic<BlockHelpers> helpers = mockStatic(BlockHelpers.class)) {
            helpers.when(() -> BlockHelpers.getPDC(lower)).thenReturn(lowerPdc);
            helpers.when(() -> BlockHelpers.getPDC(upper)).thenReturn(upperPdc);
            blocks.when(() -> OraxenBlocks.getShapedMechanic(lower)).thenReturn(mechanic);
            blocks.when(() -> OraxenBlocks.getShapedMechanic(upper)).thenReturn(mechanic);

            new ShapedBlockMechanicListener(factory).onCustomDoorPhysics(event);
        }

        assertEquals(eventSource != 1, event.isCancelled());
        verify(drop).spawns(eq(location), any());
        verify(upper).setType(Material.AIR, false);
        verify(upper, never()).setType(Material.AIR);
        verify(lower).setType(Material.AIR);
        verify(lowerPdc).remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);
        verify(upperPdc).remove(ShapedBlockMechanic.SHAPED_BLOCK_KEY);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void supportedCustomAndUnsupportedVanillaDoorsKeepNormalPhysics(boolean supported) {
        Block block = mock(Block.class);
        Door data = mock(Door.class);
        when(block.getBlockData()).thenReturn(data);
        when(data.getHalf()).thenReturn(Bisected.Half.BOTTOM);
        when(data.isSupported(block)).thenReturn(supported);
        ShapedBlockMechanicFactory factory = mock(ShapedBlockMechanicFactory.class);
        ShapedBlockMechanic mechanic = mock(ShapedBlockMechanic.class);
        if (supported) when(factory.getMechanicFromBlock(block)).thenReturn(mechanic);
        BlockPhysicsEvent event = new BlockPhysicsEvent(block, data);

        try (MockedStatic<OraxenBlocks> blocks = mockStatic(OraxenBlocks.class)) {
            new ShapedBlockMechanicListener(factory).onCustomDoorPhysics(event);
            blocks.verifyNoInteractions();
        }

        assertFalse(event.isCancelled());
        verify(block, never()).setType(any());
    }
}
