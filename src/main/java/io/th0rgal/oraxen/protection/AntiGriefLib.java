package io.th0rgal.oraxen.protection;

import net.momirealms.antigrieflib.Flag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public final class AntiGriefLib {

    private static @Nullable net.momirealms.antigrieflib.AntiGriefLib antiGriefLib;
    private static boolean debug;
    private static boolean initFailed;
    private static boolean testFailureLogged;

    private AntiGriefLib() {
    }

    public static void init(JavaPlugin plugin) {
        try {
            net.momirealms.antigrieflib.AntiGriefLib.Builder builder = net.momirealms.antigrieflib.AntiGriefLib.builder(plugin)
                    .silentLogs(!debug);

            Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
            if (worldGuard != null && classExists("com.sk89q.worldguard.WorldGuard")) {
                builder.exclude(protectionPlugin -> protectionPlugin.getName().equals("WorldGuard"))
                        .register(new OraxenWorldGuardCompatibility(worldGuard));
            }

            antiGriefLib = builder.build();
            initFailed = false;
        } catch (Exception | LinkageError exception) {
            // LinkageError covers NoClassDefFoundError etc. from protection plugins whose
            // classes are only partially visible to the isolated Paper-plugin classloader.
            // Fail closed: a broken protection integration must never grant access to
            // potentially protected claims, so deny all checks until this is resolved.
            antiGriefLib = null;
            initFailed = true;
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to initialize protection-plugin support; Oraxen will deny all build/break/interact checks until this is resolved.",
                    exception);
        }
    }

    public static void setDebug(boolean debug) {
        AntiGriefLib.debug = debug;
    }

    public static boolean getDebug() {
        return debug;
    }

    public static boolean canBuild(Player player, Location location) {
        return test(player, Flag.PLACE, location);
    }

    public static boolean canBreak(Player player, Location location) {
        return test(player, Flag.BREAK, location);
    }

    public static boolean canInteract(Player player, Location location) {
        return test(player, Flag.INTERACT, location);
    }

    public static boolean canUse(Player player, Location location) {
        return test(player, Flag.INTERACT, location);
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static <T> boolean test(Player player, Flag<T> flag, T value) {
        net.momirealms.antigrieflib.AntiGriefLib antiGriefLib = AntiGriefLib.antiGriefLib;
        // No instance is only a permissive state when initialization succeeded or never
        // ran; after an initialization failure we cannot know whether a protection
        // plugin is present, so fail closed.
        if (antiGriefLib == null) return !initFailed;

        try {
            return antiGriefLib.test(player, flag, value);
        } catch (Exception | LinkageError exception) {
            // Fail closed: never treat a broken protection integration as permission.
            if (!testFailureLogged) {
                testFailureLogged = true;
                Bukkit.getLogger().log(Level.SEVERE,
                        "Protection check failed; denying the action. Further failures are only logged with Oraxen debug enabled.",
                        exception);
            } else if (debug) {
                exception.printStackTrace();
            }
            return false;
        }
    }

    static void setInstance(@Nullable net.momirealms.antigrieflib.AntiGriefLib antiGriefLib) {
        AntiGriefLib.antiGriefLib = antiGriefLib;
        AntiGriefLib.initFailed = false;
        AntiGriefLib.testFailureLogged = false;
    }
}
