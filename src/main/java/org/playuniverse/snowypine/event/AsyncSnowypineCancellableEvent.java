package org.playuniverse.snowypine.event;

import org.bukkit.event.HandlerList;

public abstract class AsyncSnowypineCancellableEvent extends SnowypineCancellableEvent {

	public AsyncSnowypineCancellableEvent() {
		super(true);
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
