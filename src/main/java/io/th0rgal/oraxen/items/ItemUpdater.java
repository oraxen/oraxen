package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.durability.DurabilityMechanic;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;
import java.util.Optional;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;

public class ItemUpdater implements Listener {

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


    public static ItemStack updateItem(ItemStack oldItem) {
        String id = OraxenItems.getIdByItem(oldItem);
        if (id == null) return oldItem;

        Optional<ItemBuilder> newItemBuilder = OraxenItems.getOptionalItemById(id);

        if (newItemBuilder.isEmpty() || newItemBuilder.get().getOraxenMeta().isNoUpdate())
            return oldItem;
        ItemStack newItem = newItemBuilder.get().build();
        newItem.setAmount(oldItem.getAmount());
        Utils.editItemMeta(newItem, itemMeta -> {
            ItemMeta oldMeta = oldItem.getItemMeta();
            ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;
            PersistentDataContainer oldPdc = oldMeta.getPersistentDataContainer();
            PersistentDataContainer itemPdc = itemMeta.getPersistentDataContainer();

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            int cmd = newMeta.hasCustomModelData() ? newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? oldMeta.getCustomModelData() : 0;
            itemMeta.setCustomModelData(cmd);

            // Lore might be changable ingame, but I think it is safe to just set it to new
            itemMeta.setLore(newMeta.getLore());

            // Attribute modifiers are only able to be changed via config so no reason to chekc old
            itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());

            // Transfer over durability from old item
            if (itemMeta instanceof Damageable damageable && oldMeta instanceof Damageable oldDmg)
                damageable.setDamage(oldDmg.getDamage());

            // Transfer over custom durability from DurabilityMechanic from old item
            int customDurability = oldPdc.getOrDefault(DurabilityMechanic.DURABILITY_KEY, DataType.INTEGER, 0);
            if (customDurability > 0)
                itemPdc.set(DurabilityMechanic.DURABILITY_KEY, DataType.INTEGER, customDurability);

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


            if (OraxenItems.hasMechanic(id, "backpack") && oldPdc.has(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)) {
                itemPdc.set(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY,
                        oldPdc.getOrDefault(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, new ItemStack[0]));
            }
        });
        return newItem;
    }

}
