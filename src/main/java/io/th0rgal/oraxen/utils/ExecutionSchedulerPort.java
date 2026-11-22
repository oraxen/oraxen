package io.th0rgal.oraxen.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port interface abstracting task scheduling across Paper (Single-Region),
 * Folia (Regionized Multithreading), and deterministic unit test runners.
 */
public interface ExecutionSchedulerPort {

    boolean isGlobalThread();

    SchedulerUtil.ScheduledTask runTask(@Nullable Plugin plugin, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runTaskLater(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runTaskTimer(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runTaskAsync(@Nullable Plugin plugin, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runTaskLaterAsync(@Nullable Plugin plugin, long delayTicks, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runTaskTimerAsync(@Nullable Plugin plugin, long delayTicks, long periodTicks, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runAtLocation(@Nullable Plugin plugin, @NotNull Location location, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runAtLocationLater(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, @NotNull Runnable runnable);

    SchedulerUtil.ScheduledTask runAtLocationTimer(@Nullable Plugin plugin, @NotNull Location location, long delayTicks, long periodTicks, @NotNull Runnable runnable);

    void runOnOwningThread(@NotNull Entity entity, @NotNull Runnable runnable);

    @Nullable
    SchedulerUtil.ScheduledTask runForEntity(@Nullable Plugin plugin, @NotNull Entity entity, @NotNull Runnable runnable, @Nullable Runnable retired);

    @Nullable
    SchedulerUtil.ScheduledTask runForEntityLater(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, @NotNull Runnable runnable, @Nullable Runnable retired);

    @Nullable
    SchedulerUtil.ScheduledTask runForEntityTimer(@Nullable Plugin plugin, @NotNull Entity entity, long delayTicks, long periodTicks, @NotNull Runnable runnable, @Nullable Runnable retired);
}
