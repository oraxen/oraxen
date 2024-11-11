package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.tag.DamageTypeTags;
import org.jetbrains.annotations.NotNull;

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

        if (section.isString("template"))
            templateItem = ItemTemplate.getParserTemplate(section.getString("template"));

        ConfigurationSection crucibleSection = section.getConfigurationSection("crucible");
        ConfigurationSection mmoSection = section.getConfigurationSection("mmoitem");
        ConfigurationSection ecoItemSection = section.getConfigurationSection("ecoitem");
        if (crucibleSection != null)
            crucibleItem = new WrappedCrucibleItem(crucibleSection);
        else if (section.isString("crucible_id"))
            crucibleItem = new WrappedCrucibleItem(section.getString("crucible_id"));
        else if (ecoItemSection != null)
            ecoItem = new WrappedEcoItem(ecoItemSection);
        else if (section.isString("ecoitem_id"))
            ecoItem = new WrappedEcoItem(section.getString("ecoitem_id"));
        else if (mmoSection != null)
            mmoItem = new WrappedMMOItem(mmoSection);

        Material material = Material.getMaterial(section.getString("material", ""));
        if (material == null)
            material = usesTemplate() ? templateItem.type : Material.PAPER;
        type = material;

        oraxenMeta = templateItem != null ? templateItem.oraxenMeta : new OraxenMeta();
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
        return crucibleItem == null && ecoItem == null && mmoItem != null && mmoItem.build() != null;
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

    public ItemBuilder buildItem() {
        ItemBuilder item;

        if (usesCrucibleItems())
            item = new ItemBuilder(crucibleItem);
        else if (usesMMOItems())
            item = new ItemBuilder(mmoItem);
        else if (usesEcoItems())
            item = new ItemBuilder(ecoItem);
        else
            item = new ItemBuilder(type);

        // If item has a template, apply the template ontop of the builder made above
        return applyConfig(usesTemplate() ? templateItem.applyConfig(item) : item);
    }

    private ItemBuilder applyConfig(ItemBuilder item) {
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

        parseDataComponents(item);
        parseMiscOptions(item);
        parseVanillaSections(item);
        parseOraxenSections(item);
        item.setOraxenMeta(oraxenMeta);
        return item;
    }

    private void parseDataComponents(ItemBuilder item) {
        ConfigurationSection section = mergeWithTemplateSection();
        if (section.contains("itemname") && VersionUtil.atOrAbove("1.20.5"))
            item.setItemName(section.getString("itemname"));
        else if (section.contains("displayname"))
            item.setItemName(section.getString("displayname"));

        ConfigurationSection components = section.getConfigurationSection("Components");
        if (components == null || !VersionUtil.atOrAbove("1.20.5"))
            return;

        if (components.contains("max_stack_size"))
            item.setMaxStackSize(Math.clamp(components.getInt("max_stack_size"), 1, 99));

        if (components.contains("enchantment_glint_override"))
            item.setEnchantmentGlindOverride(components.getBoolean("enchantment_glint_override"));
        if (components.contains("durability")) {
            item.setDamagedOnBlockBreak(components.getBoolean("durability.damage_block_break"));
            item.setDamagedOnEntityHit(components.getBoolean("durability.damage_entity_hit"));
            item.setDurability(Math.max(components.getInt("durability.value"), components.getInt("durability", 1)));
        }
        if (components.contains("rarity"))
            item.setRarity(ItemRarity.valueOf(components.getString("rarity")));
        if (components.contains("fire_resistant"))
            item.setFireResistant(components.getBoolean("fire_resistant"));
        if (components.contains("hide_tooltip"))
            item.setHideToolTip(components.getBoolean("hide_tooltip"));

        ConfigurationSection foodSection = components.getConfigurationSection("food");
        if (foodSection != null)
            parseFoodComponent(item, foodSection);
        ConfigurationSection toolSection = components.getConfigurationSection("tool");
        if (toolSection != null)
            parseToolComponent(item, toolSection);

        if (!VersionUtil.atOrAbove("1.21"))
            return;

        ConfigurationSection jukeboxSection = components.getConfigurationSection("jukebox_playable");
        if (jukeboxSection != null) {
            JukeboxPlayableComponent jukeboxPlayable = new ItemStack(Material.MUSIC_DISC_CREATOR).getItemMeta()
                    .getJukeboxPlayable();
            jukeboxPlayable.setShowInTooltip(jukeboxSection.getBoolean("show_in_tooltip"));
            jukeboxPlayable.setSongKey(NamespacedKey.fromString(jukeboxSection.getString("song_key", "")));
            item.setJukeboxPlayable(jukeboxPlayable);
        }

        if (!VersionUtil.atOrAbove("1.21.2"))
            return;
        Optional.ofNullable(components.getConfigurationSection("equippable"))
                .ifPresent(equippable -> parseEquippableComponent(item, equippable));

        Optional.ofNullable(components.getConfigurationSection("use_cooldown")).ifPresent((cooldownSection) -> {
            UseCooldownComponent useCooldownComponent = new ItemStack(Material.PAPER).getItemMeta().getUseCooldown();
            String group = Optional.ofNullable(cooldownSection.getString("group"))
                    .orElse("oraxen:" + OraxenItems.getIdByItem(item));
            if (!group.isEmpty())
                useCooldownComponent.setCooldownGroup(NamespacedKey.fromString(group));
            useCooldownComponent.setCooldownSeconds((float) Math.max(cooldownSection.getDouble("seconds", 1.0), 0f));
            item.setUseCooldownComponent(useCooldownComponent);
        });

        Optional.ofNullable(components.getConfigurationSection("use_remainder"))
                .ifPresent(useRemainder -> parseUseRemainderComponent(item, useRemainder));
        Optional.ofNullable(components.getString("damage_resistant")).map(NamespacedKey::fromString)
                .ifPresent(damageResistantKey -> item.setDamageResistant(
                        Bukkit.getTag(DamageTypeTags.REGISTRY_DAMAGE_TYPES, damageResistantKey, DamageType.class)));

        Optional.ofNullable(components.getString("tooltip_style")).map(NamespacedKey::fromString)
                .ifPresent(item::setTooltipStyle);

        Optional.ofNullable(components.getString("item_model")).map(NamespacedKey::fromString)
                .ifPresentOrElse(item::setItemModel, () -> {
                    if (oraxenMeta == null || !oraxenMeta.hasPackInfos())
                        return;
                    if (oraxenMeta.getCustomModelData() != null)
                        return;
                    Optional.ofNullable(components.getString("item_model")).map(NamespacedKey::fromString)
                            .ifPresent(item::setItemModel);
                });

        if (components.contains("enchantable"))
            item.setEnchantable(components.getInt("enchantable"));
        if (components.contains("glider"))
            item.setGlider(components.getBoolean("glider"));

    }

    private void parseUseRemainderComponent(ItemBuilder item, @NotNull ConfigurationSection useRemainderSection) {
        ItemStack result;
        int amount = useRemainderSection.getInt("amount", 1);

        if (useRemainderSection.contains("oraxen_item"))
            result = ItemUpdater
                    .updateItem(OraxenItems.getItemById(useRemainderSection.getString("oraxen_item")).build());
        else if (useRemainderSection.contains("crucible_item"))
            result = new WrappedCrucibleItem(useRemainderSection.getString("crucible_item")).build();
        else if (useRemainderSection.contains("mmoitems_id") && useRemainderSection.isString("mmoitems_type"))
            result = MMOItems.plugin.getItem(useRemainderSection.getString("mmoitems_type"),
                    useRemainderSection.getString("mmoitems_id"));
        else if (useRemainderSection.contains("ecoitem_id"))
            result = new WrappedEcoItem(useRemainderSection.getString("ecoitem_id")).build();
        else if (useRemainderSection.contains("minecraft_type")) {
            Material material = Material.getMaterial(useRemainderSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir())
                return;
            result = new ItemStack(material);
        } else
            result = useRemainderSection.getItemStack("minecraft_item");

        if (result != null)
            result.setAmount(amount);
        item.setUseRemainder(result);
    }

    @SuppressWarnings({ "UnstableApiUsage", "unchecked" })
    private void parseToolComponent(ItemBuilder item, @NotNull ConfigurationSection toolSection) {
        ToolComponent toolComponent = new ItemStack(type).getItemMeta().getTool();
        toolComponent.setDamagePerBlock(Math.max(toolSection.getInt("damage_per_block", 1), 0));
        toolComponent.setDefaultMiningSpeed(Math.max((float) toolSection.getDouble("default_mining_speed", 1.0), 0f));

        for (Map<?, ?> ruleEntry : toolSection.getMapList("rules")) {
            float speed = NumberUtils.toFloat(String.valueOf(ruleEntry.get("speed")), 1f);
            boolean correctForDrops = Boolean.parseBoolean(String.valueOf(ruleEntry.get("correct_for_drops")));
            Set<Material> materials = new HashSet<>();
            Set<Tag<Material>> tags = new HashSet<>();

            if (ruleEntry.containsKey("material")) {
                try {
                    Material material = Material.valueOf(String.valueOf(ruleEntry.get("material")));
                    if (material.isBlock())
                        materials.add(material);
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("materials")) {
                try {
                    List<String> materialIds = (List<String>) ruleEntry.get("materials");
                    for (String materialId : materialIds) {
                        Material material = Material.valueOf(materialId);
                        if (material.isBlock())
                            materials.add(material);
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"materials\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tag")) {
                try {
                    NamespacedKey tagKey = NamespacedKey.fromString(String.valueOf(ruleEntry.get("tag")));
                    if (tagKey != null)
                        tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"tag\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tags")) {
                try {
                    for (String tagString : (List<String>) ruleEntry.get("tags")) {
                        NamespacedKey tagKey = NamespacedKey.fromString(tagString);
                        if (tagKey != null)
                            tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                }
            }

            if (!materials.isEmpty())
                toolComponent.addRule(materials, speed, correctForDrops);
            for (Tag<Material> tag : tags)
                toolComponent.addRule(tag, speed, correctForDrops);
        }

        item.setToolComponent(toolComponent);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void parseFoodComponent(ItemBuilder item, @NotNull ConfigurationSection foodSection) {
        FoodComponent foodComponent = new ItemStack(type).getItemMeta().getFood();
        foodComponent.setNutrition(foodSection.getInt("nutrition"));
        foodComponent.setSaturation((float) foodSection.getDouble("saturation", 0.0));
        foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat"));

        if (!VersionUtil.atOrAbove("1.21.2")) {
            /*
             * foodComponent.setEatSeconds((float) foodSection.getDouble("eat_seconds",
             * 1.6));
             * 
             * ConfigurationSection effectsSection =
             * foodSection.getConfigurationSection("effects");
             * if (effectsSection != null) for (String effect :
             * effectsSection.getKeys(false)) {
             * ConfigurationSection effectSection =
             * effectsSection.getConfigurationSection(effect);
             * PotionEffectType effectType = PotionUtils.getEffectType(effect);
             * if (effectSection == null || effectType == null)
             * Logs.logError("Invalid potion effect: " + effect + ", in " +
             * StringUtils.substringBefore(effectsSection.getCurrentPath(), ".") +
             * " food-property!");
             * else {
             * foodComponent.addEffect(
             * new PotionEffect(effectType,
             * effectSection.getInt("duration", 1) * 20,
             * effectSection.getInt("amplifier", 0),
             * effectSection.getBoolean("ambient", true),
             * effectSection.getBoolean("show_particles", true),
             * effectSection.getBoolean("show_icon", true)),
             * (float) effectSection.getDouble("probability", 1.0)
             * );
             * }
             * }
             */
        }

        item.setFoodComponent(foodComponent);
    }

    private void parseConsumableComponent(ItemBuilder item, @NotNull ConfigurationSection consumableSection) {
    }

    private void parseEquippableComponent(ItemBuilder item, ConfigurationSection equippableSection) {
        EquippableComponent equippableComponent = new ItemStack(type).getItemMeta().getEquippable();

        String slot = equippableSection.getString("slot");
        try {
            equippableComponent.setSlot(EquipmentSlot.valueOf(slot));
        } catch (Exception e) {
            Logs.logWarning("Error parsing equippable-component in %s...".formatted(section.getName()));
            Logs.logWarning("Invalid \"slot\"-value %s".formatted(slot));
            Logs.logWarning("Valid values are: %s".formatted(StringUtils.join(EquipmentSlot.values())));
            return;
        }

        List<EntityType> entityTypes = equippableSection.getStringList("allowed_entity_types").stream()
                .map(e -> EnumUtils.getEnum(EntityType.class, e)).toList();
        if (equippableSection.contains("allowed_entity_types"))
            equippableComponent.setAllowedEntities(entityTypes.isEmpty() ? null : entityTypes);
        if (equippableSection.contains("damage_on_hurt"))
            equippableComponent.setDamageOnHurt(equippableSection.getBoolean("damage_on_hurt", true));
        if (equippableSection.contains("dispensable"))
            equippableComponent.setDispensable(equippableSection.getBoolean("dispensable", true));
        if (equippableSection.contains("swappable"))
            equippableComponent.setSwappable(equippableSection.getBoolean("swappable", true));

        Optional.ofNullable(equippableSection.getString("model", null)).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setModel);
        Optional.ofNullable(equippableSection.getString("camera_overlay")).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setCameraOverlay);
        Optional.ofNullable(equippableSection.getString("equip_sound"))
                .map(Key::key)
                // tempfix for spigot compat
                .map(key -> Sound.valueOf(key.asString().toUpperCase()))
                .ifPresent(equippableComponent::setEquipSound);

        item.setEquippableComponent(equippableComponent);
    }

    private void parseMiscOptions(ItemBuilder item) {
        ConfigurationSection section = mergeWithTemplateSection();
        oraxenMeta.setNoUpdate(section.getBoolean("no_auto_update", false));
        oraxenMeta.setDisableEnchanting(section.getBoolean("disable_enchanting", false));
        oraxenMeta.setExcludedFromInventory(section.getBoolean("excludeFromInventory", false));
        oraxenMeta.setExcludedFromCommands(section.getBoolean("excludeFromCommands", false));

        if (section.getBoolean("injectId", true))
            item.setCustomTag(OraxenItems.ITEM_ID, PersistentDataType.STRING, section.getName());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void parseVanillaSections(ItemBuilder item) {
        ConfigurationSection section = mergeWithTemplateSection();
        if (section.contains("ItemFlags")) {
            List<String> itemFlags = section.getStringList("ItemFlags");
            for (String itemFlag : itemFlags)
                item.addItemFlags(ItemFlag.valueOf(itemFlag));
        }

        if (section.contains("PotionEffects")) {
            @SuppressWarnings("unchecked") // because this sections must always return a List<LinkedHashMap<String, ?>>
            List<LinkedHashMap<String, Object>> potionEffects = (List<LinkedHashMap<String, Object>>) section
                    .getList("PotionEffects");
            if (potionEffects == null)
                return;
            for (Map<String, Object> serializedPotionEffect : potionEffects) {
                PotionEffectType effect = PotionUtils
                        .getEffectType((String) serializedPotionEffect.getOrDefault("type", ""));
                if (effect == null)
                    return;
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
            List<LinkedHashMap<String, Object>> attributes = (List<LinkedHashMap<String, Object>>) section
                    .getList("AttributeModifiers");
            if (attributes != null)
                for (LinkedHashMap<String, Object> attributeJson : attributes) {
                    attributeJson.putIfAbsent("uuid", UUID.randomUUID().toString());
                    attributeJson.putIfAbsent("name", "oraxen:modifier");
                    attributeJson.putIfAbsent("key", "oraxen:modifier");
                    AttributeModifier attributeModifier = AttributeModifier.deserialize(attributeJson);
                    Attribute attribute = Attribute.valueOf((String) attributeJson.get("attribute"));
                    item.addAttributeModifiers(attribute, attributeModifier);
                }
        }

        if (section.contains("Enchantments")) {
            ConfigurationSection enchantSection = section.getConfigurationSection("Enchantments");
            if (enchantSection != null)
                for (String enchant : enchantSection.getKeys(false))
                    item.addEnchant(EnchantmentWrapper.getByKey(NamespacedKey.minecraft(enchant)),
                            enchantSection.getInt(enchant));
        }
    }

    private void parseOraxenSections(ItemBuilder item) {
        ConfigurationSection merged = mergeWithTemplateSection();
        ConfigurationSection mechanicsSection = merged.getConfigurationSection("Mechanics");
        if (mechanicsSection != null)
            for (String mechanicID : mechanicsSection.getKeys(false)) {
                MechanicFactory factory = MechanicsManager.getMechanicFactory(mechanicID);

                if (factory != null) {
                    ConfigurationSection mechanicSection = mechanicsSection.getConfigurationSection(mechanicID);
                    if (mechanicSection == null)
                        continue;
                    Mechanic mechanic = factory.parse(mechanicSection);
                    if (mechanic == null)
                        continue;
                    // Apply item modifiers
                    for (Function<ItemBuilder, ItemBuilder> itemModifier : mechanic.getItemModifiers())
                        item = itemModifier.apply(item);
                }
            }

        if (oraxenMeta.hasPackInfos()) {
            Integer customModelData;
            if (MODEL_DATAS_BY_ID.containsKey(section.getName())) {
                customModelData = MODEL_DATAS_BY_ID.get(section.getName()).getModelData();
            } else if (!item.hasItemModel()) {
                customModelData = ModelData.generateId(oraxenMeta.getModelName(), type);
                configUpdated = true;
                if (!Settings.DISABLE_AUTOMATIC_MODEL_DATA.toBool())
                    Optional.ofNullable(section.getConfigurationSection("Pack"))
                            .ifPresent(c -> c.set("custom_model_data", customModelData));
            } else
                customModelData = null;

            if (customModelData != null) {
                item.setCustomModelData(customModelData);
                oraxenMeta.setCustomModelData(customModelData);
            }
        }
    }

    private ConfigurationSection mergeWithTemplateSection() {
        if (section == null || templateItem == null || templateItem.section == null)
            return section;

        ConfigurationSection merged = new YamlConfiguration().createSection(section.getName());
        OraxenYaml.copyConfigurationSection(templateItem.section, merged);
        OraxenYaml.copyConfigurationSection(section, merged);

        return merged;
    }

    public boolean isConfigUpdated() {
        return configUpdated;
    }

}
