package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.InventoryUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class CustomArmorListener implements Listener {

    public CustomArmorListener() {
        if (!VersionUtil.isPaperServer()) return;
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerPickup(PlayerAttemptPickupItemEvent event) {
                ItemStack item = event.getItem().getItemStack();
                setVanillaArmorTrim(item);
                event.getItem().setItemStack(item);
            }
        }, OraxenPlugin.get());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCustomArmorRepair(PrepareAnvilEvent event) {
        if (!Settings.DISABLE_LEATHER_REPAIR_CUSTOM.toBool()) return;
        AnvilInventory inventory = event.getInventory();
        Player player = InventoryUtils.playerFromView(event);
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

    @EventHandler
    public void updateVanillaArmor(PlayerJoinEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents())
            setVanillaArmorTrim(item);
    }

    @EventHandler
    public void onPlayerEquipVanilla(ArmorEquipEvent event) {
        setVanillaArmorTrim(event.getNewArmorPiece());
        setVanillaArmorTrim(event.getOldArmorPiece());
    }

    @EventHandler
    public void onAmorStandEquip(PlayerArmorStandManipulateEvent event) {
        setVanillaArmorTrim(event.getPlayerItem());
        setVanillaArmorTrim(event.getArmorStandItem());
    }

    private void setVanillaArmorTrim(ItemStack itemStack) {
        String armorPrefix = Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString();
        if (!VersionUtil.atOrAbove("1.20")) return;
        if (CustomArmorType.getSetting() != CustomArmorType.TRIMS) return;
        if (itemStack == null || !(itemStack.getItemMeta() instanceof ArmorMeta armorMeta)) return;
        if (!itemStack.getType().name().startsWith(armorPrefix)) return;
        if (armorMeta.hasTrim() && armorMeta.getTrim().getPattern().key().namespace().equals("oraxen")) return;

        Key vanillaPatternKey = Key.key("minecraft", armorPrefix.toLowerCase(Locale.ROOT));
        @Nullable TrimPattern vanillaPattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString(vanillaPatternKey.asString()));
        if (vanillaPattern != null && (!armorMeta.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM) || !armorMeta.hasTrim() || !armorMeta.getTrim().getPattern().key().equals(vanillaPatternKey))) {
            armorMeta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, vanillaPattern));
            armorMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
            itemStack.setItemMeta(armorMeta);
        } else if (vanillaPattern == null && Settings.DEBUG.toBool()) Logs.logWarning("Vanilla trim-pattern not found for " + itemStack.getType().name() + ": " + vanillaPatternKey.asString());
    }
}
