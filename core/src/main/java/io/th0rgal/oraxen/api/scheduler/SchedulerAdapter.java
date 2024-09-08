package io.th0rgal.oraxen.api.scheduler;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * The SchedulerAdapter interface defines various methods to run and manage tasks either asynchronously,
 * globally, or on specific entities or regions within a Minecraft server.
 * It allows tasks to be scheduled to run once, after a delay, or repeatedly with a given interval.
 */
public interface SchedulerAdapter {

    /**
     * Runs a task asynchronously immediately.
     *
     * @param task The task to be executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTaskAsynchronously(Runnable task);

    /**
     * Runs a task asynchronously after a specified delay.
     *
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTaskLaterAsynchronously(Runnable task, long delay);

    /**
     * Runs a task asynchronously with a repeating timer.
     *
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is first executed.
     * @param period The interval in ticks between subsequent executions of the task.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTaskTimerAsynchronously(Runnable task, long delay, long period);


    /**
     * Runs a task synchronously immediately.
     *
     * @param task The task to be executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTask(Runnable task);

    /**
     * Runs a task synchronously after a specified delay.
     *
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTaskLater(Runnable task, long delay);

    /**
     * Runs a task synchronously with a repeating timer.
     *
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is first executed.
     * @param period The interval in ticks between subsequent executions of the task.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runTaskTimer(Runnable task, long delay, long period);


    /**
     * Runs a task tied to a specific entity, executing it immediately.
     *
     * @param entity The entity associated with the task.
     * @param task The task to be executed.
     * @param retired A callback to be executed when the task is retired or stopped.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runEntityTask(Entity entity, Runnable task, Runnable retired);

    /**
     * Runs a task tied to a specific entity after a specified delay.
     *
     * @param entity The entity associated with the task.
     * @param task The task to be executed.
     * @param retired A callback to be executed when the task is retired or stopped.
     * @param delay The delay in ticks before the task is executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runEntityTaskLater(Entity entity, Runnable task, Runnable retired, long delay);

    /**
     * Runs a task tied to a specific entity with a repeating timer.
     *
     * @param entity The entity associated with the task.
     * @param task The task to be executed.
     * @param retired A callback to be executed when the task is retired or stopped.
     * @param delay The delay in ticks before the task is first executed.
     * @param period The interval in ticks between subsequent executions of the task.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runEntityTaskTimer(Entity entity, Runnable task, Runnable retired, long delay, long period);


    /**
     * Runs a task tied to a specific region (based on Location), executing it next tick.
     *
     * @param location The location associated with the task.
     * @param task The task to be executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTask(Location location, Runnable task);

    /**
     * Runs a task tied to a specific region (based on Location), executing it immediately.
     *
     * @param location The location associated with the task.
     * @param task The task to be executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    void runRegionTaskNow(Location location, Runnable task);

    /**
     * Runs a task tied to a specific region (based on Location) after a specified delay.
     *
     * @param location The location associated with the task.
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTaskLater(Location location, Runnable task, long delay);

    /**
     * Runs a task tied to a specific region (based on Location) with a repeating timer.
     *
     * @param location The location associated with the task.
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is first executed.
     * @param period The interval in ticks between subsequent executions of the task.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTaskTimer(Location location, Runnable task, long delay, long period);

    /**
     * Runs a task tied to a specific region (based on Chunk), executing it immediately.
     *
     * @param chunk The chunk associated with the task.
     * @param task The task to be executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTask(Chunk chunk, Runnable task);

    /**
     * Runs a task tied to a specific region (based on Chunk) after a specified delay.
     *
     * @param chunk The chunk associated with the task.
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is executed.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTaskLater(Chunk chunk, Runnable task, long delay);

    /**
     * Runs a task tied to a specific region (based on Chunk) with a repeating timer.
     *
     * @param chunk The chunk associated with the task.
     * @param task The task to be executed.
     * @param delay The delay in ticks before the task is first executed.
     * @param period The interval in ticks between subsequent executions of the task.
     * @return The {@link AdaptedTask} representing the scheduled task.
     */
    AdaptedTask runRegionTaskTimer(Chunk chunk, Runnable task, long delay, long period);

    /**
     * Cancels all tasks associated with the plugin.
     * Typically used to stop any running or scheduled tasks when the plugin is disabled or unloaded.
     */
    void cancelTasks();
}
