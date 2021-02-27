package org.playuniverse.snowypine.helper;

import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.playuniverse.snowypine.event.AsyncSnowypineEvent;
import org.playuniverse.snowypine.event.SnowypineEvent;
import org.playuniverse.snowypine.helper.task.TaskHelper;

public final class EventHelper {

	private EventHelper() {}

	public static <E extends AsyncSnowypineEvent> Future<E> call(E event) {
		return TaskHelper.runAsync(() -> Bukkit.getPluginManager().callEvent(event), event);
	}

	public static <E extends SnowypineEvent> Future<E> call(E event) {
		return TaskHelper.runSync(() -> Bukkit.getPluginManager().callEvent(event), event);
	}

}
