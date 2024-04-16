package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block.stringblock;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class StringBlockMechanicPaperListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnteringTripwire(EntityInsideBlockEvent event) {
        if (event.getBlock().getType() == Material.TRIPWIRE)
            event.setCancelled(true);
    }
}
