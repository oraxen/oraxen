package org.playuniverse.snowypine.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public abstract class SnowypineCancellableEvent extends SnowypineEvent implements Cancellable {

	public SnowypineCancellableEvent() {
		this(false);
	}

	public SnowypineCancellableEvent(boolean async) {
		super(async);
	}

	/*
	 * Cancel
	 */

	protected boolean cancelled = false;

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
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
