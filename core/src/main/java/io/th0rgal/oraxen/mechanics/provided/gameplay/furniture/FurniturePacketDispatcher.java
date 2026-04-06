package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Async dispatcher for furniture-related operations that can be safely offloaded
 * from the main server thread. Uses a single-threaded executor to serialize
 * furniture packet work, preventing main-thread hitches on servers with many
 * furniture entities.
 *
 * <p>Operations that involve Bukkit API calls (entity spawning, block changes)
 * are scheduled back to the appropriate thread via {@link SchedulerUtil},
 * while CPU-intensive preparation work (calculating positions, building metadata)
 * runs on the executor thread.</p>
 */
public final class FurniturePacketDispatcher {

    private static volatile ExecutorService executor;

    private FurniturePacketDispatcher() {}

    /**
     * Initializes the async dispatcher. Called once during plugin enable.
     */
    public static void init() {
        if (executor != null && !executor.isShutdown()) return;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Oraxen-Furniture-Dispatcher");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Shuts down the dispatcher. Called during plugin disable.
     * Waits up to 5 seconds for pending tasks before forcing shutdown.
     */
    public static void shutdown() {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        executor = null;
    }

    /**
     * Submits an async task to the furniture dispatcher.
     * Use this for CPU-intensive preparation work that does not touch the Bukkit API.
     *
     * @param task The task to execute asynchronously
     */
    public static void submitAsync(@NotNull Runnable task) {
        ExecutorService exec = executor;
        if (exec == null || exec.isShutdown()) {
            // Fallback: run synchronously if dispatcher is not initialized
            task.run();
            return;
        }
        exec.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Logs.logError("Error in furniture async task");
                e.printStackTrace();
            }
        });
    }

    /**
     * Prepares data asynchronously via a supplier, then runs the result on the
     * main thread (or entity's region thread on Folia).
     *
     * <p>This is the primary pattern for async furniture handling:
     * <ol>
     *   <li>The supplier runs on the executor thread (CPU-bound work)</li>
     *   <li>The consumer runs on the main/region thread (Bukkit API calls)</li>
     * </ol>
     *
     * @param entity   The entity whose region thread should run the consumer (Folia support)
     * @param supplier Prepares data off the main thread
     * @param consumer Consumes the prepared data on the main/region thread
     * @param <T>      The type of data produced by the supplier
     */
    public static <T> void prepareAndApply(@NotNull Entity entity, @NotNull Supplier<T> supplier,
                                            @NotNull java.util.function.Consumer<T> consumer) {
        submitAsync(() -> {
            T data = supplier.get();
            SchedulerUtil.runForEntity(entity, () -> consumer.accept(data), null);
        });
    }

    /**
     * Teleports an entity asynchronously using Paper's teleportAsync API when available,
     * falling back to synchronous teleport on non-Paper servers.
     *
     * @param entity   The entity to teleport
     * @param location The target location
     */
    public static void teleportAsync(@NotNull Entity entity, @NotNull org.bukkit.Location location) {
        try {
            // Paper API: teleportAsync returns a CompletableFuture
            entity.getClass().getMethod("teleportAsync", org.bukkit.Location.class)
                    .invoke(entity, location);
        } catch (NoSuchMethodException e) {
            // Non-Paper: fall back to sync teleport on the entity's thread
            SchedulerUtil.runForEntity(entity, () -> entity.teleport(location), null);
        } catch (Exception e) {
            Logs.logError("Failed to teleport entity asynchronously");
            e.printStackTrace();
        }
    }

    /**
     * Returns whether the dispatcher is currently active.
     */
    public static boolean isActive() {
        ExecutorService exec = executor;
        return exec != null && !exec.isShutdown();
    }
}
