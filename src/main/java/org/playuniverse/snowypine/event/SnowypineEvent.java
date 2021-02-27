package org.playuniverse.snowypine.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class SnowypineEvent extends Event {

	public SnowypineEvent() {
		this(false);
	}

	public SnowypineEvent(boolean async) {
		super(async);
	}

	/*
	 * Bukkit Stuff
	 */

	public static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
