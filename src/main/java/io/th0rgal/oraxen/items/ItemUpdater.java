package io.th0rgal.oraxen.items;

import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Optional;

public class ItemUpdater implements Listener {

    public static NamespacedKey ANVIL_RENAMED = NamespacedKey.fromString("oraxen:anvil_renamed");

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Settings.AUTO_UPDATE_ITEMS.toBool())
            return;
        PlayerInventory inventory = event.getPlayer().getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack oldItem = inventory.getItem(i);
            ItemStack newItem = ItemUpdater.updateItem(oldItem);
            if (oldItem == null || oldItem.equals(newItem))
                continue;
            inventory.setItem(i, newItem);
        }
    }

    @EventHandler
    public void onAnvilRename(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory inventory)) return;
        if (event.getSlot() != 2) return;

        ItemStack firstItem = inventory.getItem(0);
        ItemStack resultItem = inventory.getItem(2);
        String resultId = OraxenItems.getIdByItem(resultItem);
        ItemStack oraxenItem = OraxenItems.getItemById(resultId).build();
        if (firstItem == null || resultItem == null || resultId == null || oraxenItem == null) return;
        ItemMeta firstMeta = firstItem.getItemMeta();
        ItemMeta resultMeta = resultItem.getItemMeta();
        ItemMeta oraxenMeta = oraxenItem.getItemMeta();
        if (firstMeta == null || !firstMeta.hasDisplayName()) return;
        if (resultMeta == null || !resultMeta.hasDisplayName()) return;
        if (resultMeta.getDisplayName().equals(firstMeta.getDisplayName())) return;
        if (oraxenMeta != null && !oraxenMeta.hasDisplayName()) return;

        if (oraxenMeta != null && oraxenMeta.hasDisplayName()) {
            String resultDisplay = AdventureUtils.PLAIN_TEXT.deserialize(resultMeta.getDisplayName()).content();
            String baseDisplay = AdventureUtils.PLAIN_TEXT.deserialize(oraxenMeta.getDisplayName()).content();
            if (resultDisplay.equals(baseDisplay)) {
                resultMeta.setDisplayName(oraxenMeta.getDisplayName());
                resultItem.setItemMeta(resultMeta);
            }
        } else Utils.editItemMeta(resultItem, itemMeta ->
                itemMeta.getPersistentDataContainer().set(ANVIL_RENAMED, DataType.STRING, resultMeta.getDisplayName()));
    }


    public static ItemStack updateItem(ItemStack oldItem) {
        String id = OraxenItems.getIdByItem(oldItem);
        if (id == null)
            return oldItem;
        Optional<ItemBuilder> newItemBuilder = OraxenItems.getOptionalItemById(id);

        if (newItemBuilder.isEmpty() || newItemBuilder.get().getOraxenMeta().isNoUpdate())
            return oldItem;

        ItemStack newItem = newItemBuilder.get().build();
        newItem.setAmount(oldItem.getAmount());
        Utils.editItemMeta(newItem, itemMeta -> {
            ItemMeta oldMeta = oldItem.getItemMeta();
            ItemMeta newMeta = newItem.getItemMeta();
            if (oldMeta == null || newMeta == null) return;

            // Add all enchantments from oldItem and add all from newItem aslong as it is not the same Enchantments
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            for (Map.Entry<Enchantment, Integer> entry : newMeta.getEnchants().entrySet().stream().filter(e -> !oldMeta.getEnchants().containsKey(e.getKey())).toList())
                itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);

            itemMeta.setCustomModelData(newMeta.hasCustomModelData() ? newMeta.getCustomModelData() : oldMeta.getCustomModelData());

            // Lore might be changable ingame, but I think it is safe to just set it to new
            itemMeta.setLore(newMeta.getLore());

            // Attribute modifiers are only able to be changed via config so no reason to chekc old
            itemMeta.setAttributeModifiers(newMeta.getAttributeModifiers());

            // Renaming items should be kept, so check old against new and add it if its from ANVIL_RENAMED
            if (oldMeta.hasDisplayName() && oldMeta.getPersistentDataContainer().has(ANVIL_RENAMED, DataType.STRING))
                itemMeta.setDisplayName(oldMeta.getDisplayName());
            else itemMeta.setDisplayName(newMeta.getDisplayName());
        });
        return newItem;
    }

}
