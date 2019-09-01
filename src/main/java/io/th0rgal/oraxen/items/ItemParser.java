package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.items.mechanics.MechanicsManager;
import io.th0rgal.oraxen.settings.Message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ItemParser {

    Item item;

    public ItemParser(ConfigurationSection section) {
        this.item = new Item(Material.valueOf(section.getString("material")));

        if (section.contains("data"))
            item.setDurability((short) section.getInt("data"));

        if (section.contains("displayname"))
            item.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("displayname")));

        if (section.contains("unbreakable"))
            item.setUnbreakable(section.getBoolean("unbreakable"));

        if (!section.contains("injectID") || section.getBoolean("injectId"))
            item.setStringNBTTag("OxnId", section.getName());

        int customModelData = -1;
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
                        if (field.equals("CustomModelData"))
                            customModelData = value;

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

        if (section.isConfigurationSection("Mechanics")) {
            ConfigurationSection mechanicsSection = section.getConfigurationSection("Mechanics");
            for (String mechanicID : mechanicsSection.getKeys(false))
                MechanicsManager.addItemMechanic(section.getName(), mechanicsSection.getConfigurationSection(mechanicID));
        }

        if (section.isConfigurationSection("Pack")) {
            item.setPackInfos(new PackInfos(section.getConfigurationSection("Pack"), customModelData));
        }

    }

    public Item buildItem() {
        return item;
    }

}
