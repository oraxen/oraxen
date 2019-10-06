package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.items.modifiers.ItemModifier;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.List;

public class ItemParser {

    ItemBuilder item;

    public ItemParser(ConfigurationSection section) {
        this.item = new ItemBuilder(Material.getMaterial(section.getString("material")));

        if (section.contains("durability"))
            item.setDurability((short) section.getInt("durability"));

        int customModelData = -1;
        if (section.contains("custom_model_data")) {
            customModelData = section.getInt("custom_model_data");
            item.setCustomModelData(customModelData);
        }

        if (section.contains("displayname"))
            item.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("displayname")));

        if (section.contains("unbreakable"))
            item.setUnbreakable(section.getBoolean("unbreakable"));

        if (section.contains("color")) {
            String[] colors = section.getString("color").split(", ");
            item.setColor(org.bukkit.Color.fromRGB(Integer.parseInt(colors[0]), Integer.parseInt(colors[1]), Integer.parseInt(colors[2])));
        }

        if (!section.contains("injectID") || section.getBoolean("injectId"))
            item.setCustomTag(new NamespacedKey(OraxenPlugin.get(), "id"), PersistentDataType.STRING, section.getName());

        if (section.contains("ItemFlags")) {
            List<String> itemFlags = section.getStringList("ItemFlags");
            for (String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }

        if (section.contains("AttributeModifiers")) {

            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
                    List<LinkedHashMap<String, Object>> attributes = (List<LinkedHashMap<String, Object>>) section.getList("AttributeModifiers");
            for (LinkedHashMap<String, Object> attributeJson : attributes) {
                AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                Attribute attribute = Attribute.valueOf((String) attributeJson.get("attribute"));
                item.addAttributeModifiers(attribute, attributeModifier);
            }
        }

        if (section.contains("Enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("Enchantments");
            for (String enchant : enchantSection.getKeys(false))
                item.addEnchant(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)), enchantSection.getInt(enchant));
        }

        if (section.isConfigurationSection("Mechanics")) {
            ConfigurationSection mechanicsSection = section.getConfigurationSection("Mechanics");
            for (String mechanicID : mechanicsSection.getKeys(false)) {
                MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);
                if (factory != null) {
                    Mechanic mechanic = factory.parse(mechanicsSection.getConfigurationSection(mechanicID));
                    // Apply item modifiers
                    for (ItemModifier itemModifier : mechanic.getItemModifiers())
                        item = itemModifier.getItem(item);
                }
            }
        }

        if (section.isConfigurationSection("Pack")) {
            item.setPackInfos(new PackInfos(section.getConfigurationSection("Pack"), customModelData));
        }

    }

    public ItemBuilder buildItem() {
        return item;
    }

}
