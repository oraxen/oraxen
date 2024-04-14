package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FurnitureUpdater implements Listener {

    public static void registerListener() {
        if (!VersionUtil.isPaperServer()) return;
        MechanicsManager.registerListeners(OraxenPlugin.get(), FurnitureFactory.instance.getMechanicID(), new FurnitureUpdater());
    }

    @EventHandler
    public void onLoad(EntityAddToWorldEvent event) {
        if (!Settings.UPDATE_FURNITURE.toBool() || !Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) return;
        OraxenFurniture.updateFurniture(event.getEntity());
    }
}
