package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;


public class PotionEffectsMechanicListener implements Listener {

    private final PotionEffectsMechanicFactory factory;

    public PotionEffectsMechanicListener(PotionEffectsMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemWorn(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        String itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemPlaced(PotionEffectsMechanic.Position.HELD, event.getPlayer());
            return;
        }
        item = player.getInventory().getItem(event.getPreviousSlot());
        itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemRemoved(PotionEffectsMechanic.Position.HELD, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemWorn(ArmorEquipEvent event) {
        ItemStack item = event.getNewArmorPiece();
        String itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemPlaced(PotionEffectsMechanic.Position.WORN, event.getPlayer());
            return;
        }
        item = event.getOldArmorPiece();
        itemID = OraxenItems.getIdByItem(item);
        if (!factory.isNotImplementedIn(itemID)) {
            PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
            mechanic.onItemRemoved(PotionEffectsMechanic.Position.WORN, event.getPlayer());
        }
    }

}
