package io.th0rgal.oraxen.introduction;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;

public class IntroductionGuide implements Listener {

    private static final String PERMISSION = "oraxen.introduction";

    private final OraxenPlugin plugin;
    private final Object lock = new Object();

    private boolean consoleSent;
    private boolean playerPending;

    public IntroductionGuide(OraxenPlugin plugin) {
        this.plugin = plugin;
        loadState();
    }

    public void start() {
        boolean sendConsoleMessage = false;
        boolean consoleSentSnapshot;
        boolean playerPendingSnapshot;
        synchronized (lock) {
            if (!consoleSent) {
                consoleSent = true;
                sendConsoleMessage = true;
            }
            consoleSentSnapshot = consoleSent;
            playerPendingSnapshot = playerPending;
        }

        if (sendConsoleMessage || playerPendingSnapshot) {
            saveState(consoleSentSnapshot, playerPendingSnapshot);
        }

        if (sendConsoleMessage) {
            Message.INTRODUCTION_GUIDE.log();
        }

        if (consoleSentSnapshot && !playerPendingSnapshot) {
            HandlerList.unregisterAll(this);
            return;
        }

        sendToFirstEligibleOnlinePlayer();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!shouldSendToPlayer())
            return;

        Player player = event.getPlayer();
        if (!isEligible(player))
            return;

        SchedulerUtil.runForEntityLater(plugin, player, 20L, () -> sendToPlayer(player), () -> {});
    }

    private void sendToFirstEligibleOnlinePlayer() {
        if (!shouldSendToPlayer())
            return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEligible(player))
                continue;
            SchedulerUtil.runForEntityLater(plugin, player, 1L, () -> sendToPlayer(player), () -> {});
            break;
        }
    }

    private boolean shouldSendToPlayer() {
        synchronized (lock) {
            return playerPending;
        }
    }

    private void sendToPlayer(Player player) {
        boolean shouldSend;
        boolean consoleSentSnapshot;
        boolean playerPendingSnapshot;
        synchronized (lock) {
            if (!playerPending || !player.isOnline() || !isEligible(player)) {
                shouldSend = false;
                consoleSentSnapshot = false;
                playerPendingSnapshot = false;
            } else {
                playerPending = false;
                shouldSend = true;
                consoleSentSnapshot = consoleSent;
                playerPendingSnapshot = playerPending;
            }
        }

        if (!shouldSend)
            return;

        saveState(consoleSentSnapshot, playerPendingSnapshot);
        Message.INTRODUCTION_GUIDE.send(player);
        SchedulerUtil.runTask(plugin, () -> HandlerList.unregisterAll(this));
    }

    private boolean isEligible(Player player) {
        return player.isOp() || player.hasPermission(PERMISSION);
    }

    private void loadState() {
        consoleSent = Boolean.TRUE.equals(Settings.INTRODUCTION_CONSOLE_SENT.toBool());
        playerPending = Boolean.TRUE.equals(Settings.INTRODUCTION_PLAYER_PENDING.toBool());
    }

    private void saveState(boolean consoleSent, boolean playerPending) {
        YamlConfiguration settings = plugin.getConfigsManager().getSettings();
        settings.set(Settings.INTRODUCTION_CONSOLE_SENT.getPath(), consoleSent);
        settings.set(Settings.INTRODUCTION_PLAYER_PENDING.getPath(), playerPending);

        try {
            settings.save(plugin.getConfigsManager().getSettingsFile());
        } catch (IOException exception) {
            Logs.logWarning("Failed to save introduction guide state");
            Logs.debug(exception);
        }
    }
}
