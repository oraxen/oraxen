package io.th0rgal.oraxen.mechanics.provided.cosmetic.hat;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class HatMechanicListener implements Listener {

    private final MechanicFactory factory;

    public HatMechanicListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryHatPut(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);

        if (factory.isNotImplementedIn(itemID))
            return;
        final Player player = event.getPlayer();
        final PlayerInventory inventory = player.getInventory();
        if (inventory.getHelmet() == null) {
            event.setCancelled(true);
            final ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent(player, ArmorEquipEvent.EquipMethod.ORAXEN_HAT,
                    ArmorType.HELMET, null, item);
            Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
            if (armorEquipEvent.isCancelled())
                return;
            final ItemStack helmet = item.clone();
            helmet.setAmount(1);
            inventory.setHelmet(helmet);
            if (player.getGameMode() != GameMode.CREATIVE)
                item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void OnPlaceHatNotOnHelmetSlot(final ArmorEquipEvent event) {
        final ItemStack newArmorPiece = event.getNewArmorPiece();
        if (newArmorPiece != null) {
            final String itemID = OraxenItems.getIdByItem(newArmorPiece);
            if (factory.isNotImplementedIn(itemID) || event.getMethod() != ArmorEquipEvent.EquipMethod.SHIFT_CLICK)
                return;
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void OnPlaceHatOnHelmetSlot(final InventoryClickEvent e) {
        final Inventory clickedInventory = e.getClickedInventory();
        final ItemStack cursor = e.getCursor();

        if (clickedInventory == null || !clickedInventory.getType().equals(InventoryType.PLAYER)
                || e.getSlotType() != InventoryType.SlotType.ARMOR || cursor == null)
            return;

        final ItemStack clone = cursor.clone();
        String itemID = OraxenItems.getIdByItem(clone);
        final ItemStack currentItem = e.getCurrentItem();

        if (factory.isNotImplementedIn(itemID)) {
            if (cursor.getType() == Material.AIR) {
                itemID = OraxenItems.getIdByItem(currentItem);
                if (factory.isNotImplementedIn(itemID))
                    return;

                final ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) e.getWhoClicked(),
                        ArmorEquipEvent.EquipMethod.ORAXEN_HAT, ArmorType.HELMET, currentItem, cursor);
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled())
                    e.setCancelled(true);
            }
        } else {

            if (e.getSlot() != 39) {
                e.setCancelled(true);
                return;
            }

            if (currentItem == null || currentItem.getType() == Material.AIR) {
                final ArmorEquipEvent armorEquipEvent = new ArmorEquipEvent((Player) e.getWhoClicked(),
                        ArmorEquipEvent.EquipMethod.ORAXEN_HAT, ArmorType.HELMET, currentItem, clone);
                Bukkit.getServer().getPluginManager().callEvent(armorEquipEvent);
                if (armorEquipEvent.isCancelled())
                    return;

                e.setCancelled(true);
                e.getWhoClicked().getInventory().setHelmet(armorEquipEvent.getNewArmorPiece());
                cursor.setAmount(0);
            }
        }
    }

}
