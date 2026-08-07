package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OraxenPackGeneratedEvent extends Event {

    private final List<VirtualFile> output;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenPackGeneratedEvent(List<VirtualFile> output) {
        this.output = output;
    }

    public List<VirtualFile> getOutput() {
        return output;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
