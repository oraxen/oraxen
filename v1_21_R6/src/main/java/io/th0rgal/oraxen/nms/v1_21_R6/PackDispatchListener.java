package io.th0rgal.oraxen.nms.v1_21_R6;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.connection.configuration.PlayerConnectionReconfigureEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.dispatch.PackSender;
import io.th0rgal.oraxen.pack.receive.PackReceiver;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        CompletableFuture<Void> future = sendResourcePack(event.getConnection(), false);
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
        if (!PackSender.isPreJoinDispatchActive() || !PackSender.isAnyDispatchEnabled()) {
            event.getConnection().completeReconfiguration();
            return;
        }
        CompletableFuture<Void> future = sendResourcePack(event.getConnection(), true);
        if (future == null) {
            event.getConnection().completeReconfiguration();
        }
    }

    @EventHandler
    public void onClose(@NotNull PlayerConnectionCloseEvent event) {
        try {
            CompletableFuture<Void> future = futures.remove(event.getPlayerUniqueId());
            if (future != null) future.complete(null);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private static CompletableFuture<Void> sendResourcePack(PlayerConfigurationConnection connection, boolean reconfigure) {
        String packUrl = OraxenPlugin.get().getPackURL();
        String hash = OraxenPlugin.get().getPackSHA1();
        if (packUrl == null || hash == null) return null;
        UUID playerId = connection.getProfile().getId();
        if (playerId == null) return null;

        byte[] hashBytes = hashArray(hash);
        UUID packUUID = UUID.nameUUIDFromBytes(hashBytes);
        CompletableFuture<Void> future = new CompletableFuture<>();

        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo()
                .id(packUUID)
                .uri(java.net.URI.create(packUrl))
                .hash(hash)
                .build();

        ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .required(Settings.SEND_PACK_MANDATORY.toBool())
                .replace(true)
                .prompt(AdventureUtils.MINI_MESSAGE.deserialize(Settings.SEND_PACK_PROMPT.toString()))
                .packs(info)
                .callback((requestId, status, audience) -> {
                    PackReceiver.handleAdventureStatus(playerId, status);
                    if (!status.intermediate()) {
                        future.complete(null);
                        if (reconfigure) connection.completeReconfiguration();
                    }
                })
                .build();

        connection.getAudience().sendResourcePacks(request);
        return future;
    }

    private static byte[] hashArray(String hash) {
        int length = hash.length();
        if (length % 2 != 0) throw new IllegalArgumentException("Hash length must be even");

        byte[] result = new byte[length / 2];
        for (int i = 0; i < result.length; i++) {
            int from = i * 2;
            result[i] = (byte) Integer.parseInt(hash.substring(from, from + 2), 16);
        }
        return result;
    }
}
