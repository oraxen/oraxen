package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Utility class for scheduling tasks that works on both Bukkit/Paper and Folia servers.
 * On Folia, this uses the RegionScheduler and EntityScheduler APIs.
 * On regular Bukkit/Paper, this falls back to the standard BukkitScheduler.
 */
public final class SchedulerUtil {

    private static Object globalRegionScheduler;
    private static Object asyncScheduler;
    private static Method globalRunMethod;
    private static Method globalRunDelayedMethod;
    private static Method globalRunAtFixedRateMethod;
    private static Method asyncRunMethod;
    private static Method asyncRunDelayedMethod;
    private static Method asyncRunAtFixedRateMethod;
    private static Method regionRunMethod;
    private static Method regionRunDelayedMethod;
    private static Method regionRunAtFixedRateMethod;
    private static Method entityRunMethod;
    private static Method entityRunDelayedMethod;
    private static Method entityRunAtFixedRateMethod;
    private static Method taskCancelMethod;

    private static boolean foliaInitialized = false;

    static {
        if (VersionUtil.isFoliaServer()) {
            try {
                initializeFoliaSchedulers();
                foliaInitialized = true;
            } catch (Exception e) {
                e.printStackTrace();
                foliaInitialized = false;
            }
        }
    }

    private static void initializeFoliaSchedulers() throws Exception {
        // Get the GlobalRegionScheduler
        Method getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
        globalRegionScheduler = getGlobalRegionScheduler.invoke(null);

        // Get the AsyncScheduler
        Method getAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
        asyncScheduler = getAsyncScheduler.invoke(null);

        // Get GlobalRegionScheduler methods
        Class<?> globalSchedulerClass = globalRegionScheduler.getClass();
        globalRunMethod = globalSchedulerClass.getMethod("run", Plugin.class, Consumer.class);
        globalRunDelayedMethod = globalSchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
        globalRunAtFixedRateMethod = globalSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

        // Get AsyncScheduler methods
        Class<?> asyncSchedulerClass = asyncScheduler.getClass();
        asyncRunMethod = asyncSchedulerClass.getMethod("runNow", Plugin.class, Consumer.class);
        asyncRunDelayedMethod = asyncSchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
        asyncRunAtFixedRateMethod = asyncSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);

        // Get RegionScheduler methods (for location-based scheduling)
        Method getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
        Object regionScheduler = getRegionScheduler.invoke(null);
        Class<?> regionSchedulerClass = regionScheduler.getClass();
        regionRunMethod = regionSchedulerClass.getMethod("run", Plugin.class, Location.class, Consumer.class);
        regionRunDelayedMethod = regionSchedulerClass.getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
        regionRunAtFixedRateMethod = regionSchedulerClass.getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class);

        // Get ScheduledTask cancel method
        Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
        taskCancelMethod = scheduledTaskClass.getMethod("cancel");

        // Get Entity scheduler methods
        Class<?> entityClass = Entity.class;
        Method getSchedulerMethod = entityClass.getMethod("getScheduler");
        // We'll call this per-entity, so we just need the method references from TaskScheduler
        Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
        entityRunMethod = entitySchedulerClass.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
        entityRunDelayedMethod = entitySchedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
        entityRunAtFixedRateMethod = entitySchedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
    }

    private SchedulerUtil() {
        // Utility class
    }

    // ==================== GLOBAL/SYNC TASKS ====================

    /**
     * Runs a task on the next server tick (global region on Folia, main thread on Bukkit).
     */
    public static ScheduledTask runTask(Runnable runnable) {
        return runTask(OraxenPlugin.get(), runnable);
    }

    /**
     * Runs a task on the next server tick (global region on Folia, main thread on Bukkit).
     */
    public static ScheduledTask runTask(Plugin plugin, Runnable runnable) {
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object task = globalRunMethod.invoke(globalRegionScheduler, plugin, consumer);
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                // Folia uses ticks for delay
                Object task = globalRunDelayedMethod.invoke(globalRegionScheduler, plugin, consumer, Math.max(1, delayTicks));
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia delayed task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object task = globalRunAtFixedRateMethod.invoke(globalRegionScheduler, plugin, consumer, Math.max(1, delayTicks), Math.max(1, periodTicks));
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia timer task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object task = asyncRunMethod.invoke(asyncScheduler, plugin, consumer);
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia async task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                // Folia AsyncScheduler uses TimeUnit, convert ticks to milliseconds (1 tick = 50ms)
                long delayMs = delayTicks * 50;
                Object task = asyncRunDelayedMethod.invoke(asyncScheduler, plugin, consumer, delayMs, TimeUnit.MILLISECONDS);
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia async delayed task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                // Folia AsyncScheduler uses TimeUnit, convert ticks to milliseconds (1 tick = 50ms)
                long delayMs = delayTicks * 50;
                long periodMs = periodTicks * 50;
                Object task = asyncRunAtFixedRateMethod.invoke(asyncScheduler, plugin, consumer, delayMs, periodMs, TimeUnit.MILLISECONDS);
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia async timer task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
            return new ScheduledTask(task);
        }
    }

    // ==================== LOCATION-BASED TASKS (Region Scheduler) ====================

    /**
     * Runs a task at a specific location (uses RegionScheduler on Folia).
     * This is important for Folia as it ensures the task runs in the correct region thread.
     */
    public static ScheduledTask runAtLocation(Location location, Runnable runnable) {
        return runAtLocation(OraxenPlugin.get(), location, runnable);
    }

    /**
     * Runs a task at a specific location (uses RegionScheduler on Folia).
     */
    public static ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
                Object regionScheduler = getRegionScheduler.invoke(null);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = regionRunMethod.invoke(regionScheduler, plugin, location, consumer);
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia region task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
                Object regionScheduler = getRegionScheduler.invoke(null);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = regionRunDelayedMethod.invoke(regionScheduler, plugin, location, consumer, Math.max(1, delayTicks));
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia region delayed task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
                Object regionScheduler = getRegionScheduler.invoke(null);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = regionRunAtFixedRateMethod.invoke(regionScheduler, plugin, location, consumer, Math.max(1, delayTicks), Math.max(1, periodTicks));
                return new ScheduledTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia region timer task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            return new ScheduledTask(task);
        }
    }

    // ==================== ENTITY-BASED TASKS (Entity Scheduler) ====================

    /**
     * Runs a task for a specific entity (uses EntityScheduler on Folia).
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
     * Runs a task for a specific entity (uses EntityScheduler on Folia).
     */
    public static ScheduledTask runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getSchedulerMethod = Entity.class.getMethod("getScheduler");
                Object entityScheduler = getSchedulerMethod.invoke(entity);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = entityRunMethod.invoke(entityScheduler, plugin, consumer, retired);
                return task != null ? new ScheduledTask(task) : null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia entity task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getSchedulerMethod = Entity.class.getMethod("getScheduler");
                Object entityScheduler = getSchedulerMethod.invoke(entity);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = entityRunDelayedMethod.invoke(entityScheduler, plugin, consumer, retired, Math.max(1, delayTicks));
                return task != null ? new ScheduledTask(task) : null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia entity delayed task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return new ScheduledTask(task);
        }
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
        if (VersionUtil.isFoliaServer() && foliaInitialized) {
            try {
                Method getSchedulerMethod = Entity.class.getMethod("getScheduler");
                Object entityScheduler = getSchedulerMethod.invoke(entity);
                Consumer<Object> consumer = task -> runnable.run();
                Object task = entityRunAtFixedRateMethod.invoke(entityScheduler, plugin, consumer, retired, Math.max(1, delayTicks), Math.max(1, periodTicks));
                return task != null ? new ScheduledTask(task) : null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to schedule Folia entity timer task", e);
            }
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            return new ScheduledTask(task);
        }
    }

    /**
     * Cancels a task by its ID (Bukkit only, no-op on Folia since we use ScheduledTask).
     */
    public static void cancelTask(int taskId) {
        if (!VersionUtil.isFoliaServer()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    /**
     * Wrapper class for scheduled tasks that works with both Bukkit and Folia.
     */
    public static class ScheduledTask {
        private final Object task;
        private final boolean isFolia;

        public ScheduledTask(Object task) {
            this.task = task;
            this.isFolia = VersionUtil.isFoliaServer() && !(task instanceof BukkitTask);
        }

        /**
         * Cancels this scheduled task.
         */
        public void cancel() {
            if (task == null) return;
            
            if (isFolia) {
                try {
                    if (taskCancelMethod != null) {
                        taskCancelMethod.invoke(task);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to cancel Folia task", e);
                }
            } else if (task instanceof BukkitTask bukkitTask) {
                bukkitTask.cancel();
            }
        }

        /**
         * Gets the task ID (Bukkit only, returns -1 on Folia).
         */
        public int getTaskId() {
            if (!isFolia && task instanceof BukkitTask bukkitTask) {
                return bukkitTask.getTaskId();
            }
            return -1;
        }

        /**
         * Checks if the task is cancelled.
         */
        public boolean isCancelled() {
            if (task == null) return true;
            
            if (isFolia) {
                try {
                    Method isCancelledMethod = task.getClass().getMethod("isCancelled");
                    return (boolean) isCancelledMethod.invoke(task);
                } catch (Exception e) {
                    return false;
                }
            } else if (task instanceof BukkitTask bukkitTask) {
                return bukkitTask.isCancelled();
            }
            return false;
        }

        /**
         * Gets the underlying task object.
         */
        public Object getTask() {
            return task;
        }
    }
}
