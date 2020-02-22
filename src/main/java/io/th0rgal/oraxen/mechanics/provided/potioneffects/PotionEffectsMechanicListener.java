package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class PotionEffectsMechanicListener implements Listener {

    private final PotionEffectsMechanicFactory factory;

    public PotionEffectsMechanicListener(PotionEffectsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemWorn(ArmorEquipEvent event) {
        ItemStack item = event.getNewArmorPiece();
        String itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemPlaced(event.getPlayer());
            return;
        }
        item = event.getOldArmorPiece();
        itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemRemoved(event.getPlayer());
        }
    }

}
