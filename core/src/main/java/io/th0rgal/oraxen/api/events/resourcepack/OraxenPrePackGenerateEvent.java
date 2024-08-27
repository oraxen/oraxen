package io.th0rgal.oraxen.api.events.resourcepack;

import io.th0rgal.oraxen.api.events.OraxenPack;
import io.th0rgal.oraxen.pack.creative.OraxenPackReader;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.ResourcePack;

import java.io.File;

public class OraxenPrePackGenerateEvent extends Event {
    private final ResourcePack resourcePack;
    private static final HandlerList HANDLERS = new HandlerList();

    public OraxenPrePackGenerateEvent(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    public ResourcePack resourcePack() {
        return this.resourcePack;
    }

    public boolean addResourcePack(ResourcePack resourcePack) {
        try {
            OraxenPack.mergePack(this.resourcePack, resourcePack);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addResourcePack(File resourcePack) {
        try {
            OraxenPack.mergePack(this.resourcePack, new OraxenPackReader().readFile(resourcePack));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @NotNull
    @Override
    public HandlerList getHandlers() { return getHandlerList(); }
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
