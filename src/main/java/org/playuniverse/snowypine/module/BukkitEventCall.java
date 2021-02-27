package org.playuniverse.snowypine.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.bukkit.event.Event;

import com.syntaxphoenix.syntaxapi.event.Cancelable;
import com.syntaxphoenix.syntaxapi.event.EventPriority;
import com.syntaxphoenix.syntaxapi.utils.general.Status;
import com.syntaxphoenix.syntaxapi.utils.java.Exceptions;

public final class BukkitEventCall {

	private final List<BukkitEventExecutor> executors;
	private final BukkitEventManager manager;
	private final Event event;

	public BukkitEventCall(BukkitEventManager manager, Event event, List<BukkitEventExecutor> executors) {
		this.executors = executors;
		this.manager = manager;
		this.event = event;

		Collections.sort(executors);
	}

	public final BukkitEventManager getManager() {
		return manager;
	}

	public final Event getEvent() {
		return event;
	}

	public final List<BukkitEventExecutor> getExecutors() {
		return executors;
	}

	public Status execute() {
		int count = 0;
		if (executors.isEmpty()) {
			return Status.EMPTY;
		}
		LinkedHashMap<EventPriority, ArrayList<BukkitEventMethod>> listeners = new LinkedHashMap<>();
		for (EventPriority priority : EventPriority.values()) {
			ArrayList<BukkitEventMethod> methods = new ArrayList<>();
			for (BukkitEventExecutor executor : executors) {
				methods.addAll(executor.getMethodsByPriority(priority));
			}
			count += methods.size();
			listeners.put(priority, methods);
		}
		Status result = new Status(count);
		return event instanceof Cancelable ? callCancelable(result, listeners) : call(result, listeners);
	}

	public Status executeAsync(ExecutorService service) {
		int count = 0;
		if (executors.isEmpty()) {
			return Status.EMPTY;
		}
		LinkedHashMap<EventPriority, ArrayList<BukkitEventMethod>> listeners = new LinkedHashMap<>();
		for (EventPriority priority : EventPriority.values()) {
			ArrayList<BukkitEventMethod> methods = new ArrayList<>();
			for (BukkitEventExecutor executor : executors) {
				methods.addAll(executor.getMethodsByPriority(priority));
			}
			count += methods.size();
			listeners.put(priority, methods);
		}

		Status result = new Status(count);
		service.submit(() -> {
			if (event instanceof Cancelable) {
				callCancelable(result, listeners);
			} else {
				call(result, listeners);
			}
		});
		return result;
	}

	private Status callCancelable(Status result, LinkedHashMap<EventPriority, ArrayList<BukkitEventMethod>> listeners) {
		Cancelable cancel = (Cancelable) event;
		for (EventPriority priority : EventPriority.ORDERED_VALUES) {
			ArrayList<BukkitEventMethod> methods = listeners.get(priority);
			if (methods.isEmpty()) {
				continue;
			}
			for (BukkitEventMethod method : methods) {
				if (cancel.isCancelled() && !method.ignoresCancel()) {
					result.cancel();
					continue;
				}
				try {
					method.execute(event);
					result.success();
				} catch (Throwable throwable) {
					result.failed();
					if (manager.hasLogger()) {
						manager.getLogger().log(throwable);
					} else {
						System.out.println(Exceptions.stackTraceToString(throwable));
					}
				}
			}
		}
		result.done();
		return result;
	}

	private Status call(Status result, LinkedHashMap<EventPriority, ArrayList<BukkitEventMethod>> listeners) {
		for (EventPriority priority : EventPriority.ORDERED_VALUES) {
			ArrayList<BukkitEventMethod> methods = listeners.get(priority);
			if (methods.isEmpty()) {
				continue;
			}
			for (BukkitEventMethod method : methods) {
				try {
					method.execute(event);
					result.success();
				} catch (Throwable throwable) {
					result.failed();
					if (manager.hasLogger()) {
						manager.getLogger().log(throwable);
					} else {
						System.out.println(Exceptions.stackTraceToString(throwable));
					}
				}
			}
		}
		result.done();
		return result;
	}

}
