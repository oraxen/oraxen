package org.playuniverse.snowypine.helper.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DoneFuture<E> implements Future<E> {

	private static final DoneFuture<?> FUTURE = new DoneFuture<>();

	@SuppressWarnings("unchecked")
	public static <E> DoneFuture<E> instance() {
		return (DoneFuture<E>) FUTURE;
	}

	private DoneFuture() {}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return true;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public E get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public E get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

}
