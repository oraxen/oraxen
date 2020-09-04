package io.th0rgal.oraxen.event.config;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.th0rgal.oraxen.settings.ConfigUpdater;

public class OraxenConfigEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    public boolean registerUpdates(Object object) {
        return ConfigUpdater.register(object);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
