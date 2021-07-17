package io.th0rgal.oraxen.mechanics.provided.invisibleitemframe;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import static io.th0rgal.oraxen.mechanics.provided.invisibleitemframe.InvisibleItemFrameMechanic.invisibleKey;

public class InvisibleItemFrameListener implements Listener {

        private final MechanicFactory factory;


        public InvisibleItemFrameListener(MechanicFactory factory) {
            this.factory = factory;
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void onHangingPlaceEvent(HangingPlaceEvent event) {
            if (event.getEntity().getType() != EntityType.ITEM_FRAME || event.getPlayer() == null) return;
            ItemStack frame;
            Player player = event.getPlayer();
            if (player.getInventory().getItemInMainHand().getType() == Material.ITEM_FRAME) {
                frame = player.getInventory().getItemInMainHand();
            } else if (player.getInventory().getItemInOffHand().getType() == Material.ITEM_FRAME) {
                frame = player.getInventory().getItemInOffHand();
            } else {
                return;
            }

            String itemID = OraxenItems.getIdByItem(frame);
            if (!factory.isNotImplementedIn(itemID))  event.getEntity().getPersistentDataContainer()
                    .set(invisibleKey, PersistentDataType.BYTE, (byte) 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerInteractWithItemFrame(PlayerInteractEntityEvent event) {
            if (event.getRightClicked().getType() == EntityType.ITEM_FRAME &&
                event.getRightClicked().getPersistentDataContainer().has(invisibleKey, PersistentDataType.BYTE)) {
                ItemFrame itemFrame = (ItemFrame) event.getRightClicked();
                Bukkit.getScheduler().runTaskLater(OraxenPlugin.get(), () -> {
                    if (itemFrame.getItem().getType() != Material.AIR) {
                        itemFrame.setVisible(false); // Need 1.16 
                    }
                }, 1L);
            }
        }

}
