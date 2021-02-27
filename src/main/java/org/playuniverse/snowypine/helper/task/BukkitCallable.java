package org.playuniverse.snowypine.helper.task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class BukkitCallable<E> implements Runnable, Future<E> {

	private final Callable<E> task;

	private E value;

	private boolean running = false;

	public BukkitCallable(Callable<E> task) {
		this.task = task;
	}

	@Override
	public void run() {
		running = true;
		try {
			value = task.call();
		} catch (Throwable throwable) {
			TaskHelper.REPORTER.catchFail(throwable, null, Thread.currentThread(), null);
		}
		running = false;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return !running;
	}

	@Override
	public E get() throws InterruptedException, ExecutionException {
		while (running) {
			Thread.sleep(20);
		}
		return value;
	}

	@Override
	public E get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long millis = unit.toMillis(timeout);
		while (running) {
			if (millis-- == 0) {
				throw new TimeoutException("Task wasn't done in time!");
			}
			Thread.sleep(1);
		}
		return value;
	}

}