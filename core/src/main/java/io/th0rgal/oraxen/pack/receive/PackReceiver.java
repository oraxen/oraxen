package io.th0rgal.oraxen.pack.receive;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PackReceiver implements Listener {

    private static final ConcurrentMap<UUID, List<PlayerResourcePackStatusEvent.Status>> pendingStatuses = new ConcurrentHashMap<>();
    private static final Object pendingStatusesLock = new Object();

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerUpdatesPackStatus(PlayerResourcePackStatusEvent event) {
        processStatus(event.getPlayer(), event.getStatus());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        List<PlayerResourcePackStatusEvent.Status> queuedStatuses;
        synchronized (pendingStatusesLock) {
            queuedStatuses = pendingStatuses.remove(playerId);
        }
        if (queuedStatuses == null || queuedStatuses.isEmpty()) return;

        for (PlayerResourcePackStatusEvent.Status status : queuedStatuses) {
            processStatus(event.getPlayer(), status);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        synchronized (pendingStatusesLock) {
            pendingStatuses.remove(event.getPlayer().getUniqueId());
        }
    }

    public static void handleAdventureStatus(@Nullable UUID playerId, @NotNull ResourcePackStatus status) {
        if (!Settings.RECEIVE_ENABLED.toBool() || playerId == null) return;

        PlayerResourcePackStatusEvent.Status bukkitStatus = toBukkitStatus(status);
        if (bukkitStatus == null) return;

        Player playerToProcess = null;
        synchronized (pendingStatusesLock) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                playerToProcess = player;
            } else {
                pendingStatuses.compute(playerId, (uuid, statuses) -> {
                    List<PlayerResourcePackStatusEvent.Status> list = statuses != null ? statuses : new ArrayList<>();
                    list.add(bukkitStatus);
                    return list;
                });
            }
        }

        if (playerToProcess != null) {
            Player scheduledPlayer = playerToProcess;
            SchedulerUtil.runTask(() -> {
                if (scheduledPlayer.isOnline()) {
                    processStatus(scheduledPlayer, bukkitStatus);
                }
            });
        }
    }

    private static void processStatus(Player player, PlayerResourcePackStatusEvent.Status status) {
        if (!Settings.RECEIVE_ENABLED.toBool()) return;

        TagResolver playerResolver = AdventureUtils.tagResolver("player", player.getName());
        PackAction packAction = switch (status) {
            case ACCEPTED -> new PackAction(Settings.RECEIVE_ALLOWED_ACTIONS.toConfigSection(), playerResolver);
            case DECLINED -> new PackAction(Settings.RECEIVE_DENIED_ACTIONS.toConfigSection(), playerResolver);
            case FAILED_DOWNLOAD -> new PackAction(Settings.RECEIVE_FAILED_ACTIONS.toConfigSection(), playerResolver);
            case SUCCESSFULLY_LOADED -> new PackAction(Settings.RECEIVE_LOADED_ACTIONS.toConfigSection(), playerResolver);

            // 1.20.3+ only. switch statement should never reach this however on lower versions
            case DOWNLOADED -> new PackAction(Settings.RECEIVE_DOWNLOADED_ACTIONS.toConfigSection(), playerResolver);
            case INVALID_URL -> new PackAction(Settings.RECEIVE_INVALID_URL_ACTIONS.toConfigSection(), playerResolver);
            case FAILED_RELOAD -> new PackAction(Settings.RECEIVE_FAILED_RELOAD_ACTIONS.toConfigSection(), playerResolver);
            case DISCARDED -> new PackAction(Settings.RECEIVE_DISCARDED_ACTIONS.toConfigSection(), playerResolver);
        };
        SchedulerUtil.runForEntityLater(player, packAction.getDelay(), () -> {
            if (packAction.hasMessage())
                sendMessage(player, packAction.getMessageType(), packAction.getMessageContent());
            if (packAction.hasSound())
                packAction.playSound(player, player.getLocation());

            packAction.getCommandsParser().perform(player);
        }, () -> {});
    }

    @Nullable
    private static PlayerResourcePackStatusEvent.Status toBukkitStatus(ResourcePackStatus status) {
        try {
            return PlayerResourcePackStatusEvent.Status.valueOf(status.name());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void sendMessage(Player receiver, String action, Component message) {
        @NotNull Audience audience = OraxenPlugin.get().getAudience().sender(receiver);
        switch (action) {
            case "KICK" -> receiver.kickPlayer(AdventureUtils.LEGACY_SERIALIZER.serialize(message));
            case "CHAT" -> audience.sendMessage(message);
            case "ACTION_BAR" -> audience.sendActionBar(message);
            case "TITLE" ->
                    audience.showTitle(Title.title(Component.empty(), message, Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(3500), Duration.ofMillis(250))));
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
    }

}
