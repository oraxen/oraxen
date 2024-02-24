package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.items.ItemBuilder.UNSTACKABLE_KEY;

public class ItemUpdater implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;

        PlayerInventory inventory = event.getPlayer().getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack oldItem = inventory.getItem(i);
            ItemStack newItem = ItemUpdater.updateItem(oldItem);
            if (oldItem == null || oldItem.equals(newItem)) continue;
            inventory.setItem(i, newItem);
        }
    }

    @EventHandler
    public void onPlayerPickUp(EntityPickupItemEvent event) {
        if (!Settings.UPDATE_ITEMS.toBool()) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack oldItem = event.getItem().getItemStack();
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        if (oldItem.equals(newItem)) return;
        event.getItem().setItemStack(newItem);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(PrepareItemEnchantEvent event) {
        String id = OraxenItems.getIdByItem(event.getItem());
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemEnchant(PrepareAnvilEvent event) {
        ItemStack item = event.getInventory().getItem(0);
        ItemStack result = event.getResult();
        String id = OraxenItems.getIdByItem(item);
        ItemBuilder builder = OraxenItems.getItemById(id);
        if (builder == null || !builder.hasOraxenMeta()) return;

        if (builder.getOraxenMeta().isDisableEnchanting()) {
            if (result == null || item == null) return;
            if (!result.getEnchantments().equals(item.getEnchantments()))
                event.setResult(null);
        }
    }

    private static final NamespacedKey GUI_ITEM_KEY = Objects.requireNonNull(NamespacedKey.fromString("oraxen:if-uuid"));
    public static ItemStack updateItem(ItemStack oldItem) {
        String id = OraxenItems.getIdByItem(oldItem);
        if (id == null) return oldItem;

        // Oraxens Inventory adds a dumb PDC entry to items, this will remove them
        // Done here over [ItemsView] as this method is called anyway and supports old items
        ItemUtils.editItemMeta(oldItem, itemMeta -> itemMeta.getPersistentDataContainer().remove(GUI_ITEM_KEY));

        Optional<ItemBuilder> optionalBuilder = OraxenItems.getOptionalItemById(id);
        if (optionalBuilder.isEmpty() || optionalBuilder.get().getOraxenMeta().isNoUpdate()) return oldItem;
        ItemBuilder newItemBuilder = optionalBuilder.get();

        ItemStack newItem = NMSHandlers.getHandler() != null ? NMSHandlers.getHandler().copyItemNBTTags(oldItem, newItemBuilder.build()) : newItemBuilder.build();
        newItem.setAmount(oldItem.getAmount());

        ItemUtils.editItemMeta(newItem, itemMeta -> {
            ItemMeta oldMeta = oldItem.getItemMeta();
            ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;
            PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();
            PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();

            // Transfer over all PDC entries from oldItem to newItem
            List<Map<?, ?>> oldPdcMap = PersistentDataSerializer.toMapList(oldPdc);
            PersistentDataSerializer.fromMapList(oldPdcMap, itemPdc);

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            int cmd = newMeta.hasCustomModelData() ? newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? oldMeta.getCustomModelData() : 0;
            itemMeta.setCustomModelData(cmd);

            // If OraxenItem has no lore, we should assume that 3rd-party plugin has added lore
            if (Settings.OVERRIDE_ITEM_LORE.toBool()) itemMeta.setLore(newMeta.getLore());
            else itemMeta.setLore(oldMeta.getLore());

            // Only change AttributeModifiers if the new item has some
            if (newMeta.hasAttributeModifiers()) itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());
            else itemMeta.setAttributeModifiers(oldMeta.getAttributeModifiers());

            // Transfer over durability from old item
            if (itemMeta instanceof Damageable damageable && oldMeta instanceof Damageable oldDmg) {
                damageable.setDamage(oldDmg.getDamage());
            }

            if (oldMeta.isUnbreakable()) itemMeta.setUnbreakable(true);

            if (itemMeta instanceof LeatherArmorMeta leatherMeta && oldMeta instanceof LeatherArmorMeta oldLeatherMeta && newMeta instanceof LeatherArmorMeta newLeatherMeta) {
                // If it is not custom armor, keep color
                if (oldItem.getType() == Material.LEATHER_HORSE_ARMOR) leatherMeta.setColor(oldLeatherMeta.getColor());
                // If it is custom armor we use newLeatherMeta color, since the builder would have been altered
                // in the process of creating the shader images. Then we just save the builder to update the config
                else {
                    leatherMeta.setColor(newLeatherMeta.getColor());
                    newItemBuilder.save();
                }
            }

            if (itemMeta instanceof PotionMeta potionMeta && oldMeta instanceof PotionMeta oldPotionMeta) {
                potionMeta.setColor(oldPotionMeta.getColor());
            }

            if (itemMeta instanceof MapMeta mapMeta && oldMeta instanceof MapMeta oldMapMeta) {
                mapMeta.setColor(oldMapMeta.getColor());
            }

            if (VersionUtil.atOrAbove("1.20") && itemMeta instanceof ArmorMeta armorMeta && oldMeta instanceof ArmorMeta oldArmorMeta) {
                armorMeta.setTrim(oldArmorMeta.getTrim());
            }

            // Parsing with legacy here to fix any inconsistensies caused by server serializers etc
            String oldDisplayName = AdventureUtils.parseLegacy(oldMeta.getDisplayName());
            String originalName = AdventureUtils.parseLegacy(oldPdc.getOrDefault(ORIGINAL_NAME_KEY, DataType.STRING, ""));
            if (Settings.OVERRIDE_RENAMED_ITEMS.toBool()) {
                itemMeta.setDisplayName(newMeta.getDisplayName());
            } else if (!originalName.equals(oldDisplayName)) {
                itemMeta.setDisplayName(oldMeta.getDisplayName());
            } else {
                itemMeta.setDisplayName(newMeta.getDisplayName());
            }

            itemPdc.set(ORIGINAL_NAME_KEY, DataType.STRING, newMeta.getDisplayName());
            // If the item is not unstackable, we should remove the unstackable tag
            if (!newItemBuilder.isUnstackable()) itemPdc.remove(UNSTACKABLE_KEY);
        });

        return newItem;
    }

}
