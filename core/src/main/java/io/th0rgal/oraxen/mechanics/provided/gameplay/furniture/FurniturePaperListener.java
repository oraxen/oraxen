package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class FurniturePaperListener implements Listener {

    @EventHandler
    public void onFurnitureRemoval(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(entity);
        if (mechanic == null) return;

        Entity baseEntity = mechanic.getBaseEntity(entity);
        List<Block> barriers = mechanic.getBarriers().stream().map(b -> entity.getLocation().add(b.toLocation(entity.getWorld())).getBlock()).toList();
        // If the baseEntity does not exist, it means furniture is broken
        // and interaction entity was left behind, or furniture is outdated
        SchedulerUtil.runAtLocationLater(entity.getLocation(), 1L, () -> {
            if (baseEntity == null) {
                if (!entity.isDead()) entity.remove();
                barriers.forEach(b -> {
                    if (b.getType() == Material.BARRIER) {
                        b.setType(Material.AIR);
                        new CustomBlockData(b, OraxenPlugin.get()).clear();
                    }
                });
            }
        });
    }
}
