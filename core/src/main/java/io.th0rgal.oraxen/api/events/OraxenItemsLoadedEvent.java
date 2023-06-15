package io.th0rgal.oraxen.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OraxenItemsLoadedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    @Override
    public HandlerList getHandlers() { return getHandlerList(); }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
