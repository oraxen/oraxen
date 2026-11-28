package io.th0rgal.oraxen.nms.handler.java21;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class BlockStatePlacementConsumptionTest {

    @BeforeAll
    static void bootstrapMinecraft() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (Bukkit.getServer() == null) {
            Server server = mock(Server.class);
            when(server.getLogger()).thenReturn(Logger.getLogger("test"));
            when(server.getTag(anyString(), any(NamespacedKey.class), any())).thenAnswer(invocation -> {
                NamespacedKey key = invocation.getArgument(1);
                @SuppressWarnings("unchecked")
                Tag<Material> tag = mock(Tag.class);
                when(tag.getValues()).thenReturn(Set.of());
                when(tag.isTagged(any(Material.class))).thenAnswer(tagCall ->
                        "shulker_boxes".equals(key.getKey()) && tagCall.getArgument(0) == Material.SHULKER_BOX);
                return tag;
            });
            Field serverField = Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, server);
        }
    }

    @Test
    void survivalPlacementConsumesHeldItemStack() {
        BlockItem tntItem = mockBlockItemConsumingOne();
        net.minecraft.world.item.ItemStack heldNmsStack = new net.minecraft.world.item.ItemStack(tntItem, 5);

        runCorrectBlockStates(Material.TNT, GameMode.SURVIVAL, heldNmsStack);

        assertEquals(4, heldNmsStack.getCount());
    }

    @Test
    void survivalPlacementConsumesLastItemInStack() {
        BlockItem tntItem = mockBlockItemConsumingOne();
        net.minecraft.world.item.ItemStack heldNmsStack = new net.minecraft.world.item.ItemStack(tntItem, 1);

        runCorrectBlockStates(Material.TNT, GameMode.SURVIVAL, heldNmsStack);

        assertEquals(0, heldNmsStack.getCount());
        assertTrue(heldNmsStack.isEmpty());
    }

    @Test
    void creativePlacementKeepsHeldItemStackCount() {
        BlockItem tntItem = mockBlockItemConsumingOne();
        net.minecraft.world.item.ItemStack heldNmsStack = new net.minecraft.world.item.ItemStack(tntItem, 5);

        runCorrectBlockStates(Material.TNT, GameMode.CREATIVE, heldNmsStack);

        assertEquals(5, heldNmsStack.getCount());
    }

    @Test
    void survivalShulkerBoxPlacementConsumesHeldItemStack() {
        BlockItem shulkerItem = mockBlockItemConsumingOne();
        net.minecraft.world.item.ItemStack heldNmsStack = new net.minecraft.world.item.ItemStack(shulkerItem, 1);

        runCorrectBlockStates(Material.SHULKER_BOX, GameMode.SURVIVAL, heldNmsStack);

        assertEquals(0, heldNmsStack.getCount());
        assertTrue(heldNmsStack.isEmpty());
    }

    private static BlockItem mockBlockItemConsumingOne() {
        BlockItem blockItem = mock(BlockItem.class);
        when(blockItem.asItem()).thenReturn(blockItem);
        when(blockItem.place(any(BlockPlaceContext.class))).thenAnswer(invocation -> {
            BlockPlaceContext context = invocation.getArgument(0);
            context.getItemInHand().consume(1, context.getPlayer());
            return InteractionResult.SUCCESS;
        });
        return blockItem;
    }

    private static void runCorrectBlockStates(Material material, GameMode gameMode,
                                              net.minecraft.world.item.ItemStack heldNmsStack) {
        ServerLevel level = mock(ServerLevel.class);
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.NOTE_BLOCK.defaultBlockState());

        ServerPlayer serverPlayer = mock(ServerPlayer.class);
        when(serverPlayer.level()).thenReturn(level);
        when(serverPlayer.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(heldNmsStack);
        when(serverPlayer.hasInfiniteMaterials()).thenReturn(gameMode == GameMode.CREATIVE);

        World world = mock(World.class);
        Block placedBlock = mock(Block.class);
        BlockData placedData = mock(BlockData.class);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(placedBlock);
        when(placedBlock.getBlockData()).thenReturn(placedData);

        CraftPlayer player = mock(CraftPlayer.class);
        when(player.getHandle()).thenReturn(serverPlayer);
        when(player.getGameMode()).thenReturn(gameMode);
        when(player.isSneaking()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);

        ItemStack bukkitMirror = mock(ItemStack.class);
        when(bukkitMirror.getType()).thenReturn(material);
        doAnswer(invocation -> {
            heldNmsStack.setCount(invocation.getArgument(0));
            return null;
        }).when(bukkitMirror).setAmount(anyInt());

        BlockHitResult hitResult = new BlockHitResult(new Vec3(0.5, 1.0, 0.5), Direction.UP, new BlockPos(0, 0, 0), false);
        NMSHandler handler = mock(NMSHandler.class, CALLS_REAL_METHODS);
        doReturn(hitResult).when(handler).getPlayerPOVHitResult(any(), any(), any());

        try (MockedStatic<CraftItemStack> craftItemStack = mockStatic(CraftItemStack.class)) {
            craftItemStack.when(() -> CraftItemStack.asNMSCopy(bukkitMirror))
                    .thenAnswer(invocation -> heldNmsStack.copy());

            handler.correctBlockStates(player, EquipmentSlot.HAND, bukkitMirror);
        }
    }
}
