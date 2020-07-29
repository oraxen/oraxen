package io.th0rgal.oraxen.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class OraxenEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
