package io.th0rgal.oraxen.api.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a task that can be adapted to work with either Bukkit's {@link BukkitTask} or Folia's {@link ScheduledTask}.
 * Provides methods to cancel the task and check its status.
 */
public class AdaptedTask {

    private final BukkitTask bukkitTask;
    private final ScheduledTask foliaTask;

    /**
     * Constructs an AdaptedTask from a BukkitTask.
     *
     * @param bukkitTask The Bukkit task to adapt.
     */
    public AdaptedTask(BukkitTask bukkitTask) {
        if (bukkitTask == null) {
            throw new IllegalArgumentException("BukkitTask cannot be null.");
        }
        this.bukkitTask = bukkitTask;
        this.foliaTask = null;
    }

    /**
     * Constructs an AdaptedTask from a Folia {@link ScheduledTask}.
     *
     * @param foliaTask The Folia task to adapt.
     */
    public AdaptedTask(ScheduledTask foliaTask) {
        if (foliaTask == null) {
            throw new IllegalArgumentException("Folia ScheduledTask cannot be null.");
        }
        this.bukkitTask = null;
        this.foliaTask = foliaTask;
    }

    /**
     * Cancels the task.
     */
    public void cancel() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
        } else if (foliaTask != null) {
            foliaTask.cancel();
        }
    }

    /**
     * Checks if the task has been cancelled.
     *
     * @return {@code true} if the task is cancelled, {@code false} otherwise.
     */
    public boolean isCancelled() {
        if (bukkitTask != null) {
            return bukkitTask.isCancelled();
        } else if (foliaTask != null) {
            return foliaTask.isCancelled();
        }
        return false;
    }

    /**
     * Gets the plugin that owns this task.
     *
     * @return The owning plugin, or {@code null} if no plugin is associated.
     */
    public Plugin getOwningPlugin() {
        if (bukkitTask != null) {
            return bukkitTask.getOwner();
        } else if (foliaTask != null) {
            return foliaTask.getOwningPlugin();
        }
        return null;
    }
}
