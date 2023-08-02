package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ItemParser {

    public static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private final OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private Material type;
    private WrappedMMOItem mmoItem;
    private WrappedCrucibleItem crucibleItem;
    private boolean configUpdated = false;

    public ItemParser(ConfigurationSection section) {
        this.section = section;

        ConfigurationSection crucibleSection = section.getConfigurationSection("crucible");
        ConfigurationSection mmoSection = section.getConfigurationSection("mmoitem");
        if (crucibleSection != null)
            crucibleItem = new WrappedCrucibleItem(crucibleSection);
        else if (mmoSection != null)
            mmoItem = new WrappedMMOItem(mmoSection);
        else type = Material.getMaterial(section.getString("material", "PAPER"));

        oraxenMeta = new OraxenMeta();
        if (section.isConfigurationSection("Pack")) {
            ConfigurationSection packSection = section.getConfigurationSection("Pack");
            oraxenMeta.setPackInfos(packSection);
            assert packSection != null;
            if (packSection.isInt("custom_model_data"))
                MODEL_DATAS_BY_ID.put(section.getName(),
                        new ModelData(type, oraxenMeta.getModelName(), packSection.getInt("custom_model_data")));
        }
    }

    public boolean usesMMOItems() {
        return type == null && crucibleItem == null && mmoItem != null;
    }

    public boolean usesCrucibleItems() {
        return type == null && mmoItem == null && crucibleItem != null;
    }

    private String parseComponentDisplayName(String miniString) {
        Component component = AdventureUtils.MINI_MESSAGE.deserialize(miniString);
        // If it has no formatting, set color to WHITE to prevent Italic
        return AdventureUtils.LEGACY_SERIALIZER.serialize(component.colorIfAbsent(NamedTextColor.WHITE));
    }

    private String parseComponentLore(String miniString) {
        Component component = AdventureUtils.MINI_MESSAGE.deserialize(miniString);
        // If it has no formatting, set color to WHITE to prevent Italic
        return AdventureUtils.LEGACY_SERIALIZER.serialize(component);
    }

    public ItemBuilder buildItem() {
        return buildItem(section.contains("displayname") ? parseComponentDisplayName(section.getString("displayname")) : null);
    }

    public ItemBuilder buildItem(String name) {
        ItemBuilder item;
        if (usesCrucibleItems()) item = new ItemBuilder(crucibleItem);
        else if (usesMMOItems()) item = new ItemBuilder(mmoItem);
        else item = new ItemBuilder(type);

        if (name != null)
            item.setDisplayName(name);
        return applyConfig(item);
    }

    private ItemBuilder applyConfig(ItemBuilder item) {

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            lore.replaceAll(this::parseComponentLore);
            item.setLore(lore);
        }

        if (section.contains("durability"))
            item.setDurability((short) section.getInt("durability"));
        if (section.contains("unbreakable"))
            item.setUnbreakable(section.getBoolean("unbreakable", false));
        if (section.contains("unstackable"))
            item.setUnstackable(section.getBoolean("unstackable", false));
        if (section.contains("color"))
            item.setColor(Utils.toColor(section.getString("color", "#FFFFFF")));

        parseMiscOptions(item);
        parseVanillaSections(item);
        parseOraxenSections(item);
        item.setOraxenMeta(oraxenMeta);
        return item;
    }

    private void parseMiscOptions(ItemBuilder item) {
        oraxenMeta.setNoUpdate(section.getBoolean("no_auto_update", false));
        oraxenMeta.setDisableEnchanting(section.getBoolean("disable_enchanting", false));
        oraxenMeta.setExcludedFromInventory(section.getBoolean("excludeFromInventory", false));
        oraxenMeta.setExcludedFromCommands(section.getBoolean("excludeFromCommands", false));

        if (!section.contains("injectID") || section.getBoolean("injectId"))
            item.setCustomTag(new NamespacedKey(OraxenPlugin.get(), "id"), PersistentDataType.STRING, section.getName());
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private void parseVanillaSections(ItemBuilder item) {

        if (section.contains("ItemFlags")) {
            List<String> itemFlags = section.getStringList("ItemFlags");
            for (String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }

        if (section.contains("PotionEffects")) {
            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
            List<LinkedHashMap<String, Object>> potionEffects = (List<LinkedHashMap<String, Object>>) section
                    .getList("PotionEffects");
            if (potionEffects == null) return;
            for (Map<String, Object> serializedPotionEffect : potionEffects) {
                PotionEffectType effect = PotionEffectType.getByName((String) serializedPotionEffect.get("type"));
                if (effect == null) return;
                int duration = (int) serializedPotionEffect.get("duration");
                int amplifier = (int) serializedPotionEffect.get("amplifier");
                boolean ambient = (boolean) serializedPotionEffect.get("ambient");
                boolean particles = (boolean) serializedPotionEffect.get("particles");
                boolean icon = (boolean) serializedPotionEffect.get("icon");
                item.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles, icon));
            }
        }

        if (section.contains("PersistentData")) {
            try {
                List<LinkedHashMap<String, Object>> dataHolder = (List<LinkedHashMap<String, Object>>) section
                        .getList("PersistentData");
                for (LinkedHashMap<String, Object> attributeJson : dataHolder) {
                    String[] keyContent = ((String) attributeJson.get("key")).split(":");
                    final Object persistentDataType = PersistentDataType.class
                            .getDeclaredField((String) attributeJson.get("type")).get(null);
                    item.addCustomTag(new NamespacedKey(keyContent[0], keyContent[1]),
                            (PersistentDataType) persistentDataType,
                            attributeJson.get("value"));
                }
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        if (section.contains("AttributeModifiers")) {
            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
            List<LinkedHashMap<String, Object>> attributes = (List<LinkedHashMap<String, Object>>) section.getList("AttributeModifiers");
            if (attributes != null) for (LinkedHashMap<String, Object> attributeJson : attributes) {
                attributeJson.putIfAbsent("uuid", UUID.randomUUID().toString());
                attributeJson.putIfAbsent("name", "oraxen:modifier");
                AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                Attribute attribute = Attribute.valueOf((String) attributeJson.get("attribute"));
                item.addAttributeModifiers(attribute, attributeModifier);
            }
        }

        if (section.contains("Enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("Enchantments");
            if (enchantSection != null) for (String enchant : enchantSection.getKeys(false))
                item.addEnchant(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)),
                        enchantSection.getInt(enchant));
        }
    }

    private void parseOraxenSections(ItemBuilder item) {

        ConfigurationSection mechanicsSection = section.getConfigurationSection("Mechanics");
        if (mechanicsSection != null) for (String mechanicID : mechanicsSection.getKeys(false)) {
            MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);
            if (factory != null) {
                ConfigurationSection mechanicSection = mechanicsSection.getConfigurationSection(mechanicID);
                if (mechanicSection == null) continue;
                //if (mechanicID.equals("furniture") && !FurnitureFactory.setDefaultType(mechanicSection)) configUpdated = true;
                Mechanic mechanic = factory.parse(mechanicSection);
                // Apply item modifiers
                for (Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                    item = itemModifier.apply(item);
            }
        }

        if (oraxenMeta.hasPackInfos()) {
            int customModelData;
            if (MODEL_DATAS_BY_ID.containsKey(section.getName())) {
                customModelData = MODEL_DATAS_BY_ID.get(section.getName()).getModelData();
            } else {
                customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
                configUpdated = true;
                if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool())
                    section.getConfigurationSection("Pack").set("custom_model_data", customModelData);
            }
            item.setCustomModelData(customModelData);
            oraxenMeta.setCustomModelData(customModelData);
        }
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }

}
