package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for scheduling tasks that works on both Paper and Folia servers.
 * Delegates to Paper's region-aware scheduler APIs (GlobalRegionScheduler, AsyncScheduler,
 * RegionScheduler and EntityScheduler), which behave like the regular main-thread
 * scheduler on non-Folia Paper servers.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
        // Utility class
    }

    // ==================== GLOBAL/SYNC TASKS ====================

    /**
     * Returns whether the caller already owns the global execution context.
     * On Folia, region tick threads are not sufficient for global registry mutation.
     * On non-Folia servers the global context is the main thread; older Paper versions
     * do not expose {@code Bukkit.isGlobalTickThread()}, so it must only be called on Folia.
     */
    public static boolean isGlobalThread() {
        if (VersionUtil.isFoliaServer()) return Bukkit.isGlobalTickThread();
        return Bukkit.isPrimaryThread();
    }

    /**
     * Runs a task on the next server tick (global region on Folia, main thread on Paper).
     */
    public static ScheduledTask runTask(Runnable runnable) {
        return runTask(OraxenPlugin.get(), runnable);
    }

    /**
     * Runs a task on the next server tick (global region on Folia, main thread on Paper).
     */
    public static ScheduledTask runTask(Plugin plugin, Runnable runnable) {
        return new ScheduledTask(Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run()));
    }

    /**
     * Runs a task after the specified delay in ticks.
     */
    public static ScheduledTask runTaskLater(long delayTicks, Runnable runnable) {
        return runTaskLater(OraxenPlugin.get(), delayTicks, runnable);
    }

    /**
     * Runs a task after the specified delay in ticks.
     */
    public static ScheduledTask runTaskLater(Plugin plugin, long delayTicks, Runnable runnable) {
        return new ScheduledTask(Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, task -> runnable.run(), Math.max(1, delayTicks)));
    }

    /**
     * Runs a task repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runTaskTimer(long delayTicks, long periodTicks, Runnable runnable) {
        return runTaskTimer(OraxenPlugin.get(), delayTicks, periodTicks, runnable);
    }

    /**
     * Runs a task repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runTaskTimer(Plugin plugin, long delayTicks, long periodTicks, Runnable runnable) {
        return new ScheduledTask(Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(plugin, task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks)));
    }

    // ==================== ASYNC TASKS ====================

    /**
     * Runs a task asynchronously.
     */
    public static ScheduledTask runTaskAsync(Runnable runnable) {
        return runTaskAsync(OraxenPlugin.get(), runnable);
    }

    /**
     * Runs a task asynchronously.
     */
    public static ScheduledTask runTaskAsync(Plugin plugin, Runnable runnable) {
        return new ScheduledTask(Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run()));
    }

    /**
     * Runs a task asynchronously after the specified delay in ticks.
     */
    public static ScheduledTask runTaskLaterAsync(long delayTicks, Runnable runnable) {
        return runTaskLaterAsync(OraxenPlugin.get(), delayTicks, runnable);
    }

    /**
     * Runs a task asynchronously after the specified delay in ticks.
     */
    public static ScheduledTask runTaskLaterAsync(Plugin plugin, long delayTicks, Runnable runnable) {
        // AsyncScheduler uses time units, convert ticks to milliseconds (1 tick = 50ms)
        return new ScheduledTask(Bukkit.getAsyncScheduler()
                .runDelayed(plugin, task -> runnable.run(), Math.max(1, delayTicks) * 50, TimeUnit.MILLISECONDS));
    }

    /**
     * Runs a task asynchronously with the specified delay and period in ticks.
     */
    public static ScheduledTask runTaskTimerAsync(long delayTicks, long periodTicks, Runnable runnable) {
        return runTaskTimerAsync(OraxenPlugin.get(), delayTicks, periodTicks, runnable);
    }

    /**
     * Runs a task asynchronously with the specified delay and period in ticks.
     */
    public static ScheduledTask runTaskTimerAsync(Plugin plugin, long delayTicks, long periodTicks, Runnable runnable) {
        // AsyncScheduler uses time units, convert ticks to milliseconds (1 tick = 50ms)
        return new ScheduledTask(Bukkit.getAsyncScheduler()
                .runAtFixedRate(plugin, task -> runnable.run(), Math.max(1, delayTicks) * 50, Math.max(1, periodTicks) * 50, TimeUnit.MILLISECONDS));
    }

    // ==================== LOCATION-BASED TASKS (Region Scheduler) ====================

    /**
     * Runs a task at a specific location (uses the RegionScheduler).
     * This is important for Folia as it ensures the task runs in the correct region thread.
     */
    public static ScheduledTask runAtLocation(Location location, Runnable runnable) {
        return runAtLocation(OraxenPlugin.get(), location, runnable);
    }

    /**
     * Runs a task at a specific location (uses the RegionScheduler).
     */
    public static ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        return new ScheduledTask(Bukkit.getRegionScheduler().run(plugin, location, task -> runnable.run()));
    }

    /**
     * Runs a task at a specific location after the specified delay in ticks.
     */
    public static ScheduledTask runAtLocationLater(Location location, long delayTicks, Runnable runnable) {
        return runAtLocationLater(OraxenPlugin.get(), location, delayTicks, runnable);
    }

    /**
     * Runs a task at a specific location after the specified delay in ticks.
     */
    public static ScheduledTask runAtLocationLater(Plugin plugin, Location location, long delayTicks, Runnable runnable) {
        return new ScheduledTask(Bukkit.getRegionScheduler()
                .runDelayed(plugin, location, task -> runnable.run(), Math.max(1, delayTicks)));
    }

    /**
     * Runs a task at a specific location repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runAtLocationTimer(Location location, long delayTicks, long periodTicks, Runnable runnable) {
        return runAtLocationTimer(OraxenPlugin.get(), location, delayTicks, periodTicks, runnable);
    }

    /**
     * Runs a task at a specific location repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runAtLocationTimer(Plugin plugin, Location location, long delayTicks, long periodTicks, Runnable runnable) {
        return new ScheduledTask(Bukkit.getRegionScheduler()
                .runAtFixedRate(plugin, location, task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks)));
    }

    // ==================== ENTITY-BASED TASKS (Entity Scheduler) ====================

    /**
     * Runs a task for a specific entity (uses the EntityScheduler).
     * This ensures the task runs on the thread that owns the entity.
     * If the entity is retired before execution, the task is silently skipped.
     *
     * @param entity   The entity to schedule the task for
     * @param runnable The task to run
     */
    public static ScheduledTask runForEntity(Entity entity, Runnable runnable) {
        return runForEntity(OraxenPlugin.get(), entity, runnable, null);
    }

    /**
     * Runs a task for a specific entity (uses the EntityScheduler).
     * This ensures the task runs on the thread that owns the entity.
     *
     * @param entity   The entity to schedule the task for
     * @param runnable The task to run
     * @param retired  The runnable to run if the entity is retired (removed) before the task runs
     */
    public static ScheduledTask runForEntity(Entity entity, Runnable runnable, Runnable retired) {
        return runForEntity(OraxenPlugin.get(), entity, runnable, retired);
    }

    /**
     * Runs a task for a specific entity (uses the EntityScheduler).
     */
    public static ScheduledTask runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                entity.getScheduler().run(plugin, t -> runnable.run(), retired);
        return task != null ? new ScheduledTask(task) : null;
    }

    /**
     * Runs a task for a specific entity after the specified delay in ticks.
     * If the entity is retired before execution, the task is silently skipped.
     *
     * @param entity     The entity to schedule the task for
     * @param delayTicks The delay in ticks
     * @param runnable   The task to run
     */
    public static ScheduledTask runForEntityLater(Entity entity, long delayTicks, Runnable runnable) {
        return runForEntityLater(OraxenPlugin.get(), entity, delayTicks, runnable, null);
    }

    /**
     * Runs a task for a specific entity after the specified delay in ticks.
     */
    public static ScheduledTask runForEntityLater(Entity entity, long delayTicks, Runnable runnable, Runnable retired) {
        return runForEntityLater(OraxenPlugin.get(), entity, delayTicks, runnable, retired);
    }

    /**
     * Runs a task for a specific entity after the specified delay in ticks.
     */
    public static ScheduledTask runForEntityLater(Plugin plugin, Entity entity, long delayTicks, Runnable runnable, Runnable retired) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                entity.getScheduler().runDelayed(plugin, t -> runnable.run(), retired, Math.max(1, delayTicks));
        return task != null ? new ScheduledTask(task) : null;
    }

    /**
     * Runs a task for a specific entity repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runForEntityTimer(Entity entity, long delayTicks, long periodTicks, Runnable runnable, Runnable retired) {
        return runForEntityTimer(OraxenPlugin.get(), entity, delayTicks, periodTicks, runnable, retired);
    }

    /**
     * Runs a task for a specific entity repeatedly with the specified delay and period in ticks.
     */
    public static ScheduledTask runForEntityTimer(Plugin plugin, Entity entity, long delayTicks, long periodTicks, Runnable runnable, Runnable retired) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask task = entity.getScheduler()
                .runAtFixedRate(plugin, t -> runnable.run(), retired, Math.max(1, delayTicks), Math.max(1, periodTicks));
        return task != null ? new ScheduledTask(task) : null;
    }

    /**
     * Wrapper around Paper's {@link io.papermc.paper.threadedregions.scheduler.ScheduledTask}.
     */
    public static class ScheduledTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

        public ScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
            this.task = task;
        }

        /**
         * Cancels this scheduled task.
         */
        public void cancel() {
            if (task != null) task.cancel();
        }

        /**
         * Checks if the task is cancelled.
         */
        public boolean isCancelled() {
            return task == null || task.isCancelled();
        }

        /**
         * Gets the underlying task object.
         */
        public io.papermc.paper.threadedregions.scheduler.ScheduledTask getTask() {
            return task;
        }
    }
}
