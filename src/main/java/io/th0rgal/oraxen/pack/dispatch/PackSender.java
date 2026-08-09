package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.pack.upload.hosts.HostingProvider;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.HashUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PackSender {

    private static final long DEFAULT_PRE_JOIN_TIMEOUT_MILLIS = 30_000L;
    private static final Pattern TIMEOUT_PATTERN = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*(ms|s|m|ticks?|t)?$", Pattern.CASE_INSENSITIVE);

    protected final HostingProvider hostingProvider;
    private static final Object dispatchNormalizationLock = new Object();
    private static volatile boolean dispatchModeNormalized = false;
    private static volatile Object cachedTimeoutSource = null;
    private static volatile long cachedTimeoutMillis = DEFAULT_PRE_JOIN_TIMEOUT_MILLIS;

    protected PackSender(HostingProvider hostingProvider) {
        this.hostingProvider = hostingProvider;
    }

    public abstract void register();

    public abstract void unregister();

    public abstract void sendPack(Player player);

    /**
     * Sends a resource pack to a player using Paper's Adventure resource pack API.
     *
     * @param player Player to send pack to
     * @param uuid Pack UUID
     * @param url Pack download URL
     * @param sha1 Pack SHA-1 hash
     * @param prompt Prompt text (MiniMessage format)
     * @param mandatory Whether pack is mandatory
     */
    static void sendResourcePack(Player player, UUID uuid, String url, byte[] sha1,
                                  String prompt, boolean mandatory) {
        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        // The Adventure ResourcePackRequest API (and its MonkeyBars helper) is only available on the
        // Adventure version bundled with 1.20.3+. On older servers (e.g. 1.20.1), use Paper's
        // component-based setResourcePack API instead.
        if (VersionUtil.atOrAbove("1.20.3")) {
            player.sendResourcePacks(createResourcePackRequest(uuid, url, sha1, componentPrompt, mandatory));
        } else {
            player.setResourcePack(url, sha1, componentPrompt, mandatory);
        }
    }

    static void sendResourcePack(Audience audience, UUID uuid, String url, byte[] sha1,
                                 String prompt, boolean mandatory) {
        net.kyori.adventure.text.Component componentPrompt = AdventureUtils.MINI_MESSAGE.deserialize(prompt);
        audience.sendResourcePacks(createResourcePackRequest(uuid, url, sha1, componentPrompt, mandatory));
    }

    private static ResourcePackRequest createResourcePackRequest(UUID uuid, String url, byte[] sha1,
                                                                 net.kyori.adventure.text.Component prompt,
                                                                 boolean mandatory) {
        String hash = HashUtils.bytesToHex(sha1);
        ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(uuid, URI.create(url), hash);

        return ResourcePackRequest.resourcePackRequest()
                .required(mandatory)
                .replace(false)
                .prompt(prompt)
                .packs(info)
                .build();
    }

    public static boolean isSendPreJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled() && getDispatchMode() == DispatchMode.PRE_JOIN;
    }

    public static boolean isSendOnJoinConfigured() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled() && getDispatchMode() == DispatchMode.JOIN;
    }

    public static boolean isPreJoinDispatchActive() {
        return isSendPreJoinConfigured() && isPreJoinSupported();
    }

    public static boolean isAnyDispatchEnabled() {
        normalizeDispatchModeForServerSupport();
        return isDispatchSendEnabled();
    }

    public static long getPreJoinTimeoutMillis() {
        Object value = Settings.SEND_PACK_TIMEOUT.getValue();
        if (Objects.equals(value, cachedTimeoutSource)) {
            return cachedTimeoutMillis;
        }

        long timeoutMillis = parsePreJoinTimeout(value);
        cachedTimeoutSource = value;
        cachedTimeoutMillis = timeoutMillis;
        return timeoutMillis;
    }

    private static long parsePreJoinTimeout(Object value) {
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue() * 1000L);
        }

        if (value instanceof String rawTimeout) {
            Matcher matcher = TIMEOUT_PATTERN.matcher(rawTimeout.trim());
            if (matcher.matches()) {
                double amount = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2) != null ? matcher.group(2).toLowerCase(Locale.ROOT) : "s";
                double multiplier = switch (unit) {
                    case "ms" -> 1D;
                    case "m" -> 60_000D;
                    case "tick", "ticks", "t" -> 50D;
                    default -> 1_000D;
                };
                return Math.max(0L, Math.round(amount * multiplier));
            }
        }

        Logs.logWarning("Invalid Pack.dispatch.timeout '" + value + "'. Falling back to 30s.");
        return DEFAULT_PRE_JOIN_TIMEOUT_MILLIS;
    }

    private static boolean isDispatchSendEnabled() {
        Object send = Settings.SEND_PACK.getValue();
        return send instanceof Boolean bool && bool;
    }

    private static DispatchMode getDispatchMode() {
        Object value = Settings.SEND_PACK_MODE.getValue();
        if (!(value instanceof String mode)) return DispatchMode.JOIN;

        return switch (mode.trim().toUpperCase(Locale.ROOT).replace('_', '-')) {
            case "PRE-JOIN", "PREJOIN" -> DispatchMode.PRE_JOIN;
            case "JOIN" -> DispatchMode.JOIN;
            default -> {
                Logs.logWarning("Invalid Pack.dispatch.mode '" + mode + "'. Falling back to JOIN.");
                yield DispatchMode.JOIN;
            }
        };
    }

    private static boolean isPreJoinSupported() {
        return isPreJoinSupported(VersionUtil.atOrAbove("1.21.7"));
    }

    static boolean isPreJoinSupported(boolean isAtLeast1_21_7) {
        return isAtLeast1_21_7;
    }

    private static void normalizeDispatchModeForServerSupport() {
        if (dispatchModeNormalized) return;

        synchronized (dispatchNormalizationLock) {
            if (dispatchModeNormalized) return;

            if (isDispatchSendEnabled() && getDispatchMode() == DispatchMode.PRE_JOIN && !isPreJoinSupported()) {
                Logs.logInfo("Pack.dispatch.mode is set to PRE-JOIN, but pre-join dispatch is not available on this server. Falling back to JOIN.");
            }

            dispatchModeNormalized = true;
        }
    }

    /**
     * Resets the dispatch mode normalization flag so it is re-evaluated on next access.
     * Should be called on plugin reload to pick up changed settings.
     */
    public static void resetDispatchNormalization() {
        dispatchModeNormalized = false;
    }

    private enum DispatchMode {
        JOIN,
        PRE_JOIN
    }

}
