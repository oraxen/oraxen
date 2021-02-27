package org.playuniverse.snowypine.helper.task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.playuniverse.snowypine.Snowypine;

import com.syntaxphoenix.syntaxapi.thread.SynThreadFactory;
import com.syntaxphoenix.syntaxapi.thread.SynThreadReporter;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public final class TaskHelper {

	public static final TaskHelper TASK = new TaskHelper();

	public static final SynThreadReporter REPORTER = (throwable, var0, var1, var2) -> Snowypine.getCurrentLogger().log(throwable);

	private TaskHelper() {}

	private final Container<ExecutorService> service = Container.of();

	public void start() {
		if (service.isPresent()) {
			return;
		}
		service.replace(Executors.newCachedThreadPool(new SynThreadFactory("Snowypine", REPORTER)));
	}

	public void shutdown() {
		if (service.isEmpty()) {
			return;
		}
		ExecutorService executor = service.get();
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
		}
		if (!executor.isTerminated()) {
			executor.shutdownNow();
		}
		service.replace(null);
	}

	public Future<?> executeAsync(Runnable runnable) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return service.get().submit(runnable);
	}

	public <E> Future<E> executeAsync(Runnable runnable, E value) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return service.get().submit(runnable, value);
	}

	public <E> Future<E> executeAsync(Supplier<E> supplier) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return service.get().submit(() -> supplier.get());
	}

	public <E> Future<E> executeAsync(Callable<E> callable) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return service.get().submit(callable);
	}

	public Future<?> executeSync(Runnable runnable) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return executeSync(runnable, null);
	}

	public <E> Future<E> executeSync(Runnable runnable, E value) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return executeBukkit(() -> {
			runnable.run();
			return value;
		});
	}

	public <E> Future<E> executeSync(Supplier<E> supplier) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return executeBukkit(() -> supplier.get());
	}

	public <E> Future<E> executeSync(Callable<E> callable) {
		if (service.isEmpty()) {
			return DoneFuture.instance();
		}
		return executeBukkit(callable);
	}

	private <E> Future<E> executeBukkit(Callable<E> callable) {
		BukkitCallable<E> task = new BukkitCallable<>(callable);
		Bukkit.getScheduler().runTask(Snowypine.getPlugin(), task);
		return task;
	}

	/*
	 * Static
	 */

	public static Future<?> runAsync(Runnable runnable) {
		return TASK.executeAsync(runnable);
	}

	public static <E> Future<E> runAsync(Runnable runnable, E value) {
		return TASK.executeAsync(runnable, value);
	}

	public static <E> Future<E> runAsync(Supplier<E> supplier) {
		return TASK.executeAsync(supplier);
	}

	public static <E> Future<E> runAsync(Callable<E> callable) {
		return TASK.executeAsync(callable);
	}

	public static Future<?> runSync(Runnable runnable) {
		return TASK.executeSync(runnable);
	}

	public static <E> Future<E> runSync(Runnable runnable, E value) {
		return TASK.executeSync(runnable, value);
	}

	public static <E> Future<E> runSync(Supplier<E> supplier) {
		return TASK.executeSync(supplier);
	}

	public static <E> Future<E> runSync(Callable<E> callable) {
		return TASK.executeSync(callable);
	}

}
