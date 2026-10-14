package io.th0rgal.oraxen.utils.timers;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimersFactoryTest {

    @Test
    void reusesTheSameTimerPerPlayer() {
        TimersFactory factory = new TimersFactory(1000L);
        Player player = player(UUID.randomUUID());
        Player otherPlayer = player(UUID.randomUUID());

        Timer timer = factory.getTimer(player);

        assertSame(timer, factory.getTimer(player));
        assertNotSame(timer, factory.getTimer(otherPlayer));
    }

    @Test
    void concurrentLookupsShareASingleTimer() throws InterruptedException {
        TimersFactory factory = new TimersFactory(1000L);
        Player player = player(UUID.randomUUID());
        int threads = 8;
        AtomicReferenceArray<Timer> timers = new AtomicReferenceArray<>(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            for (int i = 0; i < threads; i++) {
                int index = i;
                executor.execute(() -> {
                    try {
                        start.await();
                        timers.set(index, factory.getTimer(player));
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        Timer expected = timers.get(0);
        for (int i = 1; i < threads; i++) {
            assertSame(expected, timers.get(i));
        }
    }

    @Test
    void saturatesTimerExpiryWhenDelayWouldOverflow() {
        TimersFactory factory = new TimersFactory(Long.MAX_VALUE);

        assertEquals(0, factory.cachedTimerCount());
    }

    @Test
    void evictsTimersOnceTheirCooldownCannotBeRunning() throws InterruptedException {
        TimersFactory factory = new TimersFactory(0L, java.time.Duration.ofMillis(20));
        Player player = player(UUID.randomUUID());

        Timer timer = factory.getTimer(player);
        assertEquals(1, factory.cachedTimerCount());

        Thread.sleep(80);

        assertNotSame(timer, factory.getTimer(player));
        assertEquals(1, factory.cachedTimerCount());
    }

    private Player player(UUID uuid) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }
}
