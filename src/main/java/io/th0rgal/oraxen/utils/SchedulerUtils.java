package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {

    public static int scheduleSyncRepeatingTask(Plugin plugin, long delay, long period, Runnable runnable) {
        if (OraxenPlugin.isFoliaServer) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, period);
            return 1;
        } else return plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

    public static void cancelTask(Plugin plugin, int id) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
        else plugin.getServer().getScheduler().cancelTask(id);
    }

    public static void execute(Runnable runnable, Plugin plugin) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
        else plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    public static void executeAsync(Runnable runnable, Plugin plugin) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
        else plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

}
