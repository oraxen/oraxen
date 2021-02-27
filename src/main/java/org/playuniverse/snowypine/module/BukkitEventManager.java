package org.playuniverse.snowypine.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.bukkit.event.Event;

import com.syntaxphoenix.syntaxapi.event.EventListener;
import com.syntaxphoenix.syntaxapi.event.EventPriority;
import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.utils.general.Status;
import com.syntaxphoenix.syntaxapi.utils.java.Collect;

/**
 * 
 * @author Lauriichen
 *
 */

public class BukkitEventManager {

	private final LinkedHashMap<Class<? extends Event>, ArrayList<BukkitEventExecutor>> listeners = new LinkedHashMap<>();
	private final ILogger logger;

	private final ExecutorService service;
	private final BukkitEventHook hook;

	public BukkitEventManager() {
		this(null, null);
	}

	public BukkitEventManager(ExecutorService service) {
		this(null, service);
	}

	public BukkitEventManager(ILogger logger) {
		this(logger, null);
	}

	public BukkitEventManager(ILogger logger, ExecutorService service) {
		this.logger = logger;
		this.service = service;
		this.hook = new BukkitEventHook(this);
	}

	/*
	 * Getter / Infos
	 */

	public boolean hasLogger() {
		return logger != null;
	}

	public ILogger getLogger() {
		return logger;
	}

	public boolean isAsync() {
		return service != null;
	}

	public ExecutorService getExecutorService() {
		return service;
	}

	public BukkitEventHook getHook() {
		return hook;
	}

	/*
	 * Event calls
	 */

	public BukkitEventCall generateCall(Event event) {
		return new BukkitEventCall(this, event, getExecutorsForEvent(event.getClass(), true));
	}

	public Status call(Event event) {
		return call(generateCall(event));
	}

	public Status call(BukkitEventCall call) {
		if (isAsync()) {
			return call.executeAsync(service);
		} else {
			return call.execute();
		}
	}

	public Status callAsync(Event event, ExecutorService service) {
		return callAsync(generateCall(event), service);
	}

	public Status callAsync(BukkitEventCall call, ExecutorService service) {
		return call.executeAsync(service);
	}

	/*
	 * Registration
	 */

	// Register

	public BukkitEventManager registerEvents(EventListener listener) {
		BukkitEventAnalyser analyser = new BukkitEventAnalyser(listener);
		analyser.registerEvents(this);
		return this;
	}

	public BukkitEventManager registerEvent(BukkitEventMethod method) {
		if (!method.isValid()) {
			return this;
		}
		BukkitEventExecutor executor = new BukkitEventExecutor(this, method.getListener(), method.getEvent());
		executor.add(method.hasEventHandler() ? method.getHandler().priority() : EventPriority.NORMAL, method);
		registerExecutor(executor);
		return this;
	}

	public BukkitEventManager registerExecutors(Collection<BukkitEventExecutor> executors) {
		executors.forEach(executor -> registerExecutor(executor));
		return this;
	}

	public BukkitEventManager registerExecutor(BukkitEventExecutor executor) {
		if (executor == null || executor.getMethods().isEmpty()) {
			return this;
		}
		Class<? extends Event> event = executor.getEvent();
		if (listeners.containsKey(event)) {
			ArrayList<BukkitEventExecutor> executors = listeners.get(event);
			if (!executors.contains(executor)) {
				executors.add(executor);
			}
		} else if (!listeners.containsKey(event)) {
			ArrayList<BukkitEventExecutor> executors = new ArrayList<>();
			executors.add(executor);
			listeners.put(event, executors);
			hook.registerEvent(event);
		}
		return this;
	}

	// Unregister

	public BukkitEventManager unregisterEvent(Class<? extends Event> event) {
		listeners.remove(event);
		return this;
	}

	public BukkitEventManager unregisterEvents(Class<? extends EventListener> listener) {
		return unregisterExecutors(getExecutorsFromOwner(listener));
	}

	public BukkitEventManager unregisterEvents(EventListener listener) {
		return unregisterExecutors(getExecutorsFromOwner(listener));
	}

	public BukkitEventManager unregisterExecutors(Iterable<BukkitEventExecutor> executors) {
		return unregisterExecutors(executors.iterator());
	}

	public BukkitEventManager unregisterExecutors(Iterator<BukkitEventExecutor> executors) {
		while (executors.hasNext()) {
			unregisterExecutor(executors.next());
		}
		return this;
	}

	public BukkitEventManager unregisterExecutors(BukkitEventExecutor... executors) {
		for (BukkitEventExecutor executor : executors) {
			unregisterExecutor(executor);
		}
		return this;
	}

	public BukkitEventManager unregisterExecutor(BukkitEventExecutor executor) {
		ArrayList<BukkitEventExecutor> list = listeners.get(executor.getEvent());
		if (list != null && list.contains(executor)) {
			list.remove(executor);
		}
		return this;
	}

	/*
	 * Owners
	 */

	public List<? extends EventListener> getOwners() {
		return getExecutors().stream().collect(Collect.collectList((output, input) -> {
			if (!output.contains(input.getListener())) {
				output.add(input.getListener());
			}
		}));
	}

	public List<Class<? extends EventListener>> getOwnerClasses() {
		return getOwners().stream().collect(Collect.collectList((output, input) -> output.add(input.getClass())));
	}

	/*
	 * Events
	 */

	public List<Class<? extends Event>> getEvents() {
		return listeners.keySet().stream().collect(Collectors.toList());
	}

	/*
	 * Executors
	 */

	public List<BukkitEventExecutor> getExecutors() {
		return listeners.values().stream().collect(Collect.combineList());
	}

	public List<BukkitEventExecutor> getExecutorsFromOwner(EventListener listener) {
		return getExecutors().stream().filter(executor -> executor.getListener() == listener).collect(Collectors.toList());
	}

	public List<BukkitEventExecutor> getExecutorsFromOwner(Class<? extends EventListener> listener) {
		return getExecutors().stream().filter(executor -> executor.getListener().getClass() == listener).collect(Collectors.toList());
	}

	public List<BukkitEventExecutor> getExecutorsForEvent(Class<? extends Event> event) {
		return getExecutorsForEvent(event, false);
	}

	@SuppressWarnings("unchecked")
	public List<BukkitEventExecutor> getExecutorsForEvent(Class<? extends Event> event, boolean allowAssignableClasses) {
		if (!allowAssignableClasses) {
			if (!listeners.containsKey(event)) {
				return new ArrayList<>();
			}
			return (List<BukkitEventExecutor>) listeners.get(event).clone();
		}
		ArrayList<BukkitEventExecutor> executors = new ArrayList<>();
		Set<Class<? extends Event>> keys = listeners.keySet();
		for (Class<? extends Event> assign : keys) {
			if (assign.isAssignableFrom(event)) {
				executors.addAll(listeners.get(assign));
			}
		}
		return (List<BukkitEventExecutor>) executors.clone();
	}

}
