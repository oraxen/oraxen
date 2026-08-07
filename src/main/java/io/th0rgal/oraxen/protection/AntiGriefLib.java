package io.th0rgal.oraxen.protection;

import net.momirealms.antigrieflib.Flag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class AntiGriefLib {

    private static @Nullable net.momirealms.antigrieflib.AntiGriefLib antiGriefLib;
    private static boolean debug;

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
        } catch (Exception exception) {
            antiGriefLib = null;
            if (debug) exception.printStackTrace();
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
        if (antiGriefLib == null) return true;

        try {
            return antiGriefLib.test(player, flag, value);
        } catch (Exception exception) {
            if (debug) exception.printStackTrace();
            return true;
        }
    }

    static void setInstance(@Nullable net.momirealms.antigrieflib.AntiGriefLib antiGriefLib) {
        AntiGriefLib.antiGriefLib = antiGriefLib;
    }
}
