package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.helpers.FoodComponentWrapper;
import io.th0rgal.oraxen.items.helpers.ItemRarityWrapper;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class ItemParser {

    public static final Map<String, ModelData> MODEL_DATAS_BY_ID = new HashMap<>();

    private final OraxenMeta oraxenMeta;
    private final ConfigurationSection section;
    private final Material type;
    private WrappedMMOItem mmoItem;
    private WrappedCrucibleItem crucibleItem;
    private WrappedEcoItem ecoItem;
    private ItemParser templateItem;
    private boolean configUpdated = false;

    public ItemParser(ConfigurationSection section) {
        this.section = section;

        if (section.isString("template")) templateItem = ItemTemplate.getParserTemplate(section.getString("template"));

        ConfigurationSection crucibleSection = section.getConfigurationSection("crucible");
        ConfigurationSection mmoSection = section.getConfigurationSection("mmoitem");
        ConfigurationSection ecoItemSection = section.getConfigurationSection("ecoitem");
        if (crucibleSection != null) crucibleItem = new WrappedCrucibleItem(crucibleSection);
        else if (section.isString("crucible_id")) crucibleItem = new WrappedCrucibleItem(section.getString("crucible_id"));
        else if (ecoItemSection != null) ecoItem = new WrappedEcoItem(ecoItemSection);
        else if (section.isString("ecoitem_id")) ecoItem = new WrappedEcoItem(section.getString("ecoitem_id"));
        else if (mmoSection != null) mmoItem = new WrappedMMOItem(mmoSection);

        Material material = Material.getMaterial(section.getString("material", ""));
        if (material == null) material = usesTemplate() ? templateItem.type : Material.PAPER;
        type = material;

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
        return crucibleItem == null && ecoItem == null  && mmoItem != null && mmoItem.build() != null;
    }

    public boolean usesCrucibleItems() {
        return mmoItem == null && ecoItem == null && crucibleItem != null && crucibleItem.build() != null;
    }

    public boolean usesEcoItems() {
        return mmoItem == null && crucibleItem == null && ecoItem != null && ecoItem.build() != null;
    }

    public boolean usesTemplate() {
        return templateItem != null;
    }

    @Nullable
    public static String parseComponentItemName(@Nullable String miniString) {
        if (miniString == null) return null;
        if (miniString.isEmpty()) return miniString;
        Component component = AdventureUtils.MINI_MESSAGE.deserialize(miniString);
        // If it has no formatting, set color to WHITE to prevent Italic
        return AdventureUtils.LEGACY_SERIALIZER.serialize(component.colorIfAbsent(NamedTextColor.WHITE));
    }

    public static String parseComponentLore(String miniString) {
        Component component = AdventureUtils.MINI_MESSAGE.deserialize(miniString);
        // If it has no formatting, set color to WHITE to prevent Italic
        return AdventureUtils.LEGACY_SERIALIZER.serialize(component);
    }

    public ItemBuilder buildItem() {
        ItemBuilder item;

        if (usesCrucibleItems()) item = new ItemBuilder(crucibleItem);
        else if (usesMMOItems()) item = new ItemBuilder(mmoItem);
        else if (usesEcoItems()) item = new ItemBuilder(ecoItem);
        else item = new ItemBuilder(type);

        // If item has a template, apply the template ontop of the builder made above
        return applyConfig(usesTemplate() ? templateItem.applyConfig(item) : item);
    }

    private ItemBuilder applyConfig(ItemBuilder item) {
        if (!VersionUtil.atOrAbove("1.20.5"))
            item.setDisplayName(parseComponentItemName(section.getString("displayname", "")));

        //if (section.contains("type")) item.setType(Material.getMaterial(section.getString("type", "PAPER")));
        if (section.contains("lore")) item.setLore(section.getStringList("lore").stream().map(ItemParser::parseComponentLore).toList());
        if (section.contains("unbreakable")) item.setUnbreakable(section.getBoolean("unbreakable", false));
        if (section.contains("unstackable")) item.setUnstackable(section.getBoolean("unstackable", false));
        if (section.contains("color")) item.setColor(Utils.toColor(section.getString("color", "#FFFFFF")));
        if (section.contains("trim_pattern")) item.setTrimPattern(Key.key(section.getString("trim_pattern", "")));

        parse_1_20_5_Properties(item);

        parseMiscOptions(item);
        parseVanillaSections(item);
        parseOraxenSections(item);
        item.setOraxenMeta(oraxenMeta);
        return item;
    }

    private void parse_1_20_5_Properties(ItemBuilder item) {
        if (!VersionUtil.atOrAbove("1.20.5")) return;

        if (section.contains("unstackable")) item.setMaxStackSize(1);
        else if (section.contains("max_stack_size")) item.setMaxStackSize(Math.min(Math.max(section.getInt("max_stack_size"), 1), 99));
        if (item.hasMaxStackSize() && item.getMaxStackSize() == 1) item.setUnstackable(true);

        if (section.contains("itemname")) item.setItemName(parseComponentItemName(section.getString("itemname", "")));
        else item.setItemName(parseComponentItemName(section.getString("displayname", "")));

        if (section.contains("enchantment_glint_override")) item.setEnchantmentGlindOverride(section.getBoolean("enchantment_glint_override"));
        if (section.contains("durability")) {
            item.setDamagedOnBlockBreak(section.getBoolean("durability.damage_block_break"));
            item.setDamagedOnEntityHit(section.getBoolean("durability.damage_entity_hit"));
            item.setDurability(Math.max(section.getInt("durability.value"), section.getInt("durability", 1)));
        }
        if (section.contains("rarity")) item.setRarity(Arrays.stream(ItemRarityWrapper.values()).filter(r -> r.name().equalsIgnoreCase(section.getString("rarity"))).findFirst().orElse(null));
        item.setFireResistant(section.getBoolean("fire_resistant"));
        item.setHideToolTips(section.getBoolean("hide_tooltips"));

        ConfigurationSection foodSection = section.getConfigurationSection("food");
        if (foodSection != null) {
            FoodComponentWrapper foodComponent = new FoodComponentWrapper();
            foodComponent.setNutrition(foodSection.getInt("nutrition"));
            foodComponent.setSaturation((float) foodSection.getDouble("saturation", 0.0));
            foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat"));
            foodComponent.setEatSeconds((float) foodSection.getDouble("eat_seconds", 1.6));

            ConfigurationSection effectsSection = foodSection.getConfigurationSection("effects");
            if (effectsSection != null) for (String effect : effectsSection.getKeys(false)) {
                PotionEffectType effectType = PotionUtils.getEffectType(effect);
                if (effectType == null)
                    Logs.logError("Invalid potion effect: " + effect + ", in " + StringUtils.substringBefore(effectsSection.getCurrentPath(), ".") + " food-property!");
                else {
                    foodComponent.addEffect(
                            new PotionEffect(effectType,
                                    foodSection.getInt("duration", 1) * 20,
                                    foodSection.getInt("amplifier", 0),
                                    foodSection.getBoolean("ambient", true),
                                    foodSection.getBoolean("show_particles", true),
                                    foodSection.getBoolean("show_icon", true)),
                            (float) foodSection.getDouble("probability", 1.0)
                    );
                }
            }
            item.setFoodComponent(foodComponent);
        }
    }

    private void parseMiscOptions(ItemBuilder item) {
        oraxenMeta.setNoUpdate(section.getBoolean("no_auto_update", false));
        oraxenMeta.setDisableEnchanting(section.getBoolean("disable_enchanting", false));
        oraxenMeta.setExcludedFromInventory(section.getBoolean("excludeFromInventory", false));
        oraxenMeta.setExcludedFromCommands(section.getBoolean("excludeFromCommands", false));

        if (section.getBoolean("injectId", true))
            item.setCustomTag(OraxenItems.ITEM_ID, PersistentDataType.STRING, section.getName());
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
                PotionEffectType effect = PotionUtils.getEffectType((String) serializedPotionEffect.getOrDefault("type", ""));
                if (effect == null) return;
                int duration = (int) serializedPotionEffect.getOrDefault("duration", 60);
                int amplifier = (int) serializedPotionEffect.getOrDefault("amplifier", 0);
                boolean ambient = (boolean) serializedPotionEffect.getOrDefault("ambient", true);
                boolean particles = (boolean) serializedPotionEffect.getOrDefault("particles", true);
                boolean icon = (boolean) serializedPotionEffect.getOrDefault("icon", true);
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
                Mechanic mechanic = factory.parse(mechanicSection);
                if (mechanic == null) continue;
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
