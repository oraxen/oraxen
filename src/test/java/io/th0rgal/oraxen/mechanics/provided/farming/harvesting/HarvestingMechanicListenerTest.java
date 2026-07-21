package io.th0rgal.oraxen.mechanics.provided.farming.harvesting;

import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarvestingMechanicListenerTest {

    @Test
    void movesComputedDropsToInventoryAndDropsOnlyOverflow() {
        HarvestingMechanicListener listener = new HarvestingMechanicListener(mock(MechanicFactory.class));
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        World world = mock(World.class);
        Block block = mock(Block.class);
        BlockState blockState = mock(BlockState.class);
        Item itemEntity = mock(Item.class);
        UUID playerId = UUID.randomUUID();
        Location location = new Location(world, 1, 2, 3);
        ItemStack computedDrop = mock(ItemStack.class);
        ItemStack collectedDrop = mock(ItemStack.class);
        ItemStack overflow = mock(ItemStack.class);
        List<Item> itemEntities = new ArrayList<>(List.of(itemEntity));
        BlockDropItemEvent event = mock(BlockDropItemEvent.class);

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        when(blockState.getLocation()).thenReturn(location);
        when(itemEntity.getItemStack()).thenReturn(computedDrop);
        when(computedDrop.clone()).thenReturn(collectedDrop);
        when(event.getPlayer()).thenReturn(player);
        when(event.getBlockState()).thenReturn(blockState);
        when(event.getItems()).thenReturn(itemEntities);
        HashMap<Integer, ItemStack> overflowItems = new HashMap<>();
        overflowItems.put(0, overflow);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(overflowItems);
        when(player.breakBlock(block)).thenAnswer(invocation -> {
            listener.onBlockDropItem(event);
            return true;
        });

        assertTrue(listener.breakAndCollect(player, block));
        assertTrue(itemEntities.isEmpty());
        verify(inventory).addItem(collectedDrop);
        verify(world).dropItemNaturally(location, overflow);
    }
}
