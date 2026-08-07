package io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class EquipListener extends CustomListener {

    public EquipListener(String itemID, long cooldown, CustomEvent event, ClickAction clickAction) {
        super(itemID, cooldown, event, clickAction);
    }

    @EventHandler
    public void onEquipArmor(final ArmorEquipEvent event) {
        ItemStack newArmor = event.getNewArmorPiece();
        if (newArmor == null || !itemID.equals(OraxenItems.getIdByItem(newArmor))) return;
        perform(event.getPlayer(), newArmor);
    }
}
