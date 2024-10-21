package io.th0rgal.oraxen.utils.schedulers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.scheduler.AdaptedTask;
import io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable;
import io.th0rgal.oraxen.api.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    @Override
    public AdaptedTask runTaskAsynchronously(Runnable task) {
        ScheduledTask scheduledTask = Bukkit.getAsyncScheduler().runNow(OraxenPlugin.get(), asyncTask -> task.run());
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskLaterAsynchronously(Runnable task, long delay) {
        ScheduledTask scheduledTask = Bukkit.getAsyncScheduler().runDelayed(OraxenPlugin.get(), asyncTask -> task.run(), Math.max(1L, delay) * 50, TimeUnit.MILLISECONDS);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        ScheduledTask scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(OraxenPlugin.get(), asyncTask -> task.run(), Math.max(1L, delay) * 50, Math.max(1L, period) * 50, TimeUnit.MILLISECONDS);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTask(Runnable task) {
        ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().run(OraxenPlugin.get(), globalTask -> task.run());
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskLater(Runnable task, long delay) {
        ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(OraxenPlugin.get(), globalTask -> task.run(), Math.max(1L, delay));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskTimer(Runnable task, long delay, long period) {
        ScheduledTask scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(OraxenPlugin.get(), globalTask -> task.run(), Math.max(1L, delay), Math.max(1L, period));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runEntityTask(Entity entity, Runnable task, Runnable retired) {
        ScheduledTask scheduledTask = entity.getScheduler().run(OraxenPlugin.get(), entityTask -> task.run(), retired);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runEntityTaskLater(Entity entity, Runnable task, Runnable retired, long delay) {
        ScheduledTask scheduledTask = entity.getScheduler().runDelayed(OraxenPlugin.get(), entityTask -> task.run(), retired, Math.max(1L, delay));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runEntityTaskTimer(Entity entity, Runnable task, Runnable retired, long delay, long period) {
        ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(OraxenPlugin.get(), entityTask -> task.run(), retired, Math.max(1L, delay), Math.max(1L, period));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runRegionTask(Location location, Runnable task) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().run(OraxenPlugin.get(), location, regionTask -> task.run());
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public void runRegionTaskNow(Location location, Runnable task) {
        Bukkit.getRegionScheduler().execute(OraxenPlugin.get(), location, task);
    }

    @Override
    public AdaptedTask runRegionTaskLater(Location location, Runnable task, long delay) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().runDelayed(OraxenPlugin.get(), location, regionTask -> task.run(), Math.max(1L, delay));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runRegionTaskTimer(Location location, Runnable task, long delay, long period) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().runAtFixedRate(OraxenPlugin.get(), location, regionTask -> task.run(), Math.max(1L, delay), Math.max(1L, period));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    /**
     * Runs a task tied to a specific region (based on Location), executing it immediately.
     *
     * @param chunk The chunk associated with the task.
     * @param task  The task to be executed.
     */
    @Override
    public void runRegionTaskNow(Chunk chunk, Runnable task) {
        Bukkit.getRegionScheduler().execute(OraxenPlugin.get(), chunk.getWorld(), chunk.getX(), chunk.getZ(), task);
    }

    @Override
    public AdaptedTask runRegionTask(Chunk chunk, Runnable task) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().run(OraxenPlugin.get(), chunk.getWorld(), chunk.getX(), chunk.getZ(), regionTask -> task.run());
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runRegionTaskLater(Chunk chunk, Runnable task, long delay) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().runDelayed(OraxenPlugin.get(), chunk.getWorld(), chunk.getX(), chunk.getZ(), regionTask -> task.run(), Math.max(1L, delay));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runRegionTaskTimer(Chunk chunk, Runnable task, long delay, long period) {
        ScheduledTask scheduledTask = Bukkit.getRegionScheduler().runAtFixedRate(OraxenPlugin.get(), chunk.getWorld(), chunk.getX(), chunk.getZ(), regionTask -> task.run(), Math.max(1L, delay), Math.max(1L, period));
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public void cancelTasks() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(OraxenPlugin.get());
    }
}
