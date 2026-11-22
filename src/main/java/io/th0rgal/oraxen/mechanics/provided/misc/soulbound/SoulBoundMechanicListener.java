package io.th0rgal.oraxen.mechanics.provided.misc.soulbound;

import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SoulBoundMechanicListener implements Listener {
    private final SoulBoundMechanicFactory factory;

    public SoulBoundMechanicListener(SoulBoundMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        List<ItemStack> items = new ArrayList<>();
        for (Iterator<ItemStack> iterator = event.getDrops().iterator(); iterator.hasNext();) {
            ItemStack drop = iterator.next();
            String itemID = OraxenItems.getIdByItem(drop);
            if (itemID == null || factory.isNotImplementedIn(itemID))
                continue;

            SoulBoundMechanic mechanic = (SoulBoundMechanic) factory.getMechanic(itemID);
            // lose_chance is a percentage, so keep the drop with probability (100 - loseChance)%.
            if (ThreadLocalRandom.current().nextDouble(100) >= mechanic.getLoseChance()) {
                items.add(drop);
                iterator.remove();
            }
        }
        if (!items.isEmpty()) {
            event.getItemsToKeep().addAll(items);
        }
    }
}
