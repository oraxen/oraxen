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
            resolved = fallbackByMinecraftVersion();
            Logs.logWarning("Failed to resolve resource pack format via NMS. Falling back to pack_format=" + resolved);
        }

        cached = resolved;
        return resolved;
    }

    private static int fallbackByMinecraftVersion() {
        MinecraftVersion v = MinecraftVersion.getCurrentVersion();

        // Best-effort mapping for modern versions (used only if NMS reflection fails).
        // If this becomes outdated, reflection still handles it on supported servers.
        if (v.isAtLeast(new MinecraftVersion("1.21"))) return 34;
        if (v.isAtLeast(new MinecraftVersion("1.20.5"))) return 32;
        if (v.isAtLeast(new MinecraftVersion("1.20.3"))) return 22;
        if (v.isAtLeast(new MinecraftVersion("1.20.2"))) return 18;
        if (v.isAtLeast(new MinecraftVersion("1.20"))) return 15;
        if (v.isAtLeast(new MinecraftVersion("1.19.4"))) return 13;
        if (v.isAtLeast(new MinecraftVersion("1.19.3"))) return 12;
        if (v.isAtLeast(new MinecraftVersion("1.19"))) return 9;
        if (v.isAtLeast(new MinecraftVersion("1.18"))) return 8;

        // Very old / unknown
        return 6;
    }

    @Nullable
    private static Integer resolveViaMinecraftClasses() {
        try {
            // net.minecraft.SharedConstants.getCurrentVersion()
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
            Method getCurrentVersion = sharedConstants.getMethod("getCurrentVersion");
            Object currentVersion = getCurrentVersion.invoke(null);

            // net.minecraft.server.packs.PackType.CLIENT_RESOURCES
            Class<?> packTypeClass = Class.forName("net.minecraft.server.packs.PackType");
            Object clientResources = null;
            Object[] enumConstants = packTypeClass.getEnumConstants();
            if (enumConstants != null) {
                for (Object constant : enumConstants) {
                    if (constant instanceof Enum<?> enumConstant && enumConstant.name().equals("CLIENT_RESOURCES")) {
                        clientResources = constant;
                        break;
                    }
                }
            }
            if (clientResources == null) return null;

            // Preferred: currentVersion.getPackVersion(PackType)
            for (Method method : currentVersion.getClass().getMethods()) {
                if (!method.getName().equals("getPackVersion")) continue;
                if (method.getParameterCount() != 1) continue;
                if (!method.getReturnType().equals(int.class) && !method.getReturnType().equals(Integer.class)) continue;
                if (!method.getParameterTypes()[0].isAssignableFrom(packTypeClass)) continue;

                Object result = method.invoke(currentVersion, clientResources);
                return (result instanceof Integer) ? (Integer) result : ((Number) result).intValue();
            }

            // Fallback: PackType#getVersion(currentVersion)
            for (Method method : packTypeClass.getMethods()) {
                if (!method.getName().equals("getVersion") && !method.getName().equals("getPackVersion")) continue;
                if (method.getParameterCount() != 1) continue;
                if (!method.getReturnType().equals(int.class) && !method.getReturnType().equals(Integer.class)) continue;
                if (!method.getParameterTypes()[0].isAssignableFrom(currentVersion.getClass())) continue;

                Object result = method.invoke(clientResources, currentVersion);
                return (result instanceof Integer) ? (Integer) result : ((Number) result).intValue();
            }

            return null;
        } catch (Throwable t) {
            // Don't hard-fail pack generation because of a reflection mismatch
            return null;
        }
    }
}


