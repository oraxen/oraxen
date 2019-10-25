package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;

import io.th0rgal.oraxen.utils.Logs;
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
import java.util.function.Function;

class ModelData {

    private Material type;
    private int durability;
    private static Map<Material, Map<String, Integer>> datas = new HashMap<>();

    public ModelData(Material type, String model, int durability) {
        this.type = type;
        this.durability = durability;
        Map<String, Integer> usedDurabilities = datas.getOrDefault(type, new HashMap<>());
        usedDurabilities.put(model, durability);
        datas.put(type, usedDurabilities);
    }

    public Material getType() {
        return type;
    }

    public int getDurability() {
        return durability;
    }

    public static int generateId(String model, Material type) {
        Map<String, Integer> usedDurabilities;
        if (!datas.containsKey(type)) {
            usedDurabilities = new HashMap<>();
            usedDurabilities.put(model, 0);
            datas.put(type, usedDurabilities);
            return 0;
        } else
            usedDurabilities = datas.get(type);

        if (usedDurabilities.containsKey(model))
            return usedDurabilities.get(model);
        int currentMaxDurability = Collections.max(usedDurabilities.values());
        for (int i = 0; i < currentMaxDurability; i++) {
            if (!usedDurabilities.containsValue(i)) { // if the id is available
                usedDurabilities.put(model, i);
                datas.put(type, usedDurabilities);
                return i;
            }
        }
        //if no durability was available between the choosed, let's create a new one bigger
        int newMaxDurability = currentMaxDurability + 1;
        usedDurabilities.put(model, newMaxDurability);
        datas.put(type, usedDurabilities);
        return newMaxDurability;
    }
}

public class ItemParser {

    private static Map<String, ModelData> modelDatasByID = new HashMap<>();

    private PackInfos packInfos;
    private ConfigurationSection section;
    private Material type;

    public ItemParser(ConfigurationSection section) {
        this.section = section;
        this.type = Material.getMaterial(section.getString("material"));

        if (section.isConfigurationSection("Pack")) {
            ConfigurationSection packSection = section.getConfigurationSection("Pack");
            this.packInfos = new PackInfos(packSection);
            if (packSection.isInt("custom_model_data"))
                modelDatasByID.put(section.getName(), new ModelData(
                        type,
                        packInfos.getModelName(),
                        packSection.getInt("custom_model_data")));
        }
    }

    public ItemBuilder buildItem() {

        ItemBuilder item = new ItemBuilder(type);

        if (section.contains("durability"))
            item.setDurability((short) section.getInt("durability"));

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
                    for (Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                        item = itemModifier.apply(item);
                }
            }
        }


        if (packInfos != null) {
            int customModelData;
            if (modelDatasByID.containsKey(section.getName())) {
                customModelData = modelDatasByID.get(section.getName()).getDurability();
            } else {
                customModelData = ModelData.generateId(packInfos.getModelName(), type);
            }
            item.setCustomModelData(customModelData);
            packInfos.setCustomModelData(customModelData);
            item.setPackInfos(packInfos);

        }

        return item;
    }

}
