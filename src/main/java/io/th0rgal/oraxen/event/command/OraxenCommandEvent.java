package io.th0rgal.oraxen.event.command;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.syntaxphoenix.syntaxapi.command.CommandManager;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.CommandProvider;

public class OraxenCommandEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();

    private final CommandManager manager;
    private final CommandProvider provider;

    public OraxenCommandEvent(CommandProvider provider) {
        this.provider = provider;
        this.manager = provider.getManager();
    }

    /*
     * Registration
     */

    public final OraxenCommandEvent add(CommandInfo info) {
        apply(info);
        return this;
    }

    public boolean apply(CommandInfo info) {
        if (!info.register(manager))
            return false;
        provider.add(info);
        return true;
    }

    /*
     * HandlerList
     */

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
