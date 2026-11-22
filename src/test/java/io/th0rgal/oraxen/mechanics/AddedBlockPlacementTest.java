package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.events.stringblock.OraxenStringBlockPlaceEvent;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.shaped.ShapedBlockMechanicListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanicListener;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.BlockDataListener;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AddedBlockPlacementTest extends MechanicTestSupport {

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
    @ValueSource(strings = {"success", "cancel", "denyBuild", "cancelOraxen"})
    void stackingHonorsPlacementEvents(String outcome) throws Exception {
        Block block = mock(Block.class);
        BlockState state = mock(BlockState.class);
        Tripwire oldData = mock(Tripwire.class);
        Tripwire nextData = mock(Tripwire.class);
        Player player = mock(Player.class);
        ItemStack item = mock(ItemStack.class);
        StringBlockMechanic mechanic = mock(StringBlockMechanic.class);
        when(block.getState()).thenReturn(state);
        when(block.getBlockData()).thenReturn(oldData);
        when(mechanic.getNextStackVariation(0)).thenReturn(new StringBlockMechanic.StackVariation(2, "stack"));
        when(item.getAmount()).thenReturn(3);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        List<Object> events = placementEvents(outcome);

        try (var protection = mockStatic(AntiGriefLib.class);
             var factory = mockStatic(StringBlockMechanicFactory.class)) {
            protection.when(() -> AntiGriefLib.canBuild(eq(player), any())).thenReturn(true);
            factory.when(() -> StringBlockMechanicFactory.getCode(oldData)).thenReturn(0);
            factory.when(() -> StringBlockMechanicFactory.createTripwireData(2)).thenReturn(nextData);
            var method = StringBlockMechanicListener.class.getDeclaredMethod("handleStackInteraction",
                    StringBlockMechanic.class, Block.class, Player.class, ItemStack.class,
                    EquipmentSlot.class);
            method.setAccessible(true);
            assertEquals(true, method.invoke(new StringBlockMechanicListener(), mechanic, block,
                    player, item, EquipmentSlot.HAND));
        }

        BlockPlaceEvent place = (BlockPlaceEvent) events.getFirst();
        assertSame(state, place.getBlockReplacedState());
        assertSame(block, place.getBlockAgainst());
        assertSame(EquipmentSlot.HAND, place.getHand());
        assertEquals(outcome.equals("cancel") || outcome.equals("denyBuild") ? 1 : 2, events.size());
        if (outcome.equals("success")) {
            verify(item).setAmount(2);
            verify(mechanic).runPlaceEvents(player);
            verify(block, never()).setBlockData(oldData, false);
        } else {
            verify(block).setBlockData(oldData, false);
            verify(item, never()).setAmount(anyInt());
            verify(mechanic, never()).runPlaceEvents(any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"success", "cancel", "denyBuild"})
    void doublingSlabsHonorsPlacementEvents(String outcome) throws Exception {
        Block block = mock(Block.class);
        BlockState state = mock(BlockState.class);
        Slab slab = mock(Slab.class);
        BlockData oldData = mock(BlockData.class);
        Player player = mock(Player.class);
        ItemStack item = mock(ItemStack.class);
        PlayerInteractEvent interaction = mock(PlayerInteractEvent.class);
        ShapedBlockMechanic mechanic = mock(ShapedBlockMechanic.class);
        ShapedBlockMechanicFactory factory = mock(ShapedBlockMechanicFactory.class);
        when(factory.getMechanicFromBlock(block)).thenReturn(mechanic);
        when(block.getType()).thenReturn(Material.WAXED_CUT_COPPER_SLAB);
        when(mechanic.getPlacedMaterial()).thenReturn(Material.WAXED_CUT_COPPER_SLAB);
        when(block.getState()).thenReturn(state);
        when(block.getBlockData()).thenReturn(slab);
        when(slab.getType()).thenReturn(Slab.Type.BOTTOM);
        when(slab.clone()).thenReturn(oldData);
        when(interaction.getHand()).thenReturn(EquipmentSlot.HAND);
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        when(block.getWorld()).thenReturn(world);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer chunkData = mock(PersistentDataContainer.class);
        PersistentDataContainer blockData = mock(PersistentDataContainer.class);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(chunkData);
        when(chunkData.get(any(NamespacedKey.class), eq(PersistentDataType.TAG_CONTAINER))).thenReturn(blockData);
        when(chunkData.has(any(NamespacedKey.class), eq(PersistentDataType.TAG_CONTAINER))).thenReturn(true);
        when(mechanic.getItemID()).thenReturn("custom_slab");
        when(OraxenPlugin.get().isEnabled()).thenReturn(true);
        when(item.getAmount()).thenReturn(3);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        List<Object> events = placementEvents(outcome);

        List<Runnable> delayedTasks = new ArrayList<>();
        try (var protection = mockStatic(AntiGriefLib.class);
             var scheduler = mockStatic(SchedulerUtil.class)) {
            scheduler.when(() -> SchedulerUtil.runTaskLater(any(), eq(1L), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        delayedTasks.add(invocation.getArgument(2));
                        return null;
                    });
            protection.when(() -> AntiGriefLib.canBuild(eq(player), any())).thenReturn(true);
            var method = ShapedBlockMechanicListener.class.getDeclaredMethod("handleSlabPlacement",
                    PlayerInteractEvent.class, Player.class, ItemStack.class, ShapedBlockMechanic.class,
                    Block.class, BlockFace.class, Block.class);
            method.setAccessible(true);
            assertEquals(true, method.invoke(new ShapedBlockMechanicListener(factory), interaction,
                    player, item, mechanic, block, BlockFace.UP, block));
        }

        delayedTasks.forEach(Runnable::run);
        verify(blockData).set(ShapedBlockMechanic.SHAPED_BLOCK_KEY, PersistentDataType.STRING, "custom_slab");
        verify(chunkData, never()).remove(any(NamespacedKey.class));
        assertEquals(1, events.size());
        assertSame(state, ((BlockPlaceEvent) events.getFirst()).getBlockReplacedState());
        verify(interaction).setCancelled(true);
        if (outcome.equals("success")) {
            verify(item).setAmount(2);
            verify(mechanic).runPlaceEvents(player);
            verify(block, never()).setBlockData(oldData);
        } else {
            verify(block).setBlockData(oldData);
            verify(item, never()).setAmount(anyInt());
            verify(mechanic, never()).runPlaceEvents(any());
        }
    }

    private List<Object> placementEvents(String outcome) {
        List<Object> events = new ArrayList<>();
        PluginManager manager = mock(PluginManager.class);
        when(Bukkit.getServer().getPluginManager()).thenReturn(manager);
        doAnswer(invocation -> {
            Object event = invocation.getArgument(0);
            events.add(event);
            if (event instanceof BlockPlaceEvent place) {
                if (outcome.equals("cancel")) place.setCancelled(true);
                if (outcome.equals("denyBuild")) place.setBuild(false);
                if (!place.isCancelled() && place.canBuild() && place.getBlock().getChunk() != null)
                    new BlockDataListener(OraxenPlugin.get()).onPlace(place);
            }
            if (event instanceof OraxenStringBlockPlaceEvent place && outcome.equals("cancelOraxen"))
                place.setCancelled(true);
            return null;
        }).when(manager).callEvent(any());
        return events;
    }
}
