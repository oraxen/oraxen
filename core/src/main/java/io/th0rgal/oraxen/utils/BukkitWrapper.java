package io.th0rgal.oraxen.utils;

import dev.jorel.commandapi.CommandAPIConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class BukkitWrapper {

    private BukkitWrapper() {
    }

    public static CommandAPIConfig<?> createCommandApiConfig(JavaPlugin plugin) {
        final String paperConfigClass = "dev.jorel.commandapi.CommandAPIPaperConfig";
        final String spigotConfigClass = "dev.jorel.commandapi.CommandAPISpigotConfig";

        if (VersionUtil.isPaperServer()) {
            CommandAPIConfig<?> paper = tryConstruct(paperConfigClass, plugin);
            if (paper != null)
                return applyCommonOptions(paper);
        }

        CommandAPIConfig<?> spigot = tryConstruct(spigotConfigClass, plugin);
        if (spigot != null)
            return applyCommonOptions(spigot);

        throw new IllegalStateException(
                "Neither CommandAPIPaperConfig nor CommandAPISpigotConfig are available on the classpath");
    }

    private static CommandAPIConfig<?> tryConstruct(String className, JavaPlugin plugin) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(JavaPlugin.class);
            Object instance = ctor.newInstance(plugin);
            return (CommandAPIConfig<?>) instance;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static CommandAPIConfig<?> applyCommonOptions(CommandAPIConfig<?> config) {
        // Always enable silent logs
        config.silentLogs(true);
        // Best-effort: call skipReloadDatapacks(true) if method exists on this
        // implementation
        invokeIfPresent(config, "skipReloadDatapacks", true);
        return config;
    }

    private static void invokeIfPresent(Object target, String methodName, boolean arg) {
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, arg);
        } catch (Throwable ignored) {
        }
    }
}
