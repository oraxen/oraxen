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
    private static final PackFormatThreshold[] PACK_FORMAT_THRESHOLDS = {
            new PackFormatThreshold("26.1", 84),
            new PackFormatThreshold("1.26.1", 84),
            new PackFormatThreshold("1.21.11", 75),
            new PackFormatThreshold("1.21.9", 69),
            new PackFormatThreshold("1.21.7", 64),
            new PackFormatThreshold("1.21.6", 63),
            new PackFormatThreshold("1.21.5", 55),
            new PackFormatThreshold("1.21.4", 46),
            new PackFormatThreshold("1.21.2", 42),
            new PackFormatThreshold("1.21", 34),
            new PackFormatThreshold("1.20.5", 32),
            new PackFormatThreshold("1.20.3", 22),
            new PackFormatThreshold("1.20.2", 18),
            new PackFormatThreshold("1.20", 15),
            new PackFormatThreshold("1.19.4", 13),
            new PackFormatThreshold("1.19.3", 12),
            new PackFormatThreshold("1.19", 9),
            new PackFormatThreshold("1.18", 8)
    };

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
        for (PackFormatThreshold threshold : PACK_FORMAT_THRESHOLDS) {
            if (version.isAtLeast(threshold.minimumVersion)) {
                return threshold.packFormat;
            }
        }

        // Very old / unknown
        return 6;
    }

    private record PackFormatThreshold(MinecraftVersion minimumVersion, int packFormat) {
        private PackFormatThreshold(String minimumVersion, int packFormat) {
            this(new MinecraftVersion(minimumVersion), packFormat);
        }
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
