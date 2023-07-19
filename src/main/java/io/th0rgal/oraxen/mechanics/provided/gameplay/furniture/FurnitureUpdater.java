package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

public class FurnitureUpdater implements Listener {

    public FurnitureUpdater() {
        if (furnitureUpdateTask != null) furnitureUpdateTask.cancel();
        furnitureUpdateTask = new FurnitureUpdateTask();
        int delay = (Settings.FURNITURE_UPDATE_DELAY.getValue() instanceof Integer integer) ? integer : 5;
        furnitureUpdateTask.runTaskTimer(OraxenPlugin.get(), 0, delay * 20L);
    }

    public static HashSet<Entity> furnitureToUpdate = new HashSet<>();
    public static FurnitureUpdateTask furnitureUpdateTask;
    public static class FurnitureUpdateTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Entity entity : new HashSet<>(furnitureToUpdate)) {
                OraxenFurniture.updateFurniture(entity);
                furnitureToUpdate.remove(entity);
            }
        }
    }

    @EventHandler
    public void onEntityLoad(EntitiesLoadEvent event) {
        if (!Settings.UPDATE_FURNITURE.toBool() || !Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) return;

        for (Entity entity : event.getEntities())
            if (OraxenFurniture.isFurniture(entity))
                furnitureToUpdate.add(entity);
    }
}
