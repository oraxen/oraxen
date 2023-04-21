package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class SchedulerUtils {

    public static int scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long period) {
        if (OraxenPlugin.isFoliaServer) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, period);
            return 1;
        } else return plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

    public static void cancelTask(Plugin plugin, int id) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
        else plugin.getServer().getScheduler().cancelTask(id);
    }

    public static void execute(Plugin plugin, Runnable runnable) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
        else plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    public static void executeDelayed(Plugin plugin, Runnable runnable, long delay) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getGlobalRegionScheduler().execute(plugin, runnable);
        else plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
    }

    public static void executeAsync(Plugin plugin, Runnable runnable) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
        else plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void executeAsyncDelayed(Plugin plugin, Runnable runnable, long delay) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, TimeUnit.MILLISECONDS);
        else plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public static void executeRepeating(Plugin plugin, Runnable runnable, long delay, long period) {
        if (OraxenPlugin.isFoliaServer) plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), delay, period, TimeUnit.MILLISECONDS);
        else plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

}
