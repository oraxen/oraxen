package io.th0rgal.oraxen.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when all native mechanics are registered.
 * Useful to register your own mechanics, and re-register on reloads.
 */
public class OraxenNativeMechanicsRegisteredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
