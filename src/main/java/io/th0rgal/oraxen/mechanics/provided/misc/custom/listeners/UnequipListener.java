package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class UnequipListener extends CustomListener {

    public UnequipListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
    }

    @EventHandler
    public void onUnEquipArmor(final ArmorEquipEvent event) {
        ItemStack oldArmor = event.getOldArmorPiece();
        if (oldArmor == null || !itemID.equals(OraxenItems.getIdByItem(oldArmor))) return;
        perform(event.getPlayer(), oldArmor);
    }
}
