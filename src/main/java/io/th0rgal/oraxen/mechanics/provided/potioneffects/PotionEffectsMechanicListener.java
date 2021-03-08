package io.th0rgal.oraxen.mechanics.provided.potioneffects;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
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

    @SuppressWarnings("unused")
    @EventHandler
    public void onEntityResurrectEvent(EntityResurrectEvent resurrectEvent) {
        if (true)
            return; // Todo need fix strange error
        if (resurrectEvent.getEntity().getType().equals(EntityType.PLAYER)) {
            Player player = (Player) resurrectEvent.getEntity();
            // Stuff du joueur
            ItemStack boots = player.getInventory().getBoots();
            ItemStack chestplate = player.getInventory().getChestplate();
            ItemStack helmet = player.getInventory().getHelmet();
            ItemStack leggings = player.getInventory().getLeggings();
            if (boots != null) {
                String itemID = OraxenItems.getIdByItem(boots);
                PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
                mechanic.onTotemofUndying(player);
            }

            if (chestplate != null) {
                String itemID = OraxenItems.getIdByItem(chestplate);
                PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
                mechanic.onTotemofUndying(player);
            }

            if (helmet != null) {
                String itemID = OraxenItems.getIdByItem(helmet);
                PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
                mechanic.onTotemofUndying(player);
            }

            if (leggings != null) {
                String itemID = OraxenItems.getIdByItem(leggings);
                PotionEffectsMechanic mechanic = (PotionEffectsMechanic) factory.getMechanic(itemID);
                mechanic.onTotemofUndying(player);
            }

        }
    }

}
