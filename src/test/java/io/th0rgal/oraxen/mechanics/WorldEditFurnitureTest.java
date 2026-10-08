package io.th0rgal.oraxen.mechanics;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WorldEditFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorldEditFurnitureTest extends MechanicTestSupport {

    @BeforeAll
    static void initialiseWorldEditBlockTypes() throws Exception {
        when(Bukkit.getServer().getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = mock(Tag.class);
            when(tag.getValues()).thenReturn(Set.of());
            return tag;
        });
        // Initialise Bukkit tags before the extent tests mock Bukkit's static methods.
        Class.forName(BlockHelpers.class.getName());
        // The extent tests need two registry entries, without a running server.
        WorldEdit worldEdit = mock(WorldEdit.class);
        PlatformManager platformManager = mock(PlatformManager.class);
        when(worldEdit.getPlatformManager()).thenReturn(platformManager);
        when(platformManager.isInitialized()).thenReturn(true);
        BlockType barrier = registerBlockType("minecraft:barrier");
        BlockType air = registerBlockType("minecraft:air");
        try (var worldEditApi = mockStatic(WorldEdit.class)) {
            worldEditApi.when(WorldEdit::getInstance).thenReturn(worldEdit);
            assertSame(barrier, BlockTypes.BARRIER);
            assertSame(air, BlockTypes.AIR);
        }
    }

    private static BlockType registerBlockType(String id) {
        BlockType type = mock(BlockType.class);
        BlockState state = mock(BlockState.class);
        when(type.id()).thenReturn(id);
        when(type.getDefaultState()).thenReturn(state);
        when(state.getBlockType()).thenReturn(type);
        return BlockType.REGISTRY.register(id, type);
    }

    @ParameterizedTest
    @CsvSource({
            // Ceiling FIXED displays have a 180 degree yaw adjustment at placement.
            "FIXED, true, 90, -135, 45, DOWN",
            "FIXED, true, 90, 0, 180, DOWN",
            // A roof restriction alone does not imply a flipped display.
            "NONE, true, 0, 45, 45, UP",
            "FIXED, true, 0, 45, 45, UP",
            "FIXED, false, -90, 45, 45, UP"
    })
    void undoPreservesDisplayRotationAndUsesPlacementYawForHitboxes(
            ItemDisplay.ItemDisplayTransform transform, boolean roof, float pitch,
            float savedYaw, float placementYaw, BlockFace facing) {
        ItemDisplay display = mock(ItemDisplay.class);
        World world = mock(World.class);
        when(display.getLocation()).thenReturn(new Location(world, 0.5, 64.5, 0.5, savedYaw, pitch));
        when(display.getType()).thenReturn(EntityType.ITEM_DISPLAY);
        when(display.getItemDisplayTransform()).thenReturn(transform);
        FurnitureMechanic mechanic = mock(FurnitureMechanic.class);
        LimitedPlacing limitedPlacing = mock(LimitedPlacing.class);
        when(mechanic.hasLimitedPlacing()).thenReturn(true);
        when(mechanic.getLimitedPlacing()).thenReturn(limitedPlacing);
        when(limitedPlacing.isRoof()).thenReturn(roof);
        restore(display, mechanic);

        verify(mechanic).setEntityData(eq(display), eq(placementYaw), any(ItemStack.class), eq(facing));
        verify(display).setRotation(savedYaw, pitch);
    }

    @Test
    void undoKeepsTheSavedGrownItemAfterInitialisationReplacesItsModel() {
        ItemDisplay display = mock(ItemDisplay.class);
        when(display.getLocation()).thenReturn(new Location(mock(World.class), 0.5, 64, 0.5));
        when(display.getType()).thenReturn(EntityType.ITEM_DISPLAY);
        FurnitureMechanic mechanic = mock(FurnitureMechanic.class);
        RestoredItem item = restore(display, mechanic);
        assertSame(item.savedItem(), item.visibleItem(), "Undo must keep the snapshot's grown model");
    }

    private record RestoredItem(ItemStack savedItem, ItemStack visibleItem) {}

    private RestoredItem restore(ItemDisplay display, FurnitureMechanic mechanic) {
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        PersistentDataContainer savedData = mock(PersistentDataContainer.class);
        PersistentDataAdapterContext context = mock(PersistentDataAdapterContext.class);
        when(display.getPersistentDataContainer()).thenReturn(data);
        when(data.getAdapterContext()).thenReturn(context);
        when(context.newPersistentDataContainer()).thenReturn(savedData);
        when(data.has(new NamespacedKey(OraxenPlugin.get(), "worldedit_restore"), PersistentDataType.BYTE))
                .thenReturn(true);

        ItemStack grownItem = mock(ItemStack.class);
        ItemStack savedItem = mock(ItemStack.class);
        ItemStack initialisingItem = mock(ItemStack.class);
        ItemStack initialStageItem = mock(ItemStack.class);
        when(grownItem.clone()).thenReturn(savedItem);
        when(savedItem.clone()).thenReturn(initialisingItem);
        AtomicReference<ItemStack> visibleItem = new AtomicReference<>(grownItem);
        when(display.getItemStack()).thenAnswer(invocation -> visibleItem.get());
        doAnswer(invocation -> {
            visibleItem.set(invocation.getArgument(0));
            return null;
        }).when(display).setItemStack(any(ItemStack.class));
        // FurnitureMechanic#setEntityData applies the initial stage's model,
        // even when the restored PDC describes a fully grown plant.
        doAnswer(invocation -> {
            display.setItemStack(initialStageItem);
            return null;
        }).when(mechanic).setEntityData(eq(display), anyFloat(), any(ItemStack.class), any(BlockFace.class));

        try (var furniture = mockStatic(OraxenFurniture.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(display)).thenReturn(mechanic);
            scheduler.when(() -> SchedulerUtil.runForEntity(eq(display), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });
            new WorldEditFurniture().onEntityRestore(new EntityAddToWorldEvent(display, display.getLocation().getWorld()));
        }
        verify(data).copyTo(savedData, true);
        verify(savedData).copyTo(data, true);
        verify(savedData, never()).remove(FurnitureMechanic.STAGE_INDEX_KEY);
        verify(savedData).remove(FurnitureMechanic.SEATS_KEY);
        verify(data).remove(FurnitureMechanic.SEATS_KEY);
        return new RestoredItem(savedItem, visibleItem.get());
    }

    @Test
    void ordinaryBarrierEditsScheduleOneScanPerChunk() throws Exception {
        BlockState barrierState = BlockTypes.BARRIER.getDefaultState();
        BlockState airState = BlockTypes.AIR.getDefaultState();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        when(world.getChunkAt(anyInt(), anyInt())).thenReturn(chunk);
        Extent delegate = mock(Extent.class);
        when(delegate.getBlock(any(BlockVector3.class))).thenReturn(barrierState);
        when(delegate.setBlock(any(BlockVector3.class), eq(airState))).thenReturn(true);
        Extent extent = furnitureExtent(delegate, world);

        try (var bukkit = mockStatic(Bukkit.class);
             var blocks = mockStatic(BlockHelpers.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            blocks.when(() -> BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk)).thenReturn(Set.of());
            scheduler.when(() -> SchedulerUtil.runAtLocation(any(Location.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        // Chunk PDC is read inside the scheduled region action.
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });
            for (int x = -16; x < 16; x++) {
                for (int z = -16; z < 16; z++) {
                    assertTrue(extent.setBlock(BlockVector3.at(x, 64, z), airState));
                }
            }
            scheduler.verify(() -> SchedulerUtil.runAtLocation(any(Location.class), any(Runnable.class)), times(4));
            blocks.verify(() -> BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk), times(4));
        }
        verify(delegate, times(1024)).setBlock(any(BlockVector3.class), eq(airState));
    }

    @Test
    void furnitureInChunkStillGetsAnOwningRegionLookupBeforeReplacement() throws Exception {
        BlockState barrierState = BlockTypes.BARRIER.getDefaultState();
        BlockState airState = BlockTypes.AIR.getDefaultState();
        World world = mock(World.class);
        Chunk chunk = mock(Chunk.class);
        when(world.getChunkAt(0, 0)).thenReturn(chunk);
        Block barrier = mock(Block.class);
        when(world.getBlockAt(any(Location.class))).thenReturn(barrier);
        when(barrier.getX()).thenReturn(1);
        when(barrier.getY()).thenReturn(64);
        when(barrier.getZ()).thenReturn(2);
        FurnitureMechanic mechanic = mock(FurnitureMechanic.class);
        Extent delegate = mock(Extent.class);
        BlockVector3 position = BlockVector3.at(1, 64, 2);
        when(delegate.getBlock(position)).thenReturn(barrierState);
        Extent extent = furnitureExtent(delegate, world);

        try (var bukkit = mockStatic(Bukkit.class);
             var blocks = mockStatic(BlockHelpers.class);
             var furniture = mockStatic(OraxenFurniture.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            AtomicReference<Boolean> inRegion = new AtomicReference<>(false);
            blocks.when(() -> BlockHelpers.getBlocksWithCustomData(OraxenPlugin.get(), chunk)).thenAnswer(invocation -> {
                assertTrue(inRegion.get());
                return Set.of(barrier);
            });
            furniture.when(() -> OraxenFurniture.getFurnitureMechanic(barrier)).thenAnswer(invocation -> {
                assertTrue(inRegion.get());
                return mechanic;
            });
            scheduler.when(() -> SchedulerUtil.runAtLocation(any(Location.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        inRegion.set(true);
                        try {
                            invocation.<Runnable>getArgument(1).run();
                        } finally {
                            inRegion.set(false);
                        }
                        return null;
                    });
            extent.setBlock(position, airState);
            verify(mechanic).getBaseEntity(barrier);
            scheduler.verify(() -> SchedulerUtil.runAtLocation(any(Location.class), any(Runnable.class)), times(2));
        }
        verify(delegate).setBlock(position, airState);
    }

    private Extent furnitureExtent(Extent delegate, World world) throws Exception {
        Class<?> type = Class.forName(WorldEditFurniture.class.getName() + "$FurnitureExtent");
        var constructor = type.getDeclaredConstructor(Extent.class, World.class);
        constructor.setAccessible(true);
        return (Extent) constructor.newInstance(delegate, world);
    }
}
