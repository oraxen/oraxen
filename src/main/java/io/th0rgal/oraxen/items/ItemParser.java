package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.settings.Pack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ItemParser {

    private static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private final Material type;
    private boolean configUpdated = false;

    public ItemParser(ConfigurationSection section) {
        this.section = section;
        this.type = Material.getMaterial(section.getString("material"));

        if (section.isConfigurationSection("Pack")) {
            ConfigurationSection packSection = section.getConfigurationSection("Pack");
            this.oraxenMeta = new OraxenMeta(packSection);
            if (packSection.isInt("custom_model_data"))
                MODEL_DATAS_BY_ID.put(section.getName(), new ModelData(
                        type,
                        oraxenMeta.getModelName(),
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

        if (oraxenMeta != null) {
            int customModelData;
            if (MODEL_DATAS_BY_ID.containsKey(section.getName())) {
                customModelData = MODEL_DATAS_BY_ID.get(section.getName()).getDurability();
            } else {
                customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
                if ((boolean) Pack.SET_MODEL_ID.getValue()) {
                    this.configUpdated = true;
                    section.getConfigurationSection("Pack").set("custom_model_data", customModelData);
                }
            }
            item.setCustomModelData(customModelData);
            oraxenMeta.setCustomModelData(customModelData);
            item.setOraxenMeta(oraxenMeta);

        }

        return item;
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }

}
