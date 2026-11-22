package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;

import static io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic.BARRIER_KEY;

public class FurniturePaperListener implements Listener {

    @EventHandler
    public void onFurnitureRemoval(EntityRemoveEvent event) {
        if (event.getCause() == EntityRemoveEvent.Cause.UNLOAD) return;
        Entity baseEntity = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        // Seats also carry the furniture key, but only base entities store their barriers
        if (mechanic == null || !OraxenFurniture.isBaseEntity(baseEntity)
                || !baseEntity.getPersistentDataContainer().has(BARRIER_KEY, DataType.asList(BlockLocation.dataType))
                || FurnitureMechanic.isRemovingBaseEntity(baseEntity)) return;

        // Something else removed the base entity, for example /kill, so clear the barriers, hitboxes and seats it left.
        // The entity only counts as removed after this event, so wait a tick.
        SchedulerUtil.runAtLocationLater(baseEntity.getLocation(), 1L, () -> {
            if (mechanic.hasBarriers(baseEntity))
                mechanic.removeSolid(baseEntity, baseEntity.getLocation(), FurnitureMechanic.getFurnitureYaw(baseEntity));
            else mechanic.removeNonSolidFurniture(baseEntity);
        });
    }
}
