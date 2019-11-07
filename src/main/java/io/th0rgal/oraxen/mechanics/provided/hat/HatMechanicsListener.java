package io.th0rgal.oraxen.mechanics.provided.hat;

import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;

import io.th0rgal.oraxen.utils.Logs;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HatMechanicsListener implements Listener {

    private MechanicFactory factory;

    public HatMechanicsListener(HatMechanicFactory factory) {
        this.factory = factory;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    private void onInventoryHatPut(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        ItemStack item = event.getItem();
        String itemID = OraxenItems.getIdByItem(item);
        if (factory.isNotImplementedIn(itemID))
            return;

        if (event.getPlayer().getInventory().getHelmet() == null) {
            event.getPlayer().getInventory().setHelmet(item);
            item.setAmount(0);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void OnPlaceHatOnHelmetSlot(InventoryClickEvent e) {
        if (e.getClickedInventory() == null
                || !e.getClickedInventory().getType().equals(InventoryType.PLAYER)
                || e.getSlotType() != InventoryType.SlotType.ARMOR
                || e.getSlot() != 39
                || e.getCursor() == null)
            return;

        ItemStack clone = e.getCursor().clone();

        String itemID = OraxenItems.getIdByItem(clone);
        if (factory.isNotImplementedIn(itemID))
            return;

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
            e.setCancelled(true);
            e.getWhoClicked().getInventory().setHelmet(clone);
            e.getCursor().setAmount(0);
        }

    }

}
