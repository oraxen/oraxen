package io.th0rgal.oraxen.api.scheduler;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * An abstract class that represents a runnable task with an associated {@link AdaptedTask}.
 * This class allows for task cancellation and provides access to the associated AdaptedTask,
 * while offering methods to schedule tasks for entities, regions, and chunks, with support for delays and timers.
 */
public abstract class AdaptedTaskRunnable implements Runnable {

    private AdaptedTask adaptedTask;

    /**
     * Sets the {@link AdaptedTask} associated with this runnable.
     *
     * @param adaptedTask The {@link AdaptedTask} to associate with this runnable.
     */
    public void setAdaptedTask(AdaptedTask adaptedTask) {
        this.adaptedTask = adaptedTask;
    }

    /**
     * Gets the {@link AdaptedTask} associated with this runnable.
     *
     * @return The associated {@link AdaptedTask}, or {@code null} if none is set.
     */
    public AdaptedTask getAdaptedTask() {
        return adaptedTask;
    }

    /**
     * Cancels the associated {@link AdaptedTask}, if one is set.
     */
    public void cancelTask() {
        if (adaptedTask != null) {
            adaptedTask.cancel();
        }
    }

    // ---- Basic Task Execution ----

    /**
     * Runs this task asynchronously.
     *
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTaskAsynchronously() {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTaskAsynchronously(this));
        return getAdaptedTask();
    }

    /**
     * Runs this task asynchronously with a delay.
     *
     * @param delay The delay in ticks before the task is executed.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTaskLaterAsynchronously(long delay) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTaskLaterAsynchronously(this, delay));
        return getAdaptedTask();
    }

    /**
     * Runs this task asynchronously with a repeating timer.
     *
     * @param delay  The delay in ticks before the first execution.
     * @param period The interval in ticks between subsequent executions.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTaskTimerAsynchronously(long delay, long period) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTaskTimerAsynchronously(this, delay, period));
        return getAdaptedTask();
    }

    /**
     * Runs this task synchronously.
     *
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTask() {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTask(this));
        return getAdaptedTask();
    }

    /**
     * Runs this task synchronously with a delay.
     *
     * @param delay The delay in ticks before the task is executed.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTaskLater(long delay) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTaskLater(this, delay));
        return getAdaptedTask();
    }

    /**
     * Runs this task synchronously with a repeating timer.
     *
     * @param delay  The delay in ticks before the first execution.
     * @param period The interval in ticks between subsequent executions.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runTaskTimer(long delay, long period) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runTaskTimer(this, delay, period));
        return getAdaptedTask();
    }

    // ---- Entity-based Task Execution ----

    /**
     * Runs this task tied to a specific entity immediately.
     *
     * @param entity  The entity associated with the task.
     * @param retired A callback to run when the task is stopped.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runEntityTask(Entity entity, Runnable retired) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runEntityTask(entity, this, retired));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific entity with a delay.
     *
     * @param entity  The entity associated with the task.
     * @param retired A callback to run when the task is stopped.
     * @param delay   The delay in ticks before the task is executed.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runEntityTaskLater(Entity entity, Runnable retired, long delay) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runEntityTaskLater(entity, this, retired, delay));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific entity with a repeating timer.
     *
     * @param entity  The entity associated with the task.
     * @param retired A callback to run when the task is stopped.
     * @param delay   The delay in ticks before the first execution.
     * @param period  The interval in ticks between subsequent executions.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runEntityTaskTimer(Entity entity, Runnable retired, long delay, long period) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runEntityTaskTimer(entity, this, retired, delay, period));
        return getAdaptedTask();
    }

    // ---- Region-based Task Execution ----

    /**
     * Runs this task tied to a specific region (Location) immediately.
     *
     * @param location The location associated with the task.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTask(Location location) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTask(location, this));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific region (Location) with a delay.
     *
     * @param location The location associated with the task.
     * @param delay    The delay in ticks before the task is executed.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTaskLater(Location location, long delay) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTaskLater(location, this, delay));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific region (Location) with a repeating timer.
     *
     * @param location The location associated with the task.
     * @param delay    The delay in ticks before the first execution.
     * @param period   The interval in ticks between subsequent executions.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTaskTimer(Location location, long delay, long period) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTaskTimer(location, this, delay, period));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific chunk immediately.
     *
     * @param chunk The chunk associated with the task.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTask(Chunk chunk) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTask(chunk, this));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific chunk with a delay.
     *
     * @param chunk The chunk associated with the task.
     * @param delay The delay in ticks before the task is executed.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTaskLater(Chunk chunk, long delay) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTaskLater(chunk, this, delay));
        return getAdaptedTask();
    }

    /**
     * Runs this task tied to a specific chunk with a repeating timer.
     *
     * @param chunk  The chunk associated with the task.
     * @param delay  The delay in ticks before the first execution.
     * @param period The interval in ticks between subsequent executions.
     * @return The scheduled {@link AdaptedTask}.
     */
    public AdaptedTask runRegionTaskTimer(Chunk chunk, long delay, long period) {
        SchedulerAdapter scheduler = OraxenPlugin.get().getScheduler();
        setAdaptedTask(scheduler.runRegionTaskTimer(chunk, this, delay, period));
        return getAdaptedTask();
    }

    /**
     * The main method that defines the task to be run.
     * This method must be implemented by subclasses to define the specific behavior of the task.
     */
    @Override
    public abstract void run();
}
