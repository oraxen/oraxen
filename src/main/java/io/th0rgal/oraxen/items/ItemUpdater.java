package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.provided.misc.backpack.BackpackMechanic;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.th0rgal.oraxen.items.ItemBuilder.PRE_RENAMED_KEY;

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

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            itemMeta.setCustomModelData(newMeta.hasCustomModelData() ? newMeta.getCustomModelData() : oldMeta.hasCustomModelData() ? oldMeta.getCustomModelData() : 0);

            // Lore might be changable ingame, but I think it is safe to just set it to new
            itemMeta.setLore(newMeta.getLore());

            // Attribute modifiers are only able to be changed via config so no reason to chekc old
            itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());

            // If the PRE_RENAMED value is equal to newMetas displayname, it should not be updated to prevent removing renaming
            String preRenamed = oldPdc.getOrDefault(PRE_RENAMED_KEY, DataType.STRING, "");
            Logs.debug(preRenamed);
            Logs.debug(newMeta.getDisplayName());
            Logs.debug(oldMeta.getDisplayName());
            if (preRenamed.equals(newMeta.getDisplayName()))
                itemMeta.setDisplayName(oldMeta.getDisplayName());
            else itemMeta.getPersistentDataContainer().set(PRE_RENAMED_KEY, DataType.STRING, newMeta.getDisplayName());

            if (OraxenItems.hasMechanic(id, "backpack") && oldPdc.has(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)) {
                itemMeta.getPersistentDataContainer().set(
                        BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY, Objects.requireNonNull(
                                oldPdc.get(BackpackMechanic.BACKPACK_KEY, DataType.ITEM_STACK_ARRAY)
                        ));
            }
        });
        return newItem;
    }

}
