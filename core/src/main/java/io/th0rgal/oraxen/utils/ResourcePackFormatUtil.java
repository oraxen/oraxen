package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Resolves the correct {@code pack_format} for the server's current Minecraft version.
 *
 * <p>We prefer asking Mojang's own runtime constants via reflection to avoid hard-coding
 * values that change over time.</p>
 */
public final class ResourcePackFormatUtil {

    private static volatile Integer cached;

    private ResourcePackFormatUtil() {
    }

    public static int getCurrentResourcePackFormat() {
        Integer local = cached;
        if (local != null) return local;

        Integer resolved = resolveViaMinecraftClasses();
        if (resolved == null) {
            // Fallback: best-effort mapping based on the server version.
            // This should only be used if reflection fails (rare on supported versions).
            resolved = getPackFormatForVersion(MinecraftVersion.getCurrentVersion());
        }

        cached = resolved;
        return resolved;
    }

    /**
     * Gets the pack format for a specific Minecraft version.
     * This is a best-effort mapping based on known version-to-format relationships.
     * Used by multi-version pack generation to map server versions to pack formats.
     *
     * @param version Minecraft version to get pack format for
     * @return Pack format number
     */
    public static int getPackFormatForVersion(MinecraftVersion version) {
        // Best-effort mapping for modern versions.
        // NOTE: For 1.21.x versions, pack formats vary significantly between patches
        if (version.isAtLeast(new MinecraftVersion("1.21.11"))) return 61; // 1.21.11+
        if (version.isAtLeast(new MinecraftVersion("1.21.5"))) return 48; // 1.21.5-1.21.10
        if (version.isAtLeast(new MinecraftVersion("1.21.4"))) return 46; // 1.21.4
        if (version.isAtLeast(new MinecraftVersion("1.21.2"))) return 42; // 1.21.2-1.21.3
        if (version.isAtLeast(new MinecraftVersion("1.21"))) return 34; // 1.21-1.21.1
        if (version.isAtLeast(new MinecraftVersion("1.20.5"))) return 32;
        if (version.isAtLeast(new MinecraftVersion("1.20.3"))) return 22;
        if (version.isAtLeast(new MinecraftVersion("1.20.2"))) return 18;
        if (version.isAtLeast(new MinecraftVersion("1.20"))) return 15;
        if (version.isAtLeast(new MinecraftVersion("1.19.4"))) return 13;
        if (version.isAtLeast(new MinecraftVersion("1.19.3"))) return 12;
        if (version.isAtLeast(new MinecraftVersion("1.19"))) return 9;
        if (version.isAtLeast(new MinecraftVersion("1.18"))) return 8;

        // Very old / unknown
        return 6;
    }

    @Nullable
    private static Integer resolveViaMinecraftClasses() {
        try {
            Object currentVersion = getCurrentGameVersion();
            if (currentVersion == null) return null;

            Object clientResources = getClientResourcesPackType();
            if (clientResources == null) return null;

            // Try preferred method first, then fallback
            Integer result = tryGetPackVersionFromVersion(currentVersion, clientResources);
            if (result != null) return result;

            return tryGetVersionFromPackType(clientResources, currentVersion);
        } catch (Throwable t) {
            // Don't hard-fail pack generation because of a reflection mismatch
            return null;
        }
    }

    @Nullable
    private static Object getCurrentGameVersion() throws Exception {
        Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
        Method getCurrentVersion = sharedConstants.getMethod("getCurrentVersion");
        return getCurrentVersion.invoke(null);
    }

    @Nullable
    private static Object getClientResourcesPackType() throws Exception {
        Class<?> packTypeClass = Class.forName("net.minecraft.server.packs.PackType");
        Object[] enumConstants = packTypeClass.getEnumConstants();
        if (enumConstants == null) return null;

        for (Object constant : enumConstants) {
            if (constant instanceof Enum<?> enumConstant && "CLIENT_RESOURCES".equals(enumConstant.name())) {
                return constant;
            }
        }
        return null;
    }

    @Nullable
    private static Integer tryGetPackVersionFromVersion(Object currentVersion, Object clientResources) throws Exception {
        Class<?> packTypeClass = clientResources.getClass();
        for (Method method : currentVersion.getClass().getMethods()) {
            if (!"getPackVersion".equals(method.getName())) continue;
            if (method.getParameterCount() != 1) continue;
            if (!isIntReturnType(method)) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(packTypeClass)) continue;

            return toInteger(method.invoke(currentVersion, clientResources));
        }
        return null;
    }

    @Nullable
    private static Integer tryGetVersionFromPackType(Object clientResources, Object currentVersion) throws Exception {
        Class<?> packTypeClass = clientResources.getClass();
        for (Method method : packTypeClass.getMethods()) {
            if (!"getVersion".equals(method.getName()) && !"getPackVersion".equals(method.getName())) continue;
            if (method.getParameterCount() != 1) continue;
            if (!isIntReturnType(method)) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(currentVersion.getClass())) continue;

            return toInteger(method.invoke(clientResources, currentVersion));
        }
        return null;
    }

    private static boolean isIntReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        return returnType.equals(int.class) || returnType.equals(Integer.class);
    }

    private static Integer toInteger(Object result) {
        return (result instanceof Integer) ? (Integer) result : ((Number) result).intValue();
    }
}


