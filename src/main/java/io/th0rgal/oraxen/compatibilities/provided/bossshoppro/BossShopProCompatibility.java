package io.th0rgal.oraxen.compatibilities.provided.bossshoppro;

import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import net.kyori.adventure.text.minimessage.Template;
import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.events.BSCreatedShopItemEvent;
import org.black_ixx.bossshop.events.BSRegisterTypesEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class BossShopProCompatibility extends CompatibilityProvider<BossShop> {

    @EventHandler
    public void onBSCreatedShopItem(BSCreatedShopItemEvent event) {
        ConfigurationSection menuItem = event.getConfigurationSection().getConfigurationSection("MenuItem");
        if (menuItem == null)
            return;
        String itemID = menuItem.getString("oraxen");
        int amount = menuItem.getInt("amount");
        if (itemID == null)
            return;
        ItemStack itemStack = new ItemStack(Material.AIR);
        if (OraxenItems.exists(itemID)){
            itemStack = OraxenItems.getItemById(itemID).build().clone();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null) {
                String name = menuItem.getString("name");
                if (name != null) {
                    itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                }

                List<String> lore = menuItem.getStringList("lore");
                if (lore != null && lore.size() > 0) {
                    List<String> newLore = lore
                            .stream()
                            .map((line) -> ChatColor.translateAlternateColorCodes('&', line))
                            .collect(Collectors.toList());
                    itemMeta.setLore(newLore);
                }

                itemStack.setItemMeta(itemMeta);
            }
        } else {
            Message.ITEM_NOT_FOUND.log(Template.template("item", itemID));
        }

        if (amount != 0)
            itemStack.setAmount(amount);
        event.getShopItem().setItem(itemStack, false);
    }

    @EventHandler
    public void onBSRegisterTypes(BSRegisterTypesEvent event) {
        new OraxenReward().register();
        new OraxenPrice().register();
    }

}
