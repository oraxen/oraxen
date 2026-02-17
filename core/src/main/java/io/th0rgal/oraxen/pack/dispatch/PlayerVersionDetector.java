package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.pack.generation.ProtocolVersion;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Detects the Minecraft client version of players using ViaVersion, ProtocolSupport, or other APIs.
 */
public class PlayerVersionDetector {

    private static VersionDetectionMethod detectionMethod = VersionDetectionMethod.NONE;
    private static Method viaVersionGetPlayerVersionMethod;
    private static Method protocolSupportGetProtocolVersionMethod;
    private static Object viaApiInstance;

    private static volatile boolean initialized = false;

    /**
     * Initializes the version detector by checking for available version detection plugins.
     * This is called lazily on first use to ensure it's ready when players join.
     */
    public static void initialize() {
        if (initialized) return;
        
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
        
        initialized = true;
    }

    private static boolean tryInitializeViaVersion() {
        if (!PluginUtils.isEnabled("ViaVersion") && !PluginUtils.isEnabled("ViaBackwards")) {
            return false;
        }

        try {
            Class<?> viaApiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");

            Method getApiMethod = viaClass.getMethod("getAPI");
            viaApiInstance = getApiMethod.invoke(null);

            if (!tryGetViaVersionMethod(viaApiClass, "getPlayerProtocolVersion")
                    && !tryGetViaVersionMethod(viaApiClass, "getPlayerVersion")) {
                Logs.logWarning("ViaVersion detected but neither getPlayerProtocolVersion nor getPlayerVersion method found");
                return false;
            }

            return true;
        } catch (Exception e) {
            Logs.logWarning("ViaVersion detected but API initialization failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean tryGetViaVersionMethod(Class<?> viaApiClass, String methodName) {
        try {
            viaVersionGetPlayerVersionMethod = viaApiClass.getMethod(methodName, Object.class);
            return true;
        } catch (NoSuchMethodException e) {
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

        // Ensure detector is initialized (lazy initialization)
        if (!initialized) {
            initialize();
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
            String playerName = player != null ? player.getName() : "unknown";
            Logs.logWarning("Failed to detect protocol version for " + playerName + ": " + e.getMessage());
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

        try {
            Object protocolVersion = protocolSupportGetProtocolVersionMethod.invoke(null, player);

            if (protocolVersion != null) {
                try {
                    Method getIdMethod = protocolVersion.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(protocolVersion);
                    if (id instanceof Integer) {
                        return (Integer) id;
                    }
                } catch (NoSuchMethodException e) {
                    if (protocolVersion instanceof Integer) {
                        return (Integer) protocolVersion;
                    }
                }
            }

            return null;
        } catch (InvocationTargetException e) {
            Logs.logWarning("ProtocolSupport API error for " + player.getName() + ": " + e.getCause().getMessage());
            return null;
        }
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
     * Delegates to ProtocolVersion.getVersionStringForProtocol() for consistency.
     *
     * @param protocol Protocol version number
     * @return Version string (e.g., "1.20.4"), or "Unknown (N)" if unknown
     */
    public static String protocolToVersionString(int protocol) {
        return ProtocolVersion.getVersionStringForProtocol(protocol);
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
