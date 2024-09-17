package io.th0rgal.oraxen.utils.schedulers;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.scheduler.AdaptedTask;
import io.th0rgal.oraxen.api.scheduler.AdaptedTaskRunnable;
import io.th0rgal.oraxen.api.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public class SpigotSchedulerAdapter implements SchedulerAdapter {

    @Override
    public AdaptedTask runTaskAsynchronously(Runnable task) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskAsynchronously(OraxenPlugin.get(), task);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskLaterAsynchronously(Runnable task, long delay) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskLaterAsynchronously(OraxenPlugin.get(), task, delay);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(OraxenPlugin.get(), task, delay, period);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTask(Runnable task) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTask(OraxenPlugin.get(), task);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskLater(Runnable task, long delay) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), task, delay);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runTaskTimer(Runnable task, long delay, long period) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskTimer(OraxenPlugin.get(), task, delay, period);
        AdaptedTask adaptedTask = new AdaptedTask(scheduledTask);
        if (task instanceof AdaptedTaskRunnable runnable) {
            runnable.setAdaptedTask(adaptedTask);
        }
        return adaptedTask;
    }

    @Override
    public AdaptedTask runEntityTask(Entity entity, Runnable task, Runnable retired) {
        return runTask(task);
    }

    @Override
    public AdaptedTask runEntityTaskLater(Entity entity, Runnable task, Runnable retired, long delay) {
        return runTaskLater(task, delay);
    }

    @Override
    public AdaptedTask runEntityTaskTimer(Entity entity, Runnable task, Runnable retired, long delay, long period) {
        return runTaskTimer(task, delay, period);
    }

    @Override
    public AdaptedTask runRegionTask(Location location, Runnable task) {
        return runTask(task);
    }

    @Override
    public void runRegionTaskNow(Location location, Runnable task) {
        runTask(task);
    }

    @Override
    public AdaptedTask runRegionTask(Chunk chunk, Runnable task) {
        return runTask(task);
    }

    @Override
    public AdaptedTask runRegionTaskLater(Location location, Runnable task, long delay) {
        return runTaskLater(task, delay);
    }

    @Override
    public AdaptedTask runRegionTaskLater(Chunk chunk, Runnable task, long delay) {
        return runTaskLater(task, delay);
    }

    @Override
    public AdaptedTask runRegionTaskTimer(Chunk chunk, Runnable task, long delay, long period) {
        return runTaskTimer(task, delay, period);
    }

    @Override
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(OraxenPlugin.get());
    }

    @Override
    public AdaptedTask runRegionTaskTimer(Location location, Runnable task, long delay, long period) {
        return runTaskTimer(task, delay, period);
    }

    /**
     * Runs a task tied to a specific region (based on Location), executing it immediately.
     *
     * @param chunk The chunk associated with the task.
     * @param task  The task to be executed.
     */
    @Override
    public void runRegionTaskNow(Chunk chunk, Runnable task) {
        runTask(task);
    }

}
