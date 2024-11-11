package io.th0rgal.oraxen.mechanics.provided.cosmetic.hat;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.utils.EventUtils;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;

@Deprecated(since = "1.21.2")
public class HatMechanicListener implements Listener {

    private final MechanicFactory factory;

    public HatMechanicListener(final MechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onClickArmorStand(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) return;
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment.getHelmet() == null) return;
        if (!EventUtils.callEvent(new PlayerArmorStandManipulateEvent(player, armorStand, item, equipment.getHelmet(), EquipmentSlot.HEAD, EquipmentSlot.HAND))) return;

        if (item.getType() == Material.AIR) {
            if (event.getClickedPosition().getY() < 1.55) return; // Did not click head
            if (!OraxenItems.exists(equipment.getHelmet())) return;
            if (!OraxenItems.hasMechanic(OraxenItems.getIdByItem(equipment.getHelmet()), "hat")) return;
            if (player.getInventory().firstEmpty() == -1) return;

            player.getInventory().addItem(equipment.getHelmet());
            equipment.setHelmet(null);
        } else {
            String itemID = OraxenItems.getIdByItem(item);
            if (equipment.getHelmet().getType() != Material.AIR) return;
            if (!OraxenItems.hasMechanic(itemID, "hat")) return;

            ItemStack helm = item.clone();
            helm.setAmount(1);
            equipment.setHelmet(helm);
            item.setAmount(item.getAmount() - 1);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryHatPut(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final ItemStack item = event.getItem();
        final String itemID = OraxenItems.getIdByItem(item);
        final Player player = event.getPlayer();
        final PlayerInventory inventory = player.getInventory();

        if (item == null || factory.isNotImplementedIn(itemID)) return;
        if (inventory.getHelmet() != null) return;

        event.setCancelled(true);
        if (!EventUtils.callEvent(ArmorEquipEvent.OraxenHatEquipEvent(player, null, item))) return;

        final ItemStack helmet = item.clone();
        helmet.setAmount(1);
        inventory.setHelmet(helmet);
        if (player.getGameMode() != GameMode.CREATIVE)
            item.setAmount(item.getAmount() - 1);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlaceHatNotOnHelmetSlot(final ArmorEquipEvent event) {
        final ItemStack newArmorPiece = event.getNewArmorPiece();
        if (newArmorPiece == null) return;
        final String itemID = OraxenItems.getIdByItem(newArmorPiece);
        if (factory.isNotImplementedIn(itemID) || event.getMethod() != ArmorEquipEvent.EquipMethod.SHIFT_CLICK) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlaceHatOnHelmetSlot(final InventoryClickEvent e) {
        final Inventory clickedInventory = e.getClickedInventory();
        final ItemStack cursor = e.getCursor();
        final Player player = (Player) e.getWhoClicked();

        if (clickedInventory == null || !clickedInventory.getType().equals(InventoryType.PLAYER)) return;
        if (e.getSlotType() != InventoryType.SlotType.ARMOR || cursor == null) return;

        final ItemStack clone = cursor.clone();
        String itemID = OraxenItems.getIdByItem(clone);
        final ItemStack currentItem = e.getCurrentItem();

        if (factory.isNotImplementedIn(itemID)) {
            if (cursor.getType() == Material.AIR) {
                itemID = OraxenItems.getIdByItem(currentItem);
                if (factory.isNotImplementedIn(itemID)) return;

                if (!EventUtils.callEvent(ArmorEquipEvent.OraxenHatEquipEvent(player, currentItem, cursor)))
                    e.setCancelled(true);
            }
        } else {

            if (e.getSlot() != 39) {
                e.setCancelled(true);
                return;
            }

            if (currentItem == null || currentItem.getType() == Material.AIR) {
                final ArmorEquipEvent armorEquipEvent = ArmorEquipEvent.OraxenHatEquipEvent(player, currentItem, clone);
                if (!EventUtils.callEvent(armorEquipEvent)) return;

                e.setCancelled(true);
                player.getInventory().setHelmet(armorEquipEvent.getNewArmorPiece());
                cursor.setAmount(0);
            }
        }
    }

}
