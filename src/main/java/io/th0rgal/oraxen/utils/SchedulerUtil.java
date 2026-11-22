package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for scheduling tasks that works on both Paper and Folia servers,
 * delegating to {@link ExecutionSchedulerPort}.
 */
public final class SchedulerUtil {

    private static final ExecutionSchedulerPort DEFAULT_BACKEND = new PaperFoliaSchedulerBackend();
    private static volatile ExecutionSchedulerPort backend = DEFAULT_BACKEND;

    private SchedulerUtil() {
    }

    public static @NotNull ExecutionSchedulerPort getBackend() {
        return backend;
    }

    public static void setBackend(@Nullable ExecutionSchedulerPort customBackend) {
        backend = Objects.requireNonNullElse(customBackend, DEFAULT_BACKEND);
    }

    public static void resetBackend() {
        backend = DEFAULT_BACKEND;
    }

    private static final class PaperFoliaSchedulerBackend implements ExecutionSchedulerPort {

        @Override
        public boolean isGlobalThread() {
            if (VersionUtil.isFoliaServer()) return Bukkit.isGlobalTickThread();
            return Bukkit.isPrimaryThread();
        }

        @Override
        public ScheduledTask runTask(@Nullable Plugin plugin, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getGlobalRegionScheduler().run(plugin, task -> runnable.run()));
        }

        @Override
        public ScheduledTask runTaskLater(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(plugin, task -> runnable.run(), Math.max(1, delayTicks)));
        }

        @Override
        public ScheduledTask runTaskTimer(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks)));
        }

        @Override
        public ScheduledTask runTaskAsync(@Nullable Plugin plugin, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run()));
        }

        @Override
        public ScheduledTask runTaskLaterAsync(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getAsyncScheduler()
                    .runDelayed(plugin, task -> runnable.run(), Math.max(1, delayTicks) * 50, TimeUnit.MILLISECONDS));
        }

        @Override
        public ScheduledTask runTaskTimerAsync(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getAsyncScheduler()
                    .runAtFixedRate(plugin, task -> runnable.run(), Math.max(1, delayTicks) * 50, Math.max(1, periodTicks) * 50, TimeUnit.MILLISECONDS));
        }

        @Override
        public ScheduledTask runAtLocation(@Nullable Plugin plugin, @NotNull Location location, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getRegionScheduler().run(plugin, location, task -> runnable.run()));
        }

        @Override
        public ScheduledTask runAtLocationLater(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getRegionScheduler()
                    .runDelayed(plugin, location, task -> runnable.run(), Math.max(1, delayTicks)));
        }

        @Override
        public ScheduledTask runAtLocationTimer(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, long periodTicks, @NotNull Runnable runnable) {
            return new ScheduledTask(Bukkit.getRegionScheduler()
                    .runAtFixedRate(plugin, location, task -> runnable.run(), Math.max(1, delayTicks), Math.max(1, periodTicks)));
        }

        @Override
        public void runOnOwningThread(@NotNull Entity entity, @NotNull Runnable runnable) {
            if (Bukkit.isOwnedByCurrentRegion(entity)) {
                runnable.run();
            } else {
                runForEntity(OraxenPlugin.get(), entity, runnable, null);
            }
        }

        @Override
        public @Nullable ScheduledTask runForEntity(@Nullable Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @Nullable Runnable retired) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                    entity.getScheduler().run(plugin, t -> runnable.run(), retired);
            return task != null ? new ScheduledTask(task) : null;
        }

        @Override
        public @Nullable ScheduledTask runForEntityLater(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, @NotNull Runnable runnable, @Nullable Runnable retired) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task =
                    entity.getScheduler().runDelayed(plugin, t -> runnable.run(), retired, Math.max(1, delayTicks));
            return task != null ? new ScheduledTask(task) : null;
        }

        @Override
        public @Nullable ScheduledTask runForEntityTimer(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, long periodTicks, @NotNull Runnable runnable, @Nullable Runnable retired) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = entity.getScheduler()
                    .runAtFixedRate(plugin, t -> runnable.run(), retired, Math.max(1, delayTicks), Math.max(1, periodTicks));
            return task != null ? new ScheduledTask(task) : null;
        }
    }

    // ==================== GLOBAL/SYNC TASKS ====================

    public static boolean isGlobalThread() {
        return backend.isGlobalThread();
    }

    public static ScheduledTask runTask(Runnable runnable) {
        return runTask(OraxenPlugin.get(), runnable);
    }

    public static ScheduledTask runTask(Plugin plugin, Runnable runnable) {
        return backend.runTask(plugin, runnable);
    }

    public static ScheduledTask runTaskLater(long delayTicks, Runnable runnable) {
        return runTaskLater(OraxenPlugin.get(), delayTicks, runnable);
    }

    public static ScheduledTask runTaskLater(Plugin plugin, long delayTicks, Runnable runnable) {
        return backend.runTaskLater(plugin, delayTicks, runnable);
    }

    public static ScheduledTask runTaskTimer(long delayTicks, long periodTicks, Runnable runnable) {
        return runTaskTimer(OraxenPlugin.get(), delayTicks, periodTicks, runnable);
    }

    public static ScheduledTask runTaskTimer(Plugin plugin, long delayTicks, long periodTicks, Runnable runnable) {
        return backend.runTaskTimer(plugin, delayTicks, periodTicks, runnable);
    }

    // ==================== ASYNC TASKS ====================

    public static ScheduledTask runTaskAsync(Runnable runnable) {
        return runTaskAsync(OraxenPlugin.get(), runnable);
    }

    public static ScheduledTask runTaskAsync(Plugin plugin, Runnable runnable) {
        return backend.runTaskAsync(plugin, runnable);
    }

    public static ScheduledTask runTaskLaterAsync(long delayTicks, Runnable runnable) {
        return runTaskLaterAsync(OraxenPlugin.get(), delayTicks, runnable);
    }

    public static ScheduledTask runTaskLaterAsync(Plugin plugin, long delayTicks, Runnable runnable) {
        return backend.runTaskLaterAsync(plugin, delayTicks, runnable);
    }

    public static ScheduledTask runTaskTimerAsync(long delayTicks, long periodTicks, Runnable runnable) {
        return runTaskTimerAsync(OraxenPlugin.get(), delayTicks, periodTicks, runnable);
    }

    public static ScheduledTask runTaskTimerAsync(Plugin plugin, long delayTicks, long periodTicks, Runnable runnable) {
        return backend.runTaskTimerAsync(plugin, delayTicks, periodTicks, runnable);
    }

    // ==================== LOCATION-BASED TASKS (Region Scheduler) ====================

    public static ScheduledTask runAtLocation(Location location, Runnable runnable) {
        return runAtLocation(OraxenPlugin.get(), location, runnable);
    }

    public static ScheduledTask runAtLocation(Plugin plugin, Location location, Runnable runnable) {
        return backend.runAtLocation(plugin, location, runnable);
    }

    public static ScheduledTask runAtLocationLater(Location location, long delayTicks, Runnable runnable) {
        return runAtLocationLater(OraxenPlugin.get(), location, delayTicks, runnable);
    }

    public static ScheduledTask runAtLocationLater(Plugin plugin, Location location, long delayTicks, Runnable runnable) {
        return backend.runAtLocationLater(plugin, location, delayTicks, runnable);
    }

    public static ScheduledTask runAtLocationTimer(Location location, long delayTicks, long periodTicks, Runnable runnable) {
        return runAtLocationTimer(OraxenPlugin.get(), location, delayTicks, periodTicks, runnable);
    }

    public static ScheduledTask runAtLocationTimer(Plugin plugin, Location location, long delayTicks, long periodTicks, Runnable runnable) {
        return backend.runAtLocationTimer(plugin, location, delayTicks, periodTicks, runnable);
    }

    // ==================== ENTITY-BASED TASKS (Entity Scheduler) ====================

    public static void runOnOwningThread(Entity entity, Runnable runnable) {
        backend.runOnOwningThread(entity, runnable);
    }

    public static ScheduledTask runForEntity(Entity entity, Runnable runnable) {
        return runForEntity(OraxenPlugin.get(), entity, runnable, null);
    }

    public static ScheduledTask runForEntity(Entity entity, Runnable runnable, Runnable retired) {
        return runForEntity(OraxenPlugin.get(), entity, runnable, retired);
    }

    public static ScheduledTask runForEntity(Plugin plugin, Entity entity, Runnable runnable, Runnable retired) {
        return backend.runForEntity(plugin, entity, runnable, retired);
    }

    public static ScheduledTask runForEntityLater(Entity entity, long delayTicks, Runnable runnable) {
        return runForEntityLater(OraxenPlugin.get(), entity, delayTicks, runnable, null);
    }

    public static ScheduledTask runForEntityLater(Entity entity, long delayTicks, Runnable runnable, Runnable retired) {
        return runForEntityLater(OraxenPlugin.get(), entity, delayTicks, runnable, retired);
    }

    public static ScheduledTask runForEntityLater(Plugin plugin, Entity entity, long delayTicks, Runnable runnable, Runnable retired) {
        return backend.runForEntityLater(plugin, entity, delayTicks, runnable, retired);
    }

    public static ScheduledTask runForEntityTimer(Entity entity, long delayTicks, long periodTicks, Runnable runnable, Runnable retired) {
        return runForEntityTimer(OraxenPlugin.get(), entity, delayTicks, periodTicks, runnable, retired);
    }

    public static ScheduledTask runForEntityTimer(Plugin plugin, Entity entity, long delayTicks, long periodTicks, Runnable runnable, Runnable retired) {
        return backend.runForEntityTimer(plugin, entity, delayTicks, periodTicks, runnable, retired);
    }

    /**
     * Wrapper around Paper's {@link io.papermc.paper.threadedregions.scheduler.ScheduledTask}.
     */
    public static class ScheduledTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

        public ScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
            this.task = task;
        }

        public void cancel() {
            if (task != null) task.cancel();
        }

        public boolean isCancelled() {
            return task == null || task.isCancelled();
        }

        public io.papermc.paper.threadedregions.scheduler.ScheduledTask getTask() {
            return task;
        }
    }
}
