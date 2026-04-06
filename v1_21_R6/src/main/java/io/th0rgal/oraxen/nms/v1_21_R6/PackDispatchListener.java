package io.th0rgal.oraxen.nms.v1_21_R6;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PackDispatchListener implements Listener {

    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> futures = new ConcurrentHashMap<>();

    @EventHandler
    public void onInitialConfig(@NotNull AsyncPlayerConnectionConfigureEvent event) {
        if (!PackSender.isPreJoinDispatchActive() || !PackSender.isAnyDispatchEnabled()) return;

        UUID uuid = event.getConnection().getProfile().getId();
        if (uuid == null) return;

        try {
            CompletableFuture<Void> previous = futures.remove(uuid);
            if (previous != null) previous.complete(null);
        } catch (Exception ignored) {
        }

        CompletableFuture<Void> future = PackSender.sendResourcePack(event.getConnection(), false);
        if (future != null) {
            futures.put(uuid, future);
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Logs.logWarning("Timed out while waiting for pre-join resource pack callback for " + uuid);
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Logs.logWarning("Interrupted while waiting for pre-join resource pack callback for " + uuid);
                future.cancel(true);
            } catch (ExecutionException e) {
                Logs.logWarning("Pre-join resource pack dispatch failed for " + uuid + ": " + e.getMessage());
                future.cancel(true);
            } finally {
                futures.remove(uuid, future);
            }
        }
    }

    @EventHandler
    public void onReconfig(@NotNull PlayerConnectionReconfigureEvent event) {
        if (!PackSender.isPreJoinDispatchActive() || !PackSender.isAnyDispatchEnabled()) return;
        PackSender.sendResourcePack(event.getConnection(), true);
    }

    @EventHandler
    public void onClose(@NotNull PlayerConnectionCloseEvent event) {
        try {
            CompletableFuture<Void> future = futures.remove(event.getPlayerUniqueId());
            if (future != null) future.complete(null);
        } catch (Exception ignored) {
        }
    }
}
