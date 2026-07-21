package io.th0rgal.oraxen.pack.dispatch;

import io.th0rgal.oraxen.pack.generation.ProtocolVersion;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Detects the Minecraft client version of players using ViaVersion, ProtocolSupport, or other APIs.
 */
public class PlayerVersionDetector {

    private static volatile VersionDetectionMethod detectionMethod = VersionDetectionMethod.NONE;
    private static Method viaVersionGetPlayerVersionMethod;
    private static ViaVersionArgType viaVersionArgType = ViaVersionArgType.NONE;
    private static Method protocolSupportGetProtocolVersionMethod;
    private static Object viaApiInstance;

    private static volatile boolean initialized = false;

    /**
     * Initializes the version detector by checking for available version detection plugins.
     * This is called lazily on first use to ensure it's ready when players join.
     * Thread-safe: uses double-checked locking to prevent redundant initialization.
     */
    public static void initialize() {
        if (initialized) return;

        synchronized (PlayerVersionDetector.class) {
            if (initialized) return;

            // Try ViaVersion first (most popular and reliable)
            if (tryInitializeViaVersion()) {
                detectionMethod = VersionDetectionMethod.VIA_VERSION;
                Logs.logSuccess("Player version detection enabled via ViaVersion");
                initialized = true;
                return;
            }

            // Try ProtocolSupport as fallback
            if (tryInitializeProtocolSupport()) {
                detectionMethod = VersionDetectionMethod.PROTOCOL_SUPPORT;
                Logs.logSuccess("Player version detection enabled via ProtocolSupport");
                initialized = true;
                return;
            }

            // No version detection available
            detectionMethod = VersionDetectionMethod.NONE;
            Logs.logWarning("No version detection plugin found (ViaVersion/ProtocolSupport)");
            Logs.logWarning("All players will receive the server's pack version");
            initialized = true;
        }
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
        // ViaVersion API signatures vary by version; UUID and Player are both used in the wild.
        for (ViaVersionArgType argType : new ViaVersionArgType[]{ViaVersionArgType.UUID, ViaVersionArgType.PLAYER, ViaVersionArgType.OBJECT}) {
            Class<?> parameterType = argType.resolveParameterType();
            if (parameterType == null) {
                continue;
            }
            try {
                viaVersionGetPlayerVersionMethod = viaApiClass.getMethod(methodName, parameterType);
                viaVersionArgType = argType;
                return true;
            } catch (NoSuchMethodException ignored) {
                // Try next signature
            }
        }
        return false;
    }

    private static boolean tryInitializeProtocolSupport() {
        if (!PluginUtils.isEnabled("ProtocolSupport")) {
            return false;
        }

        try {
            // ProtocolSupport API
            Class<?> protocolSupportApiClass = Class.forName("protocolsupport.api.ProtocolSupportAPI");
            Class<?> playerClass = ViaVersionArgType.PLAYER.resolveParameterType();
            if (playerClass == null) {
                return false;
            }
            protocolSupportGetProtocolVersionMethod = protocolSupportApiClass.getMethod("getProtocolVersion", playerClass);

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
            String playerName = player.getName();
            Logs.logWarning("Failed to detect protocol version for " + playerName + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private static Integer getViaVersionProtocol(Player player) throws Exception {
        if (viaApiInstance == null || viaVersionGetPlayerVersionMethod == null) {
            return null;
        }

        Object argument = switch (viaVersionArgType) {
            case UUID -> player.getUniqueId();
            case PLAYER, OBJECT -> player;
            default -> null;
        };
        if (argument == null) {
            return null;
        }

        Object version = viaVersionGetPlayerVersionMethod.invoke(viaApiInstance, argument);
        if (version instanceof Integer) {
            return (Integer) version;
        }

        // ViaVersion 5.x returns a ProtocolVersion object instead of int.
        // Use reflection to call getVersion() or getId() to extract the int value.
        if (version != null) {
            try {
                Method getVersionMethod = version.getClass().getMethod("getVersion");
                Object result = getVersionMethod.invoke(version);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (NoSuchMethodException ignored) {
                // Try getId() as fallback
            }
            try {
                Method getIdMethod = version.getClass().getMethod("getId");
                Object result = getIdMethod.invoke(version);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (NoSuchMethodException ignored) {
                // Neither method available
            }
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
            Throwable cause = e.getCause();
            String errorMessage = cause != null ? cause.getMessage() : "Unknown error";
            Logs.logWarning("ProtocolSupport API error for " + player.getName() + ": " + errorMessage);
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

    enum VersionDetectionMethod {
        NONE,
        VIA_VERSION,
        PROTOCOL_SUPPORT
    }

    private enum ViaVersionArgType {
        NONE((Class<?>) null),
        UUID(UUID.class),
        PLAYER("org.bukkit.entity.Player"),
        OBJECT(Object.class);

        private final Class<?> parameterType;
        private final String parameterTypeName;

        ViaVersionArgType(Class<?> parameterType) {
            this.parameterType = parameterType;
            this.parameterTypeName = null;
        }

        ViaVersionArgType(String parameterTypeName) {
            this.parameterType = null;
            this.parameterTypeName = parameterTypeName;
        }

        @Nullable
        private Class<?> resolveParameterType() {
            if (parameterType != null) {
                return parameterType;
            }
            if (parameterTypeName == null) {
                return null;
            }
            try {
                return Class.forName(parameterTypeName);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
    }
}
