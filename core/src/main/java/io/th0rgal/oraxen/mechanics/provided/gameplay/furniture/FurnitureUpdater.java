package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FurnitureUpdater implements Listener {

    public static void registerListener() {
        if (!VersionUtil.isPaperServer()) return;
        MechanicsManager.registerListeners(OraxenPlugin.get(), FurnitureFactory.instance.getMechanicID(), new FurnitureUpdater());
    }

    @EventHandler
    public void onLoad(EntityAddToWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> OraxenFurniture.updateFurniture(event.getEntity()), 2L);
    }
}
