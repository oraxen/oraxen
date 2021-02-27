package org.playuniverse.snowypine.module;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.utils.wait.Awaiter;

public class BukkitEventHook implements Listener, EventExecutor {

	private final BukkitEventManager manager;

	public BukkitEventHook(BukkitEventManager manager) {
		this.manager = manager;
	}

	public void registerEvent(Class<? extends Event> event) {
		Bukkit.getPluginManager().registerEvent(event, this, EventPriority.HIGHEST, this, Snowypine.getPlugin());
	}

	public void unregister() {
		HandlerList.unregisterAll(this);
	}

	@Override
	public void execute(Listener listener, Event event) throws EventException {
		Awaiter.of(manager.call(event)).await();
	}

}
