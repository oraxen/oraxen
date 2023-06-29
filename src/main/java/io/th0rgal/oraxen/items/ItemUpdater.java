package io.th0rgal.oraxen.items;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;

public class ItemUpdater implements Listener {

    public ItemUpdater() {
        if (furnitureUpdateTask != null) furnitureUpdateTask.cancel();
        furnitureUpdateTask = new FurnitureUpdateTask();
        int delay = (Settings.FURNITURE_UPDATE_DELAY.getValue() instanceof Integer integer) ? integer : 5;
        furnitureUpdateTask.runTaskTimer(OraxenPlugin.get(), 0, delay * 20L);
    }

    public static HashSet<Entity> furnitureToUpdate = new HashSet<>();
    public static FurnitureUpdateTask furnitureUpdateTask;
    public static class FurnitureUpdateTask extends BukkitRunnable {

        @Override
        public void run() {
            for (Entity entity : new HashSet<>(furnitureToUpdate)) {
                OraxenFurniture.updateFurniture(entity);
                furnitureToUpdate.remove(entity);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Settings.AUTO_UPDATE_ITEMS.toBool()) return;

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
        if (!Settings.AUTO_UPDATE_ITEMS.toBool()) return;
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack oldItem = event.getItem().getItemStack();
        ItemStack newItem = ItemUpdater.updateItem(oldItem);
        if (oldItem.equals(newItem)) return;
        event.getItem().setItemStack(newItem);
    }

    @EventHandler
    public void onEntityLoad(EntitiesLoadEvent event) {
        if (!Settings.AUTO_UPDATE_ITEMS.toBool()) return;
        if (!Settings.UPDATE_FURNITURE_ON_LOAD.toBool()) return;

        for (Entity entity : event.getEntities())
            if (OraxenFurniture.isFurniture(entity))
                ItemUpdater.furnitureToUpdate.add(entity);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ItemStack updateItem(ItemStack oldItem) {
        String id = OraxenItems.getIdByItem(oldItem);
        if (id == null) return oldItem;

        // Oraxens Inventory adds a dumb PDC entry to items, this will remove them
        // Done here over [ItemsView] as this method is called anyway and supports old items
        NamespacedKey guiItemKey = Objects.requireNonNull(NamespacedKey.fromString("oraxen:if-uuid"));
        Utils.editItemMeta(oldItem, itemMeta -> itemMeta.getPersistentDataContainer().remove(guiItemKey));

        Optional<ItemBuilder> newItemBuilder = OraxenItems.getOptionalItemById(id);
        if (newItemBuilder.isEmpty() || newItemBuilder.get().getOraxenMeta().isNoUpdate()) return oldItem;

        ItemStack newItem = newItemBuilder.get().build();
        newItem.setAmount(oldItem.getAmount());
        Utils.editItemMeta(newItem, itemMeta -> {
            ItemMeta oldMeta = oldItem.getItemMeta();
            ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;
            PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();
            PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();

            // Transfer over all PDC entries - Uses method from JeffLib through CustomBlockData
            for (NamespacedKey key : oldPdc.getKeys()) {
                PersistentDataType dataType = CustomBlockData.getDataType(oldPdc, key);
                Object pdcValue = oldPdc.get(key, dataType);
                if (pdcValue != null) itemPdc.set(key, dataType, pdcValue);
            }

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            int cmd = newMeta.hasCustomModelData() ? newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? oldMeta.getCustomModelData() : 0;
            itemMeta.setCustomModelData(cmd);

            // Lore might be changable ingame, but I think it is safe to just set it to new
            if (Settings.OVERRIDE_LORE.toBool()) itemMeta.setLore(newMeta.getLore());
            else itemMeta.setLore(oldMeta.getLore());

            // Attribute modifiers are only changable via config so no reason to check old
            itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());

            // Transfer over durability from old item
            if (itemMeta instanceof Damageable damageable && oldMeta instanceof Damageable oldDmg) {
                damageable.setDamage(oldDmg.getDamage());
            }

            if (itemMeta instanceof LeatherArmorMeta leatherMeta && oldMeta instanceof LeatherArmorMeta oldLeatherMeta) {
                leatherMeta.setColor(oldLeatherMeta.getColor());
            }

            if (itemMeta instanceof PotionMeta potionMeta && oldMeta instanceof PotionMeta oldPotionMeta) {
                potionMeta.setColor(oldPotionMeta.getColor());
            }

            if (itemMeta instanceof MapMeta mapMeta && oldMeta instanceof MapMeta oldMapMeta) {
                mapMeta.setColor(oldMapMeta.getColor());
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
        });
        return newItem;
    }

}
