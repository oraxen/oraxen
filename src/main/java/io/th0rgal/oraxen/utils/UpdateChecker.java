package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker implements Listener {
    private static final URI LATEST_RELEASE = URI.create("https://api.github.com/repos/oraxen/oraxen/releases/latest");
    private static final Pattern INTERVAL = Pattern.compile("(?i)^([1-9]\\d*)([smhd])$");
    private static final Pattern VERSION = Pattern.compile("(?i)^v?(\\d+(?:\\.\\d+)*)(?:[-+].*)?$");
    // GitHub allows 60 unauthenticated requests per hour and IP, shared by every server on that IP
    static final long MIN_INTERVAL = TimeUnit.MINUTES.toMillis(10);
    static final long DEFAULT_INTERVAL = TimeUnit.HOURS.toMillis(24);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final OraxenPlugin plugin;
    private volatile String availableVersion;
    private ScheduledTask task;
    private long scheduledInterval;

    public UpdateChecker(OraxenPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!Settings.UPDATE_CHECKER_ENABLED.toBool()) {
            stop();
            availableVersion = null;
            return;
        }

        long interval = intervalMillis(Settings.UPDATE_CHECKER_INTERVAL.getValue());
        // Keep the running task and the known version on reloads so online players are not notified again
        if (task != null && interval == scheduledInterval) return;
        stop();
        scheduledInterval = interval;
        task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, ignored -> check(),
                1, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void check() {
        try {
            HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Oraxen-update-checker")
                    .timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IOException("GitHub returned HTTP " + response.statusCode());

            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            String version = release.get("tag_name").getAsString();
            String previous = availableVersion;
            availableVersion = compareVersions(version, plugin.getPluginMeta().getVersion()) > 0
                    ? version : null;
            if (availableVersion != null && !availableVersion.equals(previous))
                notifyOnlinePlayers(availableVersion);
        } catch (Exception exception) {
            if (Settings.DEBUG.toBool()) {
                Logs.logWarning("Failed to fetch the latest Oraxen release: " + exception.getMessage());
                Logs.debug(exception);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String version = availableVersion;
        Player player = event.getPlayer();
        if (version == null || !Settings.UPDATE_CHECKER_ENABLED.toBool()
                || !player.hasPermission("oraxen.update.notify")) return;
        sendUpdateMessage(player, version);
    }

    private void notifyOnlinePlayers(String version) {
        for (Player player : Bukkit.getOnlinePlayers())
            SchedulerUtil.runForEntity(player, () -> {
                if (player.isOnline() && player.hasPermission("oraxen.update.notify"))
                    sendUpdateMessage(player, version);
            }, null);
    }

    private static void sendUpdateMessage(Player player, String version) {
        player.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<prefix>").append(
                Component.text("Version " + version
                        + " is available, you are running an outdated version.")));
    }

    static long intervalMillis(Object value) {
        // Read the raw value: a numeric YAML value (e.g. "interval: 24") is not a String
        long interval = parseIntervalMillis(String.valueOf(value));
        if (interval < 0) {
            Logs.logWarning("Invalid update-checker.interval '" + value + "'; using 24h.");
            return DEFAULT_INTERVAL;
        }
        if (interval < MIN_INTERVAL) {
            Logs.logWarning("update-checker.interval '" + value + "' is too short; using the 10m minimum.");
            return MIN_INTERVAL;
        }
        return interval;
    }

    /**
     * @return the interval in milliseconds, or -1 if the value is not a valid interval
     */
    static long parseIntervalMillis(String value) {
        Matcher matcher = INTERVAL.matcher(value.trim());
        if (!matcher.matches()) return -1;
        try {
            long amount = Long.parseLong(matcher.group(1));
            long milliseconds = switch (matcher.group(2).toLowerCase()) {
                case "s" -> TimeUnit.SECONDS.toMillis(amount);
                case "m" -> TimeUnit.MINUTES.toMillis(amount);
                case "h" -> TimeUnit.HOURS.toMillis(amount);
                default -> TimeUnit.DAYS.toMillis(amount);
            };
            return milliseconds > TimeUnit.DAYS.toMillis(365) ? -1 : milliseconds;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    static int compareVersions(String latest, String current) {
        Matcher latestMatch = VERSION.matcher(latest);
        Matcher currentMatch = VERSION.matcher(current);
        if (!latestMatch.matches() || !currentMatch.matches()) return 0;
        String[] latestParts = latestMatch.group(1).split("\\.");
        String[] currentParts = currentMatch.group(1).split("\\.");
        for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
            int difference = new java.math.BigInteger(i < latestParts.length ? latestParts[i] : "0")
                    .compareTo(new java.math.BigInteger(i < currentParts.length ? currentParts[i] : "0"));
            if (difference != 0) return difference;
        }
        return 0;
    }

}
