package io.th0rgal.oraxen.sounds;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomJukeboxSongRegistryTest {

    @Test
    void usesDatapackFallbackWhenHotInjectionIsUnavailable() {
        assertTrue(CustomJukeboxSongRegistry.usesDatapackFallback(false));
        assertFalse(CustomJukeboxSongRegistry.usesDatapackFallback(true));
    }

    @Test
    void serializesConcurrentRegistryTransactions() throws Exception {
        CountDownLatch firstInside = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondInside = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> CustomJukeboxSongRegistry.runWithReloadLock(() -> {
                firstInside.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstInside.await(1, TimeUnit.SECONDS));

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                CustomJukeboxSongRegistry.runWithReloadLock(secondInside::countDown);
            });
            assertTrue(secondStarted.await(1, TimeUnit.SECONDS));
            assertFalse(secondInside.await(100, TimeUnit.MILLISECONDS));

            releaseFirst.countDown();
            assertTrue(secondInside.await(1, TimeUnit.SECONDS));
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
