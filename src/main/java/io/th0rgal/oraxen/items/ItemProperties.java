package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.AppearanceMode;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.ArmorStandProperties;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import io.th0rgal.oraxen.utils.wrappers.EnchantmentWrapper;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public final class ItemProperties {

    private final ConfigurationSection section;
    private final Material type;
    private final OraxenMeta oraxenMeta;
    private final Map<String, ModelData> modelDatasById;
    private final ItemMigrator migrator;

    public ItemProperties(final ConfigurationSection section, final Material type, final OraxenMeta oraxenMeta,
            final Map<String, ModelData> modelDatasById) {
        this(section, type, oraxenMeta, modelDatasById, null);
    }

    public ItemProperties(final ConfigurationSection section, final Material type, final OraxenMeta oraxenMeta,
            final Map<String, ModelData> modelDatasById, final ItemMigrator migrator) {
        this.section = section;
        this.type = type;
        this.oraxenMeta = oraxenMeta;
        this.modelDatasById = modelDatasById;
        this.migrator = migrator;
    }

    public boolean applyBasic(final ItemBuilder item) {
        boolean configUpdated = false;
        if (section.contains("displayname")) {
            if (VersionUtil.atOrAbove("1.20.5"))
                configUpdated = true;
            else
                item.setDisplayName(section.getString("displayname", ""));
        }
        if (section.contains("customname")) {
            if (!VersionUtil.atOrAbove("1.20.5"))
                configUpdated = true;
            else
                item.setDisplayName(section.getString("customname", ""));
        }
        // if (section.contains("type"))
        // item.setType(Material.getMaterial(section.getString("type", "PAPER")));
        if (section.contains("lore"))
            item.setLore(section.getStringList("lore").stream().map(AdventureUtils::parseMiniMessage).toList());
        if (section.contains("unbreakable"))
            item.setUnbreakable(section.getBoolean("unbreakable", false));
        if (section.contains("unstackable"))
            item.setUnstackable(section.getBoolean("unstackable", false));
        if (section.contains("color"))
            item.setColor(Utils.toColor(section.getString("color", "#FFFFFF")));
        if (section.contains("trim_pattern"))
            item.setTrimPattern(Key.key(section.getString("trim_pattern", "")));
        return configUpdated;
    }

    public void applyMiscAndVanilla(final ItemBuilder item, final ConfigurationSection mergedSection) {
        parseMiscOptions(item, mergedSection);
        parseVanillaSections(item, mergedSection);
    }

    private void parseMiscOptions(final ItemBuilder item, final ConfigurationSection mergedSection) {
        if (section.getBoolean("injectId", true))
            item.setCustomTag(OraxenItems.ITEM_ID, PersistentDataType.STRING, section.getName());
        oraxenMeta.setNoUpdate(mergedSection.getBoolean("no_auto_update", false));
        oraxenMeta.setDisableEnchanting(mergedSection.getBoolean("disable_enchanting", false));
        oraxenMeta.setExcludedFromInventory(mergedSection.getBoolean("excludeFromInventory", false));
        oraxenMeta.setExcludedFromCommands(mergedSection.getBoolean("excludeFromCommands", false));
        applyArmorStandModelProperties(mergedSection);
    }

    private void applyArmorStandModelProperties(ConfigurationSection section) {
        oraxenMeta.setArmorStandHeadScale(null);
        ConfigurationSection mechanicsSection = OraxenYaml.getConfigurationSection(section, "Mechanics");
        if (mechanicsSection == null) return;
        ConfigurationSection furnitureSection = OraxenYaml.getConfigurationSection(mechanicsSection, "furniture");
        if (furnitureSection == null) return;
        FurnitureMechanic.FurnitureType furnitureType = furnitureSection.isSet("type")
                ? FurnitureMechanic.FurnitureType.getType(furnitureSection.getString("type", FurnitureMechanic.FurnitureType.DISPLAY_ENTITY.name()))
                : FurnitureFactory.defaultFurnitureType;
        if (furnitureType != FurnitureMechanic.FurnitureType.ARMOR_STAND) return;
        ConfigurationSection armorStandSection = OraxenYaml.getConfigurationSection(furnitureSection, "armor_stand_properties");
        if (armorStandSection == null)
            armorStandSection = OraxenYaml.getConfigurationSection(furnitureSection, "display_entity_properties");
        if (armorStandSection == null) return;
        ArmorStandProperties properties = new ArmorStandProperties(armorStandSection);
        if (properties.hasScale())
            oraxenMeta.setArmorStandHeadScale(properties.getScale());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void parseVanillaSections(final ItemBuilder item, final ConfigurationSection section) {
        if (section.contains("ItemFlags")) {
            final List<String> itemFlags = section.getStringList("ItemFlags");
            for (final String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }
        if (section.contains("PotionEffects")) {
            final List<LinkedHashMap<String, Object>> potionEffects = (List<LinkedHashMap<String, Object>>) section.getList("PotionEffects");
            if (potionEffects == null) return;
            for (final Map<String, Object> serializedPotionEffect : potionEffects) {
                final PotionEffectType effect = PotionUtils.getEffectType((String) serializedPotionEffect.getOrDefault("type", ""));
                if (effect == null) return;
                final int duration = (int) serializedPotionEffect.getOrDefault("duration", 60);
                final int amplifier = (int) serializedPotionEffect.getOrDefault("amplifier", 0);
                final boolean ambient = (boolean) serializedPotionEffect.getOrDefault("ambient", true);
                final boolean particles = (boolean) serializedPotionEffect.getOrDefault("particles", true);
                final boolean icon = (boolean) serializedPotionEffect.getOrDefault("icon", true);
                item.addPotionEffect(new PotionEffect(effect, duration, amplifier, ambient, particles, icon));
            }
        }
        if (section.contains("PersistentData")) {
            try {
                final List<LinkedHashMap<String, Object>> dataHolder = (List<LinkedHashMap<String, Object>>) section.getList("PersistentData");
                for (final LinkedHashMap<String, Object> attributeJson : dataHolder) {
                    final String[] keyContent = ((String) attributeJson.get("key")).split(":");
                    final Object persistentDataType = PersistentDataType.class.getDeclaredField((String) attributeJson.get("type")).get(null);
                    item.addCustomTag(new NamespacedKey(keyContent[0], keyContent[1]),
                            (PersistentDataType<Object, Object>) persistentDataType, attributeJson.get("value"));
                }
            } catch (final IllegalAccessException | NoSuchFieldException e) {
                Logs.logWarning("Error parsing CustomTags in " + section.getName());
                Logs.debug(e);
            }
        }
        parseAttributeModifiers(item, section);
        if (section.contains("Enchantments")) {
            final ConfigurationSection enchantSection = OraxenYaml.getConfigurationSection(section, "Enchantments");
            if (enchantSection == null) return;
            for (final String enchant : enchantSection.getKeys(false)) {
                final int level = enchantSection.getInt(enchant, 1);
                final NamespacedKey namespacedKey = NamespacedKey.fromString(enchant);
                if (namespacedKey == null) {
                    Logs.logWarning("Invalid enchantment key: " + enchant + " in item: " + section.getName());
                    continue;
                }
                final Enchantment enchantment = EnchantmentWrapper.getByKey(namespacedKey);
                if (enchantment == null)
                    Logs.logWarning("Enchantment not found for key: " + enchant + " in item: " + section.getName());
                else
                    item.addEnchant(enchantment, level);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseAttributeModifiers(final ItemBuilder item, final ConfigurationSection section) {
        final ConfigurationSection attrSection = OraxenYaml.getConfigurationSection(section, "AttributeModifiers");
        if (attrSection != null) {
            if (!VersionUtil.atOrAbove("1.20.5")) {
                Logs.logWarning("Modern AttributeModifiers config format requires server 1.20.5+, skipping for item: " + section.getName());
                return;
            }
            for (final String key : attrSection.getKeys(false)) {
                final ConfigurationSection modifierSection = OraxenYaml.getConfigurationSection(attrSection, key);
                if (modifierSection == null) continue;
                try {
                    final AttributeModifierEntry entry = AttributeModifierEntry.fromConfigSection(section.getName(), key, modifierSection);
                    if (entry != null) item.addAttributeEntry(entry);
                    else Logs.logWarning("Invalid attribute modifier '" + key + "' in item: " + section.getName());
                } catch (final NoClassDefFoundError | NoSuchMethodError linkageError) {
                    Logs.logWarning("Modern attribute API unavailable for '" + key + "' in " + section.getName() + ", skipping");
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing AttributeModifier '" + key + "' in " + section.getName());
                    Logs.debug(e);
                }
            }
            return;
        }
        final List<Map<String, Object>> attributes = (List<Map<String, Object>>) section.getList("AttributeModifiers");
        if (attributes != null) {
            for (final Map<String, Object> attributeJson : attributes) {
                try {
                    attributeJson.putIfAbsent("uuid", UUID.randomUUID().toString());
                    attributeJson.putIfAbsent("name", "oraxen:modifier");
                    attributeJson.putIfAbsent("key", "oraxen:modifier");
                    final AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                    final Attribute attribute = AttributeWrapper.fromString((String) attributeJson.get("attribute"));
                    if (attribute != null) item.addAttributeModifiers(attribute, attributeModifier);
                    else Logs.logWarning("Attribute not found for key: " + attributeJson.get("attribute") + " in item: " + section.getName());
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing AttributeModifiers in " + section.getName());
                    Logs.debug(e);
                }
            }
        }
    }

    public void applyAppearance(final ItemBuilder item) {
        if (VersionUtil.atOrAbove("1.21.4")) {
            if (AppearanceMode.isItemPropertiesEnabled()) applyItemModelComponent(item);
            if (AppearanceMode.isModelDataIdsEnabled()) applyModelDataIds(item);
            if (AppearanceMode.isModelDataFloatEnabled()) {
                applyModelDataFloat(item);
                applyLegacyCustomModelData(item);
            }
        } else applyLegacyCustomModelData(item);
    }

    private void applyItemModelComponent(ItemBuilder item) {
        if (!oraxenMeta.hasPackInfos() || oraxenMeta.isExcludedFromItemModel() || item.hasItemModel()) return;
        item.setItemModel(new NamespacedKey(OraxenPlugin.get(), section.getName()));
    }

    private void applyModelDataIds(ItemBuilder item) {
        if (oraxenMeta.isExcludedFromPredicates()) return;
        final String itemId = section != null ? section.getName() : null;
        if (itemId == null || itemId.isBlank()) return;
        item.setCustomModelDataStrings(List.of(new NamespacedKey(OraxenPlugin.get(), itemId).toString()));
    }

    private void applyModelDataFloat(ItemBuilder item) {
        if (oraxenMeta.isExcludedFromPredicates()) return;
        final Integer customModelData = resolveCustomModelData();
        if (customModelData != null) {
            item.setCustomModelDataFloats(List.of((float) customModelData));
            oraxenMeta.setCustomModelData(customModelData);
        }
    }

    private void applyLegacyCustomModelData(ItemBuilder item) {
        if (oraxenMeta.isExcludedFromPredicates()) return;
        final Integer customModelData = resolveCustomModelData();
        if (customModelData != null) {
            item.setCustomModelData(customModelData);
            oraxenMeta.setCustomModelData(customModelData);
        }
    }

    private Integer resolveCustomModelData() {
        final ModelData modelData = modelDatasById.get(section.getName());
        if (modelData != null)
            return modelData.getModelData();

        if (!oraxenMeta.hasPackInfos())
            return null;

        final Integer customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
        if (migrator != null)
            migrator.markConfigUpdated();

        if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool()) {
            Optional.ofNullable(OraxenYaml.getConfigurationSection(section, "Pack"))
                    .ifPresent(packSection -> {
                        packSection.set("custom_model_data", customModelData);
                        OraxenYaml.invalidateKeyCache(packSection);
                    });
        }
        return customModelData;
    }
}
