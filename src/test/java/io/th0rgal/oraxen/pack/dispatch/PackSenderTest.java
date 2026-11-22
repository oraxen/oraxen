package io.th0rgal.oraxen.pack.dispatch;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PackSenderTest {

    private final UUID playerId = UUID.randomUUID();

    @AfterEach
    void clearPackTracking() {
        PackSender.forgetPack(playerId);
    }

    @Test
    void preJoinRequiresSupportedVersion() {
        assertTrue(PackSender.isPreJoinSupported(true));
        assertFalse(PackSender.isPreJoinSupported(false));
    }

    @Test
    void removesPreviousPackBeforeSendingUpdatedPack() {
        Audience audience = mock(Audience.class);
        UUID previousPackId = UUID.randomUUID();
        UUID updatedPackId = UUID.randomUUID();
        ResourcePackRequest previousRequest = mock(ResourcePackRequest.class);
        ResourcePackRequest updatedRequest = mock(ResourcePackRequest.class);

        PackSender.sendResourcePack(audience, playerId, previousPackId, previousRequest);
        PackSender.sendResourcePack(audience, playerId, updatedPackId, updatedRequest);

        var order = inOrder(audience);
        order.verify(audience).sendResourcePacks(previousRequest);
        order.verify(audience).removeResourcePacks(eq(previousPackId));
        order.verify(audience).sendResourcePacks(updatedRequest);
    }

    @Test
    void keepsCurrentPackWhenThePackIdHasNotChanged() {
        Audience audience = mock(Audience.class);
        UUID packId = UUID.randomUUID();

        PackSender.sendResourcePack(audience, playerId, packId, mock(ResourcePackRequest.class));
        PackSender.sendResourcePack(audience, playerId, packId, mock(ResourcePackRequest.class));

        verify(audience, never()).removeResourcePacks(any(UUID.class), any(UUID[].class));
    }

    @Test
    void forgetsTrackedPackWhenThePlayerQuits() {
        Audience audience = mock(Audience.class);
        UUID previousPackId = UUID.randomUUID();
        UUID updatedPackId = UUID.randomUUID();
        Player player = mock(Player.class);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getPlayer()).thenReturn(player);

        PackSender.sendResourcePack(audience, playerId, previousPackId, mock(ResourcePackRequest.class));
        new PackLoadingManager().onPlayerQuit(event);
        PackSender.sendResourcePack(audience, playerId, updatedPackId, mock(ResourcePackRequest.class));

        verify(audience, never()).removeResourcePacks(any(UUID.class), any(UUID[].class));
    }

    @Test
    void serializesConcurrentPackSendsForTheSamePlayer() throws Exception {
        Audience audience = mock(Audience.class);
        UUID firstPackId = UUID.randomUUID();
        UUID secondPackId = UUID.randomUUID();
        ResourcePackRequest firstRequest = mock(ResourcePackRequest.class);
        ResourcePackRequest secondRequest = mock(ResourcePackRequest.class);
        CountDownLatch firstSendStarted = new CountDownLatch(1);
        CountDownLatch finishFirstSend = new CountDownLatch(1);
        CountDownLatch secondSendStarted = new CountDownLatch(1);

        doAnswer(invocation -> {
            ResourcePackRequest request = invocation.getArgument(0);
            if (request == firstRequest) {
                firstSendStarted.countDown();
                assertTrue(finishFirstSend.await(5, TimeUnit.SECONDS));
            } else if (request == secondRequest) {
                secondSendStarted.countDown();
            }
            return null;
        }).when(audience).sendResourcePacks(any(ResourcePackRequest.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstSend = executor.submit(() ->
                    PackSender.sendResourcePack(audience, playerId, firstPackId, firstRequest));
            assertTrue(firstSendStarted.await(5, TimeUnit.SECONDS));

            CountDownLatch secondSendAttempted = new CountDownLatch(1);
            Future<?> secondSend = executor.submit(() -> {
                secondSendAttempted.countDown();
                PackSender.sendResourcePack(audience, playerId, secondPackId, secondRequest);
            });
            assertTrue(secondSendAttempted.await(5, TimeUnit.SECONDS));
            assertFalse(secondSendStarted.await(1, TimeUnit.SECONDS));

            finishFirstSend.countDown();
            firstSend.get(5, TimeUnit.SECONDS);
            secondSend.get(5, TimeUnit.SECONDS);
            assertTrue(secondSendStarted.getCount() == 0);

            var order = inOrder(audience);
            order.verify(audience).sendResourcePacks(firstRequest);
            order.verify(audience).removeResourcePacks(eq(firstPackId));
            order.verify(audience).sendResourcePacks(secondRequest);
        } finally {
            finishFirstSend.countDown();
            executor.shutdownNow();
        }
    }
}
