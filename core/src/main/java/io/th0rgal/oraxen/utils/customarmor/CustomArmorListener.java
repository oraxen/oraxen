package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class CustomArmorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomArmorRepair(PrepareAnvilEvent event) {
        if (!Settings.DISABLE_LEATHER_REPAIR_CUSTOM.toBool()) return;
        AnvilInventory inventory = event.getInventory();
        Player player = (Player) inventory.getViewers().stream().filter(p -> p.getOpenInventory().getTopInventory() == inventory).findFirst().orElse(null);
        if (player == null) return;
        ItemStack first = inventory.getItem(0);
        ItemStack second = inventory.getItem(1);
        String firstID = OraxenItems.getIdByItem(first);
        String secondID = OraxenItems.getIdByItem(second);
        if (first == null || second == null) return; // Empty slot
        if (firstID == null) return; // Not a custom item
        if (!(first.getItemMeta() instanceof LeatherArmorMeta)) return; // Not a custom armor

        if (second.getType() == Material.LEATHER || (!firstID.equals(secondID) && second.getItemMeta() instanceof LeatherArmorMeta)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWashCustomArmor(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (block == null || block.getType() != Material.WATER_CAULDRON) return;
        if (item == null || !(item.getItemMeta() instanceof LeatherArmorMeta)) return;
        if (!OraxenItems.exists(item)) return;

        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onTrimCustomArmor(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack armorPiece = inventory.getInputEquipment();
        if (CustomArmorType.getSetting() != CustomArmorType.TRIMS) return;
        if (armorPiece == null || !armorPiece.hasItemMeta() || !OraxenItems.exists(armorPiece)) return;
        if (!armorPiece.hasItemMeta() || !(armorPiece.getItemMeta() instanceof ArmorMeta armorMeta)) return;
        if (!armorMeta.hasTrim() || !armorMeta.getTrim().getPattern().key().namespace().equals("oraxen")) return;
        event.setResult(null);
    }
}
