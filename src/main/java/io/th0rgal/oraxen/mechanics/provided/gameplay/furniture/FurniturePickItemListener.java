package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import io.papermc.paper.event.player.PlayerPickEntityEvent;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.inventories.PickItemUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FurniturePickItemListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPickBlock(PlayerPickBlockEvent event) {
        handlePick(event, OraxenFurniture.getFurnitureMechanic(event.getBlock()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickEntity(PlayerPickEntityEvent event) {
        handlePick(event, OraxenFurniture.getFurnitureMechanic(event.getEntity()));
    }

    private void handlePick(PlayerPickItemEvent event, FurnitureMechanic mechanic) {
        if (mechanic == null) return;

        event.setCancelled(true);
        ItemBuilder itemBuilder = OraxenItems.getItemById(mechanic.getItemID());
        if (itemBuilder == null) return;

        PickItemUtils.pickItem(event.getPlayer(), itemBuilder.build());
    }
}
