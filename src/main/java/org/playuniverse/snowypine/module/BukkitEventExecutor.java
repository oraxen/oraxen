package org.playuniverse.snowypine.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.event.Event;

import com.syntaxphoenix.syntaxapi.event.EventListener;
import com.syntaxphoenix.syntaxapi.event.EventPriority;

public final class BukkitEventExecutor implements Comparable<BukkitEventExecutor> {

	private final EnumMap<EventPriority, ArrayList<BukkitEventMethod>> methods = new EnumMap<>(EventPriority.class);
	private final Class<? extends Event> event;
	private final EventListener listener;
	private final BukkitEventManager manager;

	public BukkitEventExecutor(BukkitEventManager manager, EventListener listener, Class<? extends Event> event) {
		this.listener = listener;
		this.manager = manager;
		this.event = event;
	}

	/*
	 * 
	 */

	public final BukkitEventManager getManager() {
		return manager;
	}

	public final EventListener getListener() {
		return listener;
	}

	public final Class<? extends Event> getEvent() {
		return event;
	}

	/*
	 * 
	 */

	protected BukkitEventExecutor add(EventPriority priority, BukkitEventMethod method) {
		ArrayList<BukkitEventMethod> list = methods.get(priority);
		if (list == null) {
			methods.put(priority, list = new ArrayList<>());
		} else if (list.contains(method)) {
			return this;
		}
		list.add(method);
		return this;
	}

	public List<BukkitEventMethod> getMethodsByPriority(EventPriority priority) {
		if (!methods.containsKey(priority)) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(methods.get(priority));
	}

	public List<BukkitEventMethod> getMethods() {
		ArrayList<BukkitEventMethod> output = new ArrayList<>();
		methods.forEach((priority, list) -> output.addAll(list));
		return output;
	}

	/*
	 * 
	 */

	@Override
	public int compareTo(BukkitEventExecutor o) {
		Class<? extends Event> other = o.getEvent();
		if (event.equals(other)) {
			return 0;
		}
		if (event.isAssignableFrom(other)) {
			return -1;
		}
		return 1;
	}

}
