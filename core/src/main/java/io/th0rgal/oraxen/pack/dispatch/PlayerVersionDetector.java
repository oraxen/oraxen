package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Detects the Minecraft client version of players using ViaVersion, ProtocolSupport, or other APIs.
 */
public class PlayerVersionDetector {

    private static VersionDetectionMethod detectionMethod = VersionDetectionMethod.NONE;
    private static Method viaVersionGetPlayerVersionMethod;
    private static Method protocolSupportGetProtocolVersionMethod;
    private static Object viaApiInstance;

    /**
     * Initializes the version detector by checking for available version detection plugins.
     */
    public static void initialize() {
        // Try ViaVersion first (most popular and reliable)
        if (tryInitializeViaVersion()) {
            detectionMethod = VersionDetectionMethod.VIA_VERSION;
            Logs.logSuccess("Player version detection enabled via ViaVersion");
            return;
        }

        // Try ProtocolSupport as fallback
        if (tryInitializeProtocolSupport()) {
            detectionMethod = VersionDetectionMethod.PROTOCOL_SUPPORT;
            Logs.logSuccess("Player version detection enabled via ProtocolSupport");
            return;
        }

        // No version detection available
        detectionMethod = VersionDetectionMethod.NONE;
        Logs.logWarning("No version detection plugin found (ViaVersion/ProtocolSupport)");
        Logs.logWarning("All players will receive the server's pack version");
    }

    private static boolean tryInitializeViaVersion() {
        if (!PluginUtils.isEnabled("ViaVersion") && !PluginUtils.isEnabled("ViaBackwards")) {
            return false;
        }

        try {
            // ViaVersion API
            Class<?> viaApiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");

            // Get Via.getAPI() method
            Method getApiMethod = viaClass.getMethod("getAPI");
            viaApiInstance = getApiMethod.invoke(null);

            // Get getPlayerVersion(Player) method from ViaAPI
            viaVersionGetPlayerVersionMethod = viaApiClass.getMethod("getPlayerVersion", Object.class);

            return true;
        } catch (Exception e) {
            Logs.logWarning("ViaVersion detected but API initialization failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean tryInitializeProtocolSupport() {
        if (!PluginUtils.isEnabled("ProtocolSupport")) {
            return false;
        }

        try {
            // ProtocolSupport API
            Class<?> protocolSupportApiClass = Class.forName("protocolsupport.api.ProtocolSupportAPI");
            protocolSupportGetProtocolVersionMethod = protocolSupportApiClass.getMethod("getProtocolVersion", Player.class);

            return true;
        } catch (Exception e) {
            Logs.logWarning("ProtocolSupport detected but API initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the protocol version of a player's client.
     *
     * @param player Player to check
     * @return Protocol version number, or null if detection failed or unavailable
     */
    @Nullable
    public static Integer getPlayerProtocolVersion(Player player) {
        if (player == null) {
            return null;
        }

        try {
            switch (detectionMethod) {
                case VIA_VERSION:
                    return getViaVersionProtocol(player);
                case PROTOCOL_SUPPORT:
                    return getProtocolSupportProtocol(player);
                default:
                    return null;
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to detect protocol version for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private static Integer getViaVersionProtocol(Player player) throws Exception {
        if (viaApiInstance == null || viaVersionGetPlayerVersionMethod == null) {
            return null;
        }

        Object version = viaVersionGetPlayerVersionMethod.invoke(viaApiInstance, player);
        if (version instanceof Integer) {
            return (Integer) version;
        }

        return null;
    }

    @Nullable
    private static Integer getProtocolSupportProtocol(Player player) throws Exception {
        if (protocolSupportGetProtocolVersionMethod == null) {
            return null;
        }

        Object protocolVersion = protocolSupportGetProtocolVersionMethod.invoke(null, player);

        // ProtocolSupport returns a ProtocolVersion enum
        if (protocolVersion != null) {
            Method getIdMethod = protocolVersion.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(protocolVersion);
            if (id instanceof Integer) {
                return (Integer) id;
            }
        }

        return null;
    }

    /**
     * Gets a human-readable version string for a player.
     * This is for logging/debugging purposes.
     *
     * @param player Player to check
     * @return Version string (e.g., "1.20.4"), or "Unknown" if detection failed
     */
    public static String getPlayerVersionString(Player player) {
        Integer protocol = getPlayerProtocolVersion(player);
        if (protocol == null) {
            return "Unknown";
        }

        return protocolToVersionString(protocol);
    }

    /**
     * Converts a protocol version number to a human-readable Minecraft version string.
     * This is a utility function for logging/debugging and testing purposes.
     *
     * @param protocol Protocol version number
     * @return Version string (e.g., "1.20.4"), or "Unknown (N)" if unknown
     */
    public static String protocolToVersionString(int protocol) {
        // Map protocol versions to Minecraft versions (best effort)
        // https://minecraft.wiki/w/Protocol_version

        if (protocol >= 769) return "1.21.4+";
        if (protocol >= 768) return "1.21.2";
        if (protocol == 767) return "1.21";
        if (protocol == 766) return "1.20.5";
        if (protocol == 765) return "1.20.3";
        if (protocol == 764) return "1.20.2";
        if (protocol == 763) return "1.20.1";
        if (protocol == 762) return "1.19.4";
        if (protocol == 761) return "1.19.3";
        if (protocol == 760) return "1.19.1";
        if (protocol == 759) return "1.19";
        if (protocol == 758) return "1.18.2";

        return "Unknown (" + protocol + ")";
    }

    /**
     * Checks if version detection is available.
     *
     * @return true if ViaVersion or ProtocolSupport is available
     */
    public static boolean isAvailable() {
        return detectionMethod != VersionDetectionMethod.NONE;
    }

    /**
     * Gets the current detection method.
     *
     * @return Detection method being used
     */
    public static VersionDetectionMethod getDetectionMethod() {
        return detectionMethod;
    }

    public enum VersionDetectionMethod {
        NONE,
        VIA_VERSION,
        PROTOCOL_SUPPORT
    }
}
