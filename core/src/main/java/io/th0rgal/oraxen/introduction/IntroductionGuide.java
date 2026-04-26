package io.th0rgal.oraxen.introduction;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;

/**
 * Sends the first-run introduction guide:
 *   - once to console on first successful enable, AND
 *   - once to the first eligible player who joins after that startup.
 *
 * Single source of truth for "first run" is {@link Settings#INTRODUCTION_CONSOLE_SENT}.
 * The "player guide pending" state is derived from "console message was sent during this
 * startup" rather than persisted to settings.yml — that avoids the bootstrap problem
 * where existing installs whose settings.yml is missing the new key would silently
 * never deliver the player message.
 */
public class IntroductionGuide implements Listener {

    private static final String PERMISSION = "oraxen.introduction";

    private final OraxenPlugin plugin;

    /** Set true when this startup just delivered the console message. */
    private volatile boolean playerPending;

    public IntroductionGuide(OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // Treat missing key as enabled (default true) so upgraded installs
        // with a stale settings.yml still receive the introduction. Only an
        // explicit false in settings.yml acts as a kill-switch.
        Boolean enabled = Settings.INTRODUCTION_ENABLED.toBool();
        if (Boolean.FALSE.equals(enabled)) {
            HandlerList.unregisterAll(this);
            return;
        }

        boolean alreadySent = Boolean.TRUE.equals(Settings.INTRODUCTION_CONSOLE_SENT.toBool());
        if (alreadySent) {
            HandlerList.unregisterAll(this);
            return;
        }

        Message.INTRODUCTION_GUIDE.log();
        if (!persistConsoleSent()) {
            // If we cannot persist, don't arm the player flow either: we'd repeat
            // the console message on next restart and risk double-sending.
            HandlerList.unregisterAll(this);
            return;
        }
        playerPending = true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!playerPending) return;

        Player player = event.getPlayer();
        if (!isEligible(player)) return;

        // Brief delay so the join message lands first.
        SchedulerUtil.runForEntityLater(plugin, player, 20L,
                () -> sendToPlayer(player),
                () -> {});
    }

    private synchronized void sendToPlayer(Player player) {
        if (!playerPending) return;
        if (!player.isOnline() || !isEligible(player)) return;

        playerPending = false;
        Message.INTRODUCTION_GUIDE.send(player);
        // HandlerList mutation must happen on the main thread to be safe on Folia.
        SchedulerUtil.runTask(plugin, () -> HandlerList.unregisterAll(this));
    }

    private boolean isEligible(Player player) {
        // Permission default is OP, so hasPermission already covers the op case.
        return player.hasPermission(PERMISSION);
    }

    private boolean persistConsoleSent() {
        YamlConfiguration settings = plugin.getConfigsManager().getSettings();
        settings.set(Settings.INTRODUCTION_CONSOLE_SENT.getPath(), true);
        try {
            settings.save(plugin.getConfigsManager().getSettingsFile());
            return true;
        } catch (IOException exception) {
            Logs.logWarning("Failed to save introduction guide state");
            Logs.debug(exception);
            return false;
        }
    }
}
