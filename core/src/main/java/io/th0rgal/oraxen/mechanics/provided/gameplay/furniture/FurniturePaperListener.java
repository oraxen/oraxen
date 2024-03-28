package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FurniturePaperListener implements Listener {

    @EventHandler
    public void onFurnitureRemoval(EntityRemoveFromWorldEvent event) {
        Entity baseEntity = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        mechanic.removeBaseEntity(baseEntity);
    }
}
