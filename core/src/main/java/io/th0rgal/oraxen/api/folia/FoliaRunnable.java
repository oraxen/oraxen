package io.th0rgal.oraxen.api.folia;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * A runnable class with both Paper and Folia Support
 *
 * @author Euphyllia
 * @since 2.0
 */
public abstract class FoliaRunnable implements Runnable {

    private ScheduledTask task;
    private @Nullable AsyncScheduler asyncScheduler;
    private @Nullable TimeUnit timeUnit;
    private @Nullable EntityScheduler entityScheduler;
    private @Nullable Runnable entityRetired;
    private @Nullable GlobalRegionScheduler globalRegionScheduler;
    private @Nullable RegionScheduler regionScheduler;
    private @Nullable Location location;
    private @Nullable World world;
    private int chunkX;
    private int chunkZ;

    /**
     * A constructor to build an {@link AsyncScheduler} with a {@link TimeUnit}
     *
     * @param scheduler the {@link AsyncScheduler}
     * @param timeUnit the {@link TimeUnit}
     * @since 2.0
     */
    public FoliaRunnable(@NotNull final AsyncScheduler scheduler, @Nullable final TimeUnit timeUnit) {
        this.asyncScheduler = scheduler;
        this.timeUnit = timeUnit;
    }

    /**
     * A constructor to build an entity scheduler
     *
     * @param scheduler the {@link EntityScheduler}
     * @param retired the {@link Runnable}
     * @since 2.0
     */
    public FoliaRunnable(@NotNull final EntityScheduler scheduler, @Nullable final Runnable retired) {
        this.entityScheduler = scheduler;
        this.entityRetired = retired;
    }

    /**
     * A constructor for the {@link GlobalRegionScheduler}
     *
     * @param scheduler the {@link GlobalRegionScheduler}
     * @since 2.0
     */
    public FoliaRunnable(@NotNull final GlobalRegionScheduler scheduler) {
        this.globalRegionScheduler = scheduler;
    }

    /**
     * A constructor that builds a {@link Runnable} for the {@link Location}'s region.
     *
     * @param scheduler the {@link RegionScheduler} for the {@link Location}
     * @param location the {@link Location}
     * @since 2.0
     */
    public FoliaRunnable(@NotNull final RegionScheduler scheduler, @Nullable final Location location) {
        this.regionScheduler = scheduler;
        this.location = location;
    }

    /**
     * A constructor that builds a {@link Runnable} for the world region.
     *
     * @param scheduler the {@link RegionScheduler} for the world
     * @param world the {@link World}
     * @param chunkX the chunk's {@link Integer}
     * @param chunkZ the chunk's {@link Integer}
     * @since 2.0
     */
    public FoliaRunnable(@NotNull final RegionScheduler scheduler, @Nullable final World world, final int chunkX, final int chunkZ) {
        this.regionScheduler = scheduler;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /**
     * Checks if the task is cancelled or not.
     *
     * @return true or false
     * @throws IllegalStateException throws this exception if unable to check
     * @since 2.0
     */
    public final boolean isCancelled() throws IllegalStateException {
        checkScheduled();

        return task.isCancelled();
    }

    /**
     * Cancels a task.
     *
     * @throws IllegalStateException throws this exception if unable to cancel
     * @since 2.0
     */
    public void cancel() throws IllegalStateException {
        task.cancel();
    }

    /**
     * Runs a task supporting Folia/Paper
     *
     * @param plugin the {@link JavaPlugin}
     * @return a scheduled task
     * @throws IllegalArgumentException throws this exception if it fails
     * @throws IllegalStateException throws this exception if it is unstable
     * @since 2.0
     */
    public @NotNull final ScheduledTask run(@NotNull final JavaPlugin plugin) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();

        if (this.globalRegionScheduler != null) {
            return setupTask(this.globalRegionScheduler.run(plugin, scheduledTask -> this.run()));
        } else if (this.entityScheduler != null) {
            return setupTask(this.entityScheduler.run(plugin, scheduledTask -> this.run(), entityRetired));
        } else if (this.regionScheduler != null) {
            if (this.location != null) {
                return setupTask(this.regionScheduler.run(plugin, location, scheduledTask -> this.run()));
            } else if (world != null) {
                return setupTask(this.regionScheduler.run(plugin, world, chunkX, chunkZ, scheduledTask -> this.run()));
            } else {
                throw new UnsupportedOperationException("The region type is not supported.");
            }
        } else if (this.asyncScheduler != null) {
            return setupTask(this.asyncScheduler.runNow(plugin, scheduledTask -> this.run()));
        } else {
            throw new UnsupportedOperationException("The task type is not supported.");
        }
    }

    /**
     * Schedules this to run after the specified number of server ticks.
     *
     * @param plugin the reference to the {@link JavaPlugin} scheduling task
     * @param delay the ticks to wait before running the task
     * @return a ScheduledTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException if this was already scheduled
     * @since 2.0
     */
    public @NotNull final ScheduledTask runDelayed(@NotNull final JavaPlugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();

        delay = Math.max(1, delay);

        if (this.globalRegionScheduler != null) {
            return setupTask(this.globalRegionScheduler.runDelayed(plugin, scheduledTask -> this.run(), delay));
        } else if (this.entityScheduler != null) {
            return setupTask(this.entityScheduler.runDelayed(plugin, scheduledTask -> this.run(), entityRetired, delay));
        } else if (this.regionScheduler != null) {
            if (this.location != null) {
                return setupTask(this.regionScheduler.runDelayed(plugin, location, scheduledTask -> this.run(), delay));
            } else if (world != null) {
                return setupTask(this.regionScheduler.runDelayed(plugin, world, chunkX, chunkZ, scheduledTask -> this.run(), delay));
            } else {
                throw new UnsupportedOperationException("The region type is not supported.");
            }
        } else if (this.asyncScheduler != null && this.timeUnit != null) {
            return setupTask(this.asyncScheduler.runDelayed(plugin, scheduledTask -> this.run(), delay, timeUnit));
        } else {
            throw new UnsupportedOperationException("The task type is not supported.");
        }
    }

    /**
     * Schedules this to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param plugin the reference to the {@link JavaPlugin} scheduling task
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a ScheduledTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException if this was already scheduled
     * @since 2.0
     */
    public @NotNull final ScheduledTask runAtFixedRate(@NotNull final JavaPlugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();

        delay = Math.max(1, delay);
        period = Math.max(1, period);

        if (this.globalRegionScheduler != null) {
            return setupTask(this.globalRegionScheduler.runAtFixedRate(plugin, scheduledTask -> this.run(), delay, period));
        } else if (this.entityScheduler != null) {
            return setupTask(this.entityScheduler.runAtFixedRate(plugin, scheduledTask -> this.run(), this.entityRetired, delay, period));
        } else if (this.regionScheduler != null) {
            if (this.location != null) {
                return setupTask(this.regionScheduler.runAtFixedRate(plugin, this.location, scheduledTask -> this.run(), delay, period));
            } else if (world != null) {
                return setupTask(this.regionScheduler.runAtFixedRate(plugin, this.world, this.chunkX, this.chunkZ, scheduledTask -> this.run(), delay, period));
            } else {
                throw new UnsupportedOperationException("The region type is not supported.");
            }
        } else if (this.asyncScheduler != null && this.timeUnit != null) {
            return setupTask(this.asyncScheduler.runAtFixedRate(plugin, scheduledTask -> this.run(), delay, period, this.timeUnit));
        } else {
            throw new UnsupportedOperationException("The task type is not supported.");
        }
    }

    /**
     * Gets the task id for this runnable.
     *
     * @return the task id that this runnable was scheduled as
     * @throws IllegalStateException if task was not scheduled yet
     * @since 2.0
     */
    public final int getTaskId() throws IllegalStateException {
        checkScheduled();

        return this.task.hashCode();
    }

    /**
     * Checks scheduled and throws a state exception if null i.e. not scheduled.
     * @since 2.0
     */
    private void checkScheduled() {
        if (this.task == null) throw new IllegalStateException("Not scheduled yet");
    }

    /**
     * Checks if scheduled and throws an illegal state exception if not null i.e. already scheduled.
     * @since 2.0
     */
    private void checkNotYetScheduled() {
        if (this.task != null) throw new IllegalStateException("Already scheduled as " + task.hashCode());
    }

    /**
     * Sets up the {@link ScheduledTask}.
     *
     * @param task the {@link ScheduledTask} to schedule
     * @return the {@link ScheduledTask}
     * @since 2.0
     */
    @NotNull
    private ScheduledTask setupTask(@NotNull final ScheduledTask task) {
        this.task = task;

        return task;
    }
}