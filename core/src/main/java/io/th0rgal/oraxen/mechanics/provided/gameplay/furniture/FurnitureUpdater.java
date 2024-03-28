package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;

public class FurnitureUpdater implements Listener {

    public FurnitureUpdater() {
        if (!Settings.UPDATE_FURNITURE.toBool()) return;
        if (Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) {
            Bukkit.getPluginManager().registerEvent(EntitiesLoadEvent.class, this, EventPriority.NORMAL, (listener, event) ->
                    ((EntitiesLoadEvent) event).getEntities().stream().filter(OraxenFurniture::isFurniture).forEach(OraxenFurniture::updateFurniture)
                    , OraxenPlugin.get());
        }
    }
}
