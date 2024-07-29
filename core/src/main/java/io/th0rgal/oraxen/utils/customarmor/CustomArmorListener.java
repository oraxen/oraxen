package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.armorequipevent.ArmorEquipEvent;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;

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

    @EventHandler
    public void onTrimCustomArmor(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack armorPiece = inventory.getInputEquipment();
        if (!Settings.CUSTOM_ARMOR_ENABLED.toBool()) return;
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
        if (!Settings.CUSTOM_ARMOR_ENABLED.toBool()) return;
        if (itemStack == null || !(itemStack.getItemMeta() instanceof ArmorMeta armorMeta)) return;
        if (!itemStack.getType().name().startsWith("CHAINMAIL")) return;
        if (armorMeta.hasTrim() && armorMeta.getTrim().getPattern().key().namespace().equals("oraxen")) return;

        Key vanillaPatternKey = Key.key("minecraft", "chainmail");
        @Nullable TrimPattern vanillaPattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString(vanillaPatternKey.asString()));
        if (vanillaPattern != null && (!armorMeta.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM) || !armorMeta.hasTrim() || !armorMeta.getTrim().getPattern().key().equals(vanillaPatternKey))) {
            armorMeta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, vanillaPattern));
            armorMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
            itemStack.setItemMeta(armorMeta);
        } else if (vanillaPattern == null && Settings.DEBUG.toBool()) Logs.logWarning("Vanilla trim-pattern not found for " + itemStack.getType().name() + ": " + vanillaPatternKey.asString());
    }
}
