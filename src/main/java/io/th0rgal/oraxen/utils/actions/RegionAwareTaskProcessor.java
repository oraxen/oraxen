package io.th0rgal.oraxen.utils.actions;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import me.gabytm.util.actions.tasks.TaskProcessor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * {@link TaskProcessor} for the actions library backed by Paper's region-aware
 * schedulers. The library's default {@code SpigotTaskProcessor} routes delayed
 * and async actions through the legacy {@code BukkitScheduler}, which throws
 * {@link UnsupportedOperationException} on Folia; this implementation delegates
 * to {@link SchedulerUtil} instead so delayed click actions keep working.
 */
public class RegionAwareTaskProcessor implements TaskProcessor {

    @Override
    public void runAsync(@NotNull final Runnable task) {
        SchedulerUtil.runTaskAsync(task);
    }

    @Override
    public void runAsync(@NotNull final Runnable task, final long delay) {
        SchedulerUtil.runTaskLaterAsync(delay, task);
    }

    @Override
    public void runSync(@NotNull final Runnable task) {
        // True on Folia region tick threads as well as Paper's main thread, so
        // undelayed actions run inline on the event's owning thread.
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        SchedulerUtil.runTask(task);
    }

    @Override
    public void runSync(@NotNull final Runnable task, final long delay) {
        SchedulerUtil.runTaskLater(delay, task);
    }
}
