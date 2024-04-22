package io.th0rgal.oraxen.api.events.resourcepack;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;

public class OraxenPostPackGenerateEvent extends Event {

    private final ResourcePack resourcePack;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenPostPackGenerateEvent(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public ResourcePack resourcePack() {
        return this.resourcePack;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() { return getHandlerList(); }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
