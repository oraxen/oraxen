package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.items.mechanics.MechanicsManager;
import io.th0rgal.oraxen.items.modifiers.ItemModifier;
import io.th0rgal.oraxen.settings.Message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.List;

public class ItemParser {

    Item item;

    public ItemParser(ConfigurationSection section) {
        this.item = new Item(Material.valueOf(section.getString("material")));

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
            item.setStringNBTTag("OxnId", section.getName());

        if (section.contains("NBTTags")) {

            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
                    List<LinkedHashMap<String, ?>> tagsList = (List<LinkedHashMap<String, ?>>) section.getList("NBTTags");

            for (LinkedHashMap<String, ?> tag : tagsList) {
                String type = tag.get("type").toString();
                String field = tag.get("name").toString();

                switch (type) {
                    case "boolean":
                        item.setBooleanNBTTag(field, Boolean.parseBoolean(tag.get("value").toString()));
                        break;
                    case "int":
                        int value = Integer.parseInt(tag.get("value").toString());
                        item.setIntNBTTag(field, value);
                        break;
                    case "String":
                        item.setStringNBTTag(field, tag.get("value").toString());
                        break;
                    default:
                        Message.WRONG_TYPE.send(Bukkit.getConsoleSender());
                        break;
                }

            }
        }

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
            for (String mechanicID : mechanicsSection.getKeys(false))
                MechanicsManager.addItemMechanic(section.getName(), mechanicsSection.getConfigurationSection(mechanicID));
        }

        //apply item modifiers
        for (ItemModifier itemModifier : MechanicsManager.getModifiersByItemID(section.getName())) {
            item = itemModifier.getItem(item);
        }

        if (section.isConfigurationSection("Pack")) {
            item.setPackInfos(new PackInfos(section.getConfigurationSection("Pack"), customModelData));
        }

    }

    public Item buildItem() {
        return item;
    }

}
