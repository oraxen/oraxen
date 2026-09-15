package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.farming.bigmining.BigMiningMechanic;
import io.th0rgal.oraxen.mechanics.provided.farming.bigmining.BigMiningMechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.farming.bigmining.BigMiningMechanicListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BigMiningMechanicListenerTest extends MechanicTestSupport {

    @Test
    @SuppressWarnings("unchecked")
    void keepsRecursiveBreaksIsolatedPerThread() throws Exception {
        BigMiningMechanicFactory factory = mock(BigMiningMechanicFactory.class);
        BigMiningMechanic firstMechanic = mock(BigMiningMechanic.class);
        BigMiningMechanic secondMechanic = mock(BigMiningMechanic.class);
        when(factory.callEvents()).thenReturn(true);
        when(firstMechanic.getRadius()).thenReturn(1);
        when(firstMechanic.getDepth()).thenReturn(1);
        when(secondMechanic.getRadius()).thenReturn(1);
        when(secondMechanic.getDepth()).thenReturn(1);

        ItemStack firstItem = mock(ItemStack.class);
        ItemStack secondItem = mock(ItemStack.class);
        when(factory.getMechanic(firstItem)).thenReturn(firstMechanic);
        when(factory.getMechanic(secondItem)).thenReturn(secondMechanic);

        World world = mock(World.class);
        Block initialBlock = mock(Block.class);
        Block targetBlock = mock(Block.class);
        Block nearestBlock = mock(Block.class);
        Block secondTargetBlock = mock(Block.class);
        Location initialLocation = new Location(world, 0, 0, 0);
        when(initialBlock.getLocation()).thenReturn(initialLocation);
        when(targetBlock.isLiquid()).thenReturn(false);
        when(targetBlock.getType()).thenReturn(Material.STONE);
        when(targetBlock.getLocation()).thenReturn(new Location(world, 1, 0, 0));
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(targetBlock);
        when(world.getBlockAt(any(Location.class))).thenReturn(targetBlock);
        when(nearestBlock.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(secondTargetBlock.getLocation()).thenReturn(new Location(world, 0, 0, 1));
        when(secondTargetBlock.getFace(nearestBlock)).thenReturn(BlockFace.NORTH);

        Player firstPlayer = player(firstItem, List.of(nearestBlock, secondTargetBlock));
        Player secondPlayer = player(secondItem, List.of(nearestBlock, secondTargetBlock));
        BlockBreakEvent firstEvent = new BlockBreakEvent(initialBlock, firstPlayer);
        BlockBreakEvent secondEvent = new BlockBreakEvent(initialBlock, secondPlayer);
        BigMiningMechanicListener listener = new BigMiningMechanicListener(factory);

        Server server = Bukkit.getServer();
        PluginManager previousPluginManager = server.getPluginManager();
        when(server.getTag(anyString(), any(NamespacedKey.class), eq(Material.class)))
                .thenAnswer(invocation -> new EmptyMaterialTag());
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        CountDownLatch firstSyntheticEvent = new CountDownLatch(1);
        CountDownLatch secondBreakFinished = new CountDownLatch(1);
        AtomicBoolean firstSynthetic = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        doAnswer(invocation -> {
            BlockBreakEvent syntheticEvent = invocation.getArgument(0);
            if (firstSynthetic.compareAndSet(false, true)) {
                firstSyntheticEvent.countDown();
                if (!secondBreakFinished.await(5, TimeUnit.SECONDS))
                    throw new AssertionError("The concurrent break did not finish");
            }
            syntheticEvent.setDropItems(false);
            listener.onBlockBreak(syntheticEvent);
            return null;
        }).when(pluginManager).callEvent(any(BlockBreakEvent.class));

        Thread firstThread = new Thread(() -> {
            try {
                listener.onBlockBreak(firstEvent);
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            }
        });
        Thread secondThread = new Thread(() -> {
            try {
                listener.onBlockBreak(secondEvent);
            } catch (Throwable throwable) {
                failure.compareAndSet(null, throwable);
            } finally {
                secondBreakFinished.countDown();
            }
        });
        firstThread.setDaemon(true);
        secondThread.setDaemon(true);

        try {
            firstThread.start();
            assertTrue(firstSyntheticEvent.await(5, TimeUnit.SECONDS),
                    () -> "The first break failed: " + failure.get());
            secondThread.start();
            assertTrue(secondBreakFinished.await(5, TimeUnit.SECONDS));
            firstThread.join(5_000);
            assertFalse(firstThread.isAlive());

            Throwable thrown = failure.get();
            if (thrown != null) throw new AssertionError("Concurrent block breaks failed", thrown);
            verify(factory, times(1)).getMechanic(firstItem);
            verify(factory, times(1)).getMechanic(secondItem);
        } finally {
            when(server.getPluginManager()).thenReturn(previousPluginManager);
        }
    }

    @SuppressWarnings("unchecked")
    private static Player player(ItemStack item, List<Block> targetBlocks) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(item);
        when(player.getLastTwoTargetBlocks(nullable(Set.class), eq(5))).thenReturn(targetBlocks);
        return player;
    }

    private static final class EmptyMaterialTag implements Tag<Material> {

        @Override
        public boolean isTagged(Material material) {
            return false;
        }

        @Override
        public Set<Material> getValues() {
            return Set.of();
        }

        @Override
        public NamespacedKey getKey() {
            return NamespacedKey.minecraft("test");
        }
    }
}
