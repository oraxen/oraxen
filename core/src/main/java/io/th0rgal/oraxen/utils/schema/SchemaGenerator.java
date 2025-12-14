package io.th0rgal.oraxen.utils.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.MechanicConfigProperty;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Generates JSON schema for Oraxen Studio by extracting enum values
 * from Bukkit API. Requires a running Bukkit server for full registry access.
 * Use via /oraxen schema command or enable debug mode for auto-generation.
 */
public class SchemaGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Generate schema and save to plugin data folder.
     * Called during plugin enable or via command.
     *
     * @return true if schema was generated successfully, false on I/O error
     */
    public static boolean generateAndSave() {
        try {
            String version = OraxenPlugin.get().getDescription().getVersion();
            JsonObject schema = generateSchema(version);

            File outputFile = new File(OraxenPlugin.get().getDataFolder(), "oraxen-schema.json");
            Files.writeString(outputFile.toPath(), GSON.toJson(schema));
            Logs.logSuccess("Schema generated: " + outputFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Logs.logError("Failed to write schema: " + e.getMessage());
            return false;
        }
    }

    public static JsonObject generateSchema(String version) {
        JsonObject schema = new JsonObject();

        // Version info
        schema.addProperty("oraxenVersion", version);
        schema.addProperty("minecraftVersion", Bukkit.getMinecraftVersion());
        schema.addProperty("serverVersion", Bukkit.getBukkitVersion());
        schema.addProperty("generatedAt", Instant.now().toString());

        // Add all sections
        schema.add("enums", generateEnums());
        schema.add("itemProperties", generateItemProperties());
        schema.add("components", generateComponents());
        schema.add("mechanics", generateMechanics());
        schema.add("pack", generatePackSchema());

        return schema;
    }

    private static JsonObject generateEnums() {
        JsonObject enums = new JsonObject();

        enums.add("Material", generateMaterialEnum());
        enums.add("Attribute", generateAttributeEnum());
        enums.add("AttributeOperation", generateAttributeOperationEnum());
        enums.add("PotionEffectType", generatePotionEffectTypeEnum());
        enums.add("Enchantment", generateEnchantmentEnum());
        enums.add("ItemFlag", generateSimpleEnum(ItemFlag.class));
        enums.add("EquipmentSlot", generateSimpleEnum(EquipmentSlot.class));
        enums.add("EntityType", generateEntityTypeEnum());
        enums.add("Particle", generateParticleEnum());
        enums.add("Sound", generateSoundEnum());
        enums.add("BlockTag", generateBlockTags());

        return enums;
    }

    private static JsonObject generateMaterialEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject categories = new JsonObject();

        JsonArray items = new JsonArray();
        JsonArray tools = new JsonArray();
        JsonArray armor = new JsonArray();
        JsonArray blocks = new JsonArray();
        JsonArray food = new JsonArray();
        JsonArray weapons = new JsonArray();

        for (Material material : Material.values()) {
            if (material.isLegacy())
                continue;

            String name = material.name();
            values.add(name);

            // Categorize using actual API methods
            if (material.isBlock())
                blocks.add(name);
            if (material.isItem())
                items.add(name);
            if (material.isEdible())
                food.add(name);

            // Pattern-based categorization for tools/armor/weapons
            if (name.contains("PICKAXE") || name.contains("_AXE") || name.contains("SHOVEL") ||
                    name.contains("HOE") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL")) {
                tools.add(name);
            }
            if (name.contains("HELMET") || name.contains("CHESTPLATE") ||
                    name.contains("LEGGINGS") || name.contains("BOOTS") || name.equals("ELYTRA")) {
                armor.add(name);
            }
            if (name.contains("SWORD") || name.equals("BOW") || name.equals("CROSSBOW") ||
                    name.equals("TRIDENT") || name.equals("MACE")) {
                weapons.add(name);
            }
        }

        result.add("values", values);
        categories.add("items", items);
        categories.add("blocks", blocks);
        categories.add("tools", tools);
        categories.add("armor", armor);
        categories.add("weapons", weapons);
        categories.add("food", food);
        result.add("categories", categories);

        return result;
    }

    private static JsonObject generateAttributeEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject descriptions = new JsonObject();

        // Paper 1.21.1 uses different Bukkit API types for some registries
        // (e.g. Attribute/Sound/Enchantment/PotionEffectType are classes/enums in
        // 1.21.1 but interfaces in later versions). If we compile against the newer
        // API and call interface methods directly, 1.21.1 can throw
        // IncompatibleClassChangeError.
        //
        // Iterate registries as raw Iterables and use reflection for getKey().
        forEachRegistryEntry(Registry.ATTRIBUTE, entry -> {
            NamespacedKey key = reflectNamespacedKey(entry);
            if (key == null)
                return;

            String keyString = key.getKey();
            values.add(keyString);

            String desc = getAttributeDescription(keyString);
            if (desc != null)
                descriptions.addProperty(keyString, desc);
        });

        result.add("values", values);
        result.add("descriptions", descriptions);
        return result;
    }

    private static String getAttributeDescription(String key) {
        return switch (key) {
            case "generic.max_health", "max_health" -> "Maximum health of the entity";
            case "generic.movement_speed", "movement_speed" -> "Movement speed of the entity";
            case "generic.attack_damage", "attack_damage" -> "Attack damage dealt by the entity";
            case "generic.armor", "armor" -> "Armor points of the entity";
            case "generic.armor_toughness", "armor_toughness" -> "Armor toughness of the entity";
            case "generic.attack_speed", "attack_speed" -> "Attack speed of the entity";
            case "generic.knockback_resistance", "knockback_resistance" -> "Knockback resistance of the entity";
            case "generic.luck", "luck" -> "Luck of the entity for loot tables";
            case "generic.flying_speed", "flying_speed" -> "Flying speed of the entity";
            case "generic.follow_range", "follow_range" -> "Follow range for mobs";
            default -> null;
        };
    }

    private static JsonObject generateAttributeOperationEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject descriptions = new JsonObject();

        for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
            String name = op.name();
            values.add(name);

            String desc = switch (op) {
                case ADD_NUMBER -> "Adds to the base value";
                case ADD_SCALAR -> "Multiplies the base value (additive with other scalars)";
                case MULTIPLY_SCALAR_1 -> "Multiplies the final value";
            };
            descriptions.addProperty(name, desc);
        }

        result.add("values", values);
        result.add("descriptions", descriptions);
        return result;
    }

    private static JsonObject generatePotionEffectTypeEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();

        forEachRegistryEntry(Registry.POTION_EFFECT_TYPE, entry -> {
            NamespacedKey key = reflectNamespacedKey(entry);
            if (key != null)
                values.add(key.getKey());
        });

        result.add("values", values);
        return result;
    }

    private static JsonObject generateEnchantmentEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject maxLevels = new JsonObject();

        forEachRegistryEntry(Registry.ENCHANTMENT, entry -> {
            NamespacedKey key = reflectNamespacedKey(entry);
            if (key == null)
                return;

            String keyString = key.getKey();
            values.add(keyString);

            Integer maxLevel = reflectInt(entry, "getMaxLevel");
            if (maxLevel != null)
                maxLevels.addProperty(keyString, maxLevel);
        });

        result.add("values", values);
        result.add("maxLevels", maxLevels);
        return result;
    }

    private static JsonObject generateEntityTypeEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject categories = new JsonObject();
        JsonArray mobs = new JsonArray();

        for (EntityType type : EntityType.values()) {
            String name = type.name();
            values.add(name);

            if (type.isAlive())
                mobs.add(name);
        }

        result.add("values", values);
        categories.add("mobs", mobs);
        result.add("categories", categories);
        return result;
    }

    private static JsonObject generateParticleEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();

        for (Particle particle : Particle.values()) {
            values.add(particle.name());
        }

        result.add("values", values);
        return result;
    }

    private static JsonObject generateSoundEnum() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();
        JsonObject categories = new JsonObject();

        JsonArray armorEquip = new JsonArray();
        JsonArray eat = new JsonArray();

        forEachRegistryEntry(Registry.SOUNDS, entry -> {
            NamespacedKey key = reflectNamespacedKey(entry);
            if (key == null)
                return;

            String name = key.getKey();
            values.add(name);

            if (name.contains("armor") && name.contains("equip"))
                armorEquip.add(name);
            if (name.contains("eat") || name.contains("burp"))
                eat.add(name);
        });

        result.add("values", values);
        categories.add("armor_equip", armorEquip);
        categories.add("eat", eat);
        result.add("categories", categories);
        return result;
    }

    private static void forEachRegistryEntry(Object registry, java.util.function.Consumer<Object> consumer) {
        if (!(registry instanceof Iterable<?> iterable))
            return;
        for (Object entry : iterable) {
            if (entry != null) {
                consumer.accept(entry);
            }
        }
    }

    private static NamespacedKey reflectNamespacedKey(Object registryEntry) {
        try {
            Method getKey = registryEntry.getClass().getMethod("getKey");
            Object keyObj = getKey.invoke(registryEntry);
            return keyObj instanceof NamespacedKey nk ? nk : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer reflectInt(Object instance, String methodName) {
        try {
            Method m = instance.getClass().getMethod(methodName);
            Object v = m.invoke(instance);
            return v instanceof Number n ? n.intValue() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static <T extends Enum<?>> JsonObject generateSimpleEnum(Class<T> enumClass) {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();

        for (T constant : enumClass.getEnumConstants()) {
            values.add(constant.name());
        }

        result.add("values", values);
        return result;
    }

    private static JsonObject generateBlockTags() {
        JsonObject result = new JsonObject();
        JsonArray values = new JsonArray();

        // Get all registered block tags
        try {
            String[] commonTags = {
                    "mineable/axe", "mineable/pickaxe", "mineable/shovel", "mineable/hoe",
                    "needs_stone_tool", "needs_iron_tool", "needs_diamond_tool",
                    "logs", "planks", "wool", "leaves", "dirt", "sand", "ice",
                    "base_stone_overworld", "base_stone_nether",
                    "beacon_base_blocks", "beds", "crops", "flowers", "saplings",
                    "signs", "stairs", "slabs", "walls", "fences", "fence_gates",
                    "doors", "trapdoors", "buttons", "pressure_plates"
            };

            for (String tagName : commonTags) {
                NamespacedKey key = NamespacedKey.minecraft(tagName);
                Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
                if (tag != null) {
                    values.add("minecraft:" + tagName);
                }
            }
        } catch (Exception e) {
            // Fallback: just add common tag names
            String[] fallbackTags = {
                    "minecraft:mineable/axe", "minecraft:mineable/pickaxe",
                    "minecraft:mineable/shovel", "minecraft:mineable/hoe",
                    "minecraft:needs_stone_tool", "minecraft:needs_iron_tool",
                    "minecraft:needs_diamond_tool", "minecraft:logs", "minecraft:planks"
            };
            for (String tag : fallbackTags)
                values.add(tag);
        }

        result.add("values", values);
        return result;
    }

    private static JsonObject generateItemProperties() {
        JsonObject properties = new JsonObject();

        // itemname / displayname
        addProperty(properties, "itemname", "string", "Display name with MiniMessage formatting (1.20.5+)", true,
                "minimessage", "1.20.5+");
        addProperty(properties, "displayname", "string", "Display name (legacy, use itemname instead)", false,
                "minimessage", null);
        properties.getAsJsonObject("displayname").addProperty("deprecated", true);

        // material
        JsonObject material = new JsonObject();
        material.addProperty("type", "enum");
        material.addProperty("enum", "Material");
        material.addProperty("required", false);
        material.addProperty("default", "PAPER");
        material.addProperty("description", "Base material for the item");
        properties.add("material", material);

        // Other properties
        addProperty(properties, "lore", "array", "Item lore lines with MiniMessage formatting", false, "minimessage",
                null);
        addProperty(properties, "unbreakable", "boolean", "Whether the item is unbreakable", false, null, null);
        properties.getAsJsonObject("unbreakable").addProperty("default", false);
        addProperty(properties, "unstackable", "boolean", "Whether the item cannot be stacked", false, null, null);
        properties.getAsJsonObject("unstackable").addProperty("default", false);
        addProperty(properties, "injectId", "boolean", "Whether to inject Oraxen item ID into NBT", false, null, null);
        properties.getAsJsonObject("injectId").addProperty("default", true);

        // color
        JsonObject color = new JsonObject();
        color.addProperty("type", "string");
        color.addProperty("format", "color");
        color.addProperty("pattern", "^(#[0-9A-Fa-f]{6}|\\d{1,3},\\s*\\d{1,3},\\s*\\d{1,3})$");
        color.addProperty("description",
                "RGB color for leather armor, potions, maps (e.g., '255, 128, 0' or '#FF8000')");
        properties.add("color", color);

        // trim_pattern
        addProperty(properties, "trim_pattern", "string", "Armor trim pattern key", false, "namespacedKey", "1.20+");

        // template
        addProperty(properties, "template", "string", "Template item ID to inherit from", false, null, null);

        // excludeFromInventory / excludeFromCommands
        addProperty(properties, "excludeFromInventory", "boolean", "Exclude from /oraxen inventory", false, null, null);
        addProperty(properties, "excludeFromCommands", "boolean", "Exclude from /oraxen give autocomplete", false, null,
                null);
        addProperty(properties, "no_auto_update", "boolean", "Disable automatic item updates", false, null, null);
        addProperty(properties, "disable_enchanting", "boolean", "Prevent enchanting this item", false, null, null);

        // ItemFlags
        JsonObject itemFlags = new JsonObject();
        itemFlags.addProperty("type", "array");
        JsonObject itemsSchema = new JsonObject();
        itemsSchema.addProperty("type", "enum");
        itemsSchema.addProperty("enum", "ItemFlag");
        itemFlags.add("items", itemsSchema);
        itemFlags.addProperty("description", "Item flags to apply");
        properties.add("ItemFlags", itemFlags);

        // Enchantments
        JsonObject enchantments = new JsonObject();
        enchantments.addProperty("type", "object");
        enchantments.addProperty("description", "Map of enchantment key to level");
        JsonObject enchantAdditional = new JsonObject();
        enchantAdditional.addProperty("type", "integer");
        enchantAdditional.addProperty("min", 1);
        enchantments.add("additionalProperties", enchantAdditional);
        properties.add("Enchantments", enchantments);

        // AttributeModifiers
        JsonObject attrMods = new JsonObject();
        attrMods.addProperty("type", "array");
        attrMods.addProperty("description", "Attribute modifiers to apply to the item");
        properties.add("AttributeModifiers", attrMods);

        // PotionEffects
        JsonObject potionEffects = new JsonObject();
        potionEffects.addProperty("type", "array");
        potionEffects.addProperty("description", "Potion effects for potion items");
        properties.add("PotionEffects", potionEffects);

        // PersistentData
        JsonObject persistentData = new JsonObject();
        persistentData.addProperty("type", "array");
        persistentData.addProperty("description", "Custom persistent data to store on the item");
        properties.add("PersistentData", persistentData);

        return properties;
    }

    private static void addProperty(JsonObject props, String name, String type, String desc, boolean required,
            String format, String minVersion) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", type);
        prop.addProperty("description", desc);
        if (required)
            prop.addProperty("required", true);
        if (format != null)
            prop.addProperty("format", format);
        if (minVersion != null)
            prop.addProperty("minecraftVersion", minVersion);
        props.add(name, prop);
    }

    private static JsonObject generateComponents() {
        JsonObject components = new JsonObject();

        // durability
        JsonObject durability = new JsonObject();
        durability.addProperty("type", "object");
        durability.addProperty("minecraftVersion", "1.20.5+");
        durability.addProperty("description", "Custom durability for the item");
        JsonObject durProps = new JsonObject();
        addComponentProp(durProps, "value", "integer", "Max durability value", 1, null);
        addComponentProp(durProps, "damage_block_break", "boolean", "Whether breaking blocks damages the item", null,
                true);
        addComponentProp(durProps, "damage_entity_hit", "boolean", "Whether hitting entities damages the item", null,
                true);
        durability.add("properties", durProps);
        components.add("durability", durability);

        // fire_resistant
        addSimpleComponent(components, "fire_resistant", "boolean", "Item is immune to fire/lava", "1.20.5+");

        // hide_tooltip
        addSimpleComponent(components, "hide_tooltip", "boolean", "Hide the item tooltip entirely", "1.20.5+");

        // max_stack_size
        JsonObject maxStack = new JsonObject();
        maxStack.addProperty("type", "integer");
        maxStack.addProperty("min", 1);
        maxStack.addProperty("max", 99);
        maxStack.addProperty("minecraftVersion", "1.20.5+");
        maxStack.addProperty("description", "Maximum stack size for this item");
        components.add("max_stack_size", maxStack);

        // food
        JsonObject food = new JsonObject();
        food.addProperty("type", "object");
        food.addProperty("minecraftVersion", "1.20.5+");
        food.addProperty("description", "Food properties for edible items");
        JsonObject foodProps = new JsonObject();
        addComponentProp(foodProps, "nutrition", "integer", "Food points restored", 0, null);
        addComponentProp(foodProps, "saturation", "number", "Saturation points restored", 0, null);
        addComponentProp(foodProps, "can_always_eat", "boolean", "Whether item can be eaten when not hungry", null,
                false);
        food.add("properties", foodProps);
        components.add("food", food);

        // consumable (1.21.2+)
        JsonObject consumable = new JsonObject();
        consumable.addProperty("type", "object");
        consumable.addProperty("minecraftVersion", "1.21.2+");
        consumable.addProperty("description", "Consumable properties");
        JsonObject consumeProps = new JsonObject();
        addComponentProp(consumeProps, "consume_seconds", "number", "Time to consume in seconds", 0, 1.6);
        JsonObject animProp = new JsonObject();
        animProp.addProperty("type", "string");
        animProp.addProperty("description", "Animation to play while consuming");
        consumeProps.add("animation", animProp);
        JsonObject soundProp = new JsonObject();
        soundProp.addProperty("type", "string");
        soundProp.addProperty("format", "sound");
        soundProp.addProperty("description", "Sound to play while consuming");
        consumeProps.add("sound", soundProp);
        addComponentProp(consumeProps, "has_consume_particles", "boolean", "Show particles while consuming", null,
                true);
        consumable.add("properties", consumeProps);
        components.add("consumable", consumable);

        // equippable (1.21.2+)
        JsonObject equippable = new JsonObject();
        equippable.addProperty("type", "object");
        equippable.addProperty("minecraftVersion", "1.21.2+");
        equippable.addProperty("description", "Equipment properties");
        JsonObject equipProps = new JsonObject();
        JsonObject slotProp = new JsonObject();
        slotProp.addProperty("type", "enum");
        slotProp.addProperty("enum", "EquipmentSlot");
        slotProp.addProperty("required", true);
        slotProp.addProperty("description", "Equipment slot this item can be equipped to");
        equipProps.add("slot", slotProp);
        addComponentProp(equipProps, "model", "string", "Model to use when equipped", null, null);
        addComponentProp(equipProps, "equip_sound", "string", "Sound to play when equipped", null, null);
        addComponentProp(equipProps, "dispensable", "boolean", "Can be equipped by dispensers", null, true);
        addComponentProp(equipProps, "swappable", "boolean", "Can be swapped with other equipment", null, true);
        addComponentProp(equipProps, "damage_on_hurt", "boolean", "Takes damage when player is hurt", null, true);
        addComponentProp(equipProps, "camera_overlay", "string", "Texture for camera overlay", null, null);
        equippable.add("properties", equipProps);
        components.add("equippable", equippable);

        // tool (1.20.5+)
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "object");
        tool.addProperty("minecraftVersion", "1.20.5+");
        tool.addProperty("description", "Tool properties for mining");
        JsonObject toolProps = new JsonObject();
        addComponentProp(toolProps, "damage_per_block", "integer", "Durability lost per block broken", 0, 1);
        addComponentProp(toolProps, "default_mining_speed", "number", "Default mining speed", 0, 1.0);
        JsonObject rules = new JsonObject();
        rules.addProperty("type", "array");
        rules.addProperty("description", "Mining rules for specific blocks/tags");
        toolProps.add("rules", rules);
        tool.add("properties", toolProps);
        components.add("tool", tool);

        // use_cooldown (1.21.2+)
        JsonObject useCooldown = new JsonObject();
        useCooldown.addProperty("type", "object");
        useCooldown.addProperty("minecraftVersion", "1.21.2+");
        useCooldown.addProperty("description", "Cooldown after using the item");
        JsonObject cooldownProps = new JsonObject();
        addComponentProp(cooldownProps, "seconds", "number", "Cooldown duration in seconds", 0, null);
        addComponentProp(cooldownProps, "group", "string", "Cooldown group (items in same group share cooldown)", null,
                null);
        useCooldown.add("properties", cooldownProps);
        components.add("use_cooldown", useCooldown);

        // use_remainder (1.21.2+)
        JsonObject useRemainder = new JsonObject();
        useRemainder.addProperty("type", "object");
        useRemainder.addProperty("minecraftVersion", "1.21.2+");
        useRemainder.addProperty("description", "Item left behind after consumption");
        JsonObject remainderProps = new JsonObject();
        addComponentProp(remainderProps, "oraxen_item", "string", "Oraxen item ID for remainder", null, null);
        addComponentProp(remainderProps, "minecraft_type", "string", "Vanilla material for remainder", null, null);
        addComponentProp(remainderProps, "crucible_item", "string", "MythicCrucible item ID", null, null);
        addComponentProp(remainderProps, "mmoitems_id", "string", "MMOItems item ID", null, null);
        addComponentProp(remainderProps, "mmoitems_type", "string", "MMOItems item type", null, null);
        addComponentProp(remainderProps, "ecoitem_id", "string", "EcoItems item ID", null, null);
        addComponentProp(remainderProps, "amount", "integer", "Amount of remainder items", 1, 1);
        useRemainder.add("properties", remainderProps);
        components.add("use_remainder", useRemainder);

        // jukebox_playable (1.21+)
        JsonObject jukebox = new JsonObject();
        jukebox.addProperty("type", "object");
        jukebox.addProperty("minecraftVersion", "1.21+");
        jukebox.addProperty("description", "Makes item playable in jukeboxes");
        JsonObject jukeboxProps = new JsonObject();
        addComponentProp(jukeboxProps, "show_in_tooltip", "boolean", "Show song in tooltip", null, true);
        addComponentProp(jukeboxProps, "song_key", "string", "Namespaced key of the song", null, null);
        jukebox.add("properties", jukeboxProps);
        components.add("jukebox_playable", jukebox);

        // tooltip_style (1.21.2+)
        addSimpleComponent(components, "tooltip_style", "string", "Custom tooltip style resource location", "1.21.2+");

        // item_model (1.21.2+)
        addSimpleComponent(components, "item_model", "string", "Custom item model resource location", "1.21.2+");

        return components;
    }

    private static void addSimpleComponent(JsonObject components, String name, String type, String desc,
            String minVersion) {
        JsonObject comp = new JsonObject();
        comp.addProperty("type", type);
        comp.addProperty("description", desc);
        if (minVersion != null)
            comp.addProperty("minecraftVersion", minVersion);
        components.add(name, comp);
    }

    private static void addComponentProp(JsonObject props, String name, String type, String desc, Number min,
            Object defaultVal) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", type);
        prop.addProperty("description", desc);
        if (min != null)
            prop.addProperty("min", min);
        if (defaultVal != null) {
            if (defaultVal instanceof Boolean)
                prop.addProperty("default", (Boolean) defaultVal);
            else if (defaultVal instanceof Number)
                prop.addProperty("default", (Number) defaultVal);
            else
                prop.addProperty("default", defaultVal.toString());
        }
        props.add(name, prop);
    }

    private static JsonObject generateMechanics() {
        JsonObject mechanics = new JsonObject();

        // First, try to get schemas from registered mechanic factories
        Map<String, MechanicFactory> factories = MechanicsManager.getAllFactories();

        for (Map.Entry<String, MechanicFactory> entry : factories.entrySet()) {
            String mechanicId = entry.getKey();
            MechanicFactory factory = entry.getValue();

            List<MechanicConfigProperty> schema = factory.getConfigSchema();
            String category = factory.getMechanicCategory();
            String description = factory.getMechanicDescription();

            // Include factory if it provides schema, category, or description metadata
            // This ensures factories with @MechanicInfo (including deprecation warnings)
            // are included even if they have no @ConfigProperty fields
            if (!schema.isEmpty() || category != null || description != null) {
                addMechanicFromFactory(mechanics, mechanicId, factory);
            }
        }

        // Fallback: Add manual schemas for mechanics that haven't been updated yet
        // These will only be added if not already present from factory

        // Combat mechanics
        addMechanicIfAbsent(mechanics, "lifeleech", "combat", "Heals player when dealing damage",
                Map.of("amount", prop("integer", "Health restored per hit (in half-hearts)", 1, null)));

        addMechanicIfAbsent(mechanics, "bleeding", "combat", "Causes targets to bleed over time",
                Map.of(
                        "chance", prop("number", "Chance to apply bleeding (0-1)", 0, 0.3),
                        "duration", prop("integer", "Duration in ticks", 1, 100),
                        "damage_per_interval", prop("number", "Damage per tick", 0, 0.5),
                        "interval", prop("integer", "Ticks between damage", 1, 20)));

        addMechanicIfAbsent(mechanics, "thor", "combat", "Summons lightning on hit",
                Map.of(
                        "lightning_bolts_amount", prop("integer", "Number of lightning bolts", 1, 1),
                        "random_location_variation", prop("number", "Random offset for bolt position", 0, 1.5),
                        "delay", prop("integer", "Cooldown in milliseconds", 0, null)));

        addMechanicIfAbsent(mechanics, "spear_lunge", "combat", "Charge and lunge attack for spears",
                Map.of(
                        "charge_ticks", prop("integer", "Ticks to charge before lunge", 1, 12),
                        "lunge_velocity", prop("number", "Velocity multiplier for lunge", 0, 0.8),
                        "active_model", prop("string", "Model to show while charging", null, null),
                        "cooldown_ticks", prop("integer", "Cooldown after lunge in ticks", 0, null)));

        addMechanicIfAbsent(mechanics, "energyblast", "combat", "Fires an energy blast projectile",
                Map.of("delay", prop("integer", "Cooldown in milliseconds", 0, null)));

        addMechanicIfAbsent(mechanics, "fireball", "combat", "Shoots a fireball",
                Map.of("delay", prop("integer", "Cooldown in milliseconds", 0, null)));

        addMechanicIfAbsent(mechanics, "witherskull", "combat", "Launches a wither skull",
                Map.of("delay", prop("integer", "Cooldown in milliseconds", 0, null)));

        // Farming mechanics
        addMechanicIfAbsent(mechanics, "bigmining", "farming", "Mines blocks in an area",
                Map.of(
                        "radius", prop("integer", "Horizontal radius", 1, null),
                        "depth", prop("integer", "Depth of mining area", 1, null)));

        addMechanicIfAbsent(mechanics, "smelting", "farming", "Auto-smelts mined blocks",
                Map.of("play_sound", prop("boolean", "Play smelting sound", null, true)));

        addMechanicIfAbsent(mechanics, "harvesting", "farming", "Harvests and replants crops", Map.of());
        addMechanicIfAbsent(mechanics, "watering", "farming", "Waters farmland", Map.of());
        addMechanicIfAbsent(mechanics, "bottledexp", "farming", "Stores experience in bottles", Map.of());

        addMechanicIfAbsent(mechanics, "bedrockbreak", "farming", "Allows breaking bedrock (requires ProtocolLib)",
                Map.of(
                        "delay", prop("integer", "Break delay in ticks", 0, null),
                        "probability", prop("number", "Chance to break (0-1)", 0, 1.0)));

        // Gameplay mechanics
        addMechanicIfAbsent(mechanics, "durability", "gameplay", "Custom durability behavior",
                Map.of("value", prop("integer", "Durability value", 1, null)));

        addMechanicIfAbsent(mechanics, "efficiency", "gameplay", "Modifies mining speed",
                Map.of("amount", prop("number", "Efficiency modifier", null, null)));

        addMechanicIfAbsent(mechanics, "repair", "gameplay", "Allows repairing with custom materials",
                Map.of(
                        "ratio", prop("number", "Repair ratio per material", 0, null),
                        "oraxen_item", prop("string", "Oraxen item ID for repair material", null, null)));

        addMechanicIfAbsent(mechanics, "furniture", "gameplay", "Place item as furniture entity",
                Map.of(
                        "barrier", prop("boolean", "Use barrier block for collision", null, false),
                        "light", prop("integer", "Light level (0-15)", 0, null),
                        "hardness", prop("number", "Break hardness", 0, null)));

        addMechanicIfAbsent(mechanics, "noteblock", "gameplay", "Custom noteblock-based block",
                Map.of(
                        "hardness", prop("number", "Block hardness", 0, null),
                        "light", prop("integer", "Light level (0-15)", 0, null)));

        addMechanicIfAbsent(mechanics, "stringblock", "gameplay", "Custom tripwire-based block",
                Map.of(
                        "hardness", prop("number", "Block hardness", 0, null),
                        "light", prop("integer", "Light level (0-15)", 0, null)));

        addMechanicIfAbsent(mechanics, "block", "gameplay", "Custom block mechanic", Map.of());

        // Cosmetic mechanics
        addMechanicIfAbsent(mechanics, "aura", "cosmetic", "Particle aura around player",
                Map.of(
                        "type", propEnum("string", "Aura type", new String[] { "simple", "ring", "helix" }),
                        "particle", propEnum("enum", "Particle type", "Particle")));

        addMechanicIfAbsent(mechanics, "hat", "cosmetic", "Item can be worn as a hat", Map.of());

        addMechanicIfAbsent(mechanics, "skin", "cosmetic", "Skin item for skinnable items",
                Map.of("consume", prop("boolean", "Consume skin on apply", null, true)));

        addMechanicIfAbsent(mechanics, "skinnable", "cosmetic", "Item that can accept skins", Map.of());

        // Misc mechanics
        addMechanicIfAbsent(mechanics, "soulbound", "misc", "Item stays in inventory on death",
                Map.of("lose_chance", prop("number", "Chance to lose item anyway (0-100)", 0, 0)));

        addMechanicIfAbsent(mechanics, "armor_effects", "misc", "Apply potion effects while wearing armor", Map.of());

        addMechanicIfAbsent(mechanics, "commands", "misc", "Execute commands on events",
                Map.of(
                        "permission", prop("string", "Required permission", null, null),
                        "cooldown", prop("integer", "Cooldown in ticks", 0, null)));

        addMechanicIfAbsent(mechanics, "custom", "misc", "Custom mechanic with actions and conditions", Map.of());
        addMechanicIfAbsent(mechanics, "consumable", "misc", "Legacy consumable behavior (pre-1.21.2)", Map.of());
        addMechanicIfAbsent(mechanics, "consumable_potion_effects", "misc", "Apply effects on consume", Map.of());

        addMechanicIfAbsent(mechanics, "food", "misc", "Legacy food behavior (pre-1.21.2)",
                Map.of(
                        "hunger", prop("integer", "Hunger points restored", 0, null),
                        "saturation", prop("number", "Saturation restored", 0, null)));

        addMechanicIfAbsent(mechanics, "music_disc", "misc", "Custom music disc",
                Map.of("song", prop("string", "Song resource location", null, null)));

        addMechanicIfAbsent(mechanics, "backpack", "misc", "Portable storage",
                Map.of(
                        "rows", prop("integer", "Number of rows (1-6)", 1, 3),
                        "title", prop("string", "Inventory title", null, null)));

        addMechanicIfAbsent(mechanics, "itemtype", "misc", "Define item type behavior",
                Map.of("type", prop("string", "Item type identifier", null, null)));

        addMechanicIfAbsent(mechanics, "misc", "misc", "Miscellaneous properties",
                Map.of("break_music_discs", prop("boolean", "Can break music discs", null, false)));

        return mechanics;
    }

    private static void addMechanic(JsonObject mechanics, String id, String category, String description,
            Map<String, JsonObject> properties) {
        JsonObject mechanic = new JsonObject();
        mechanic.addProperty("category", category);
        mechanic.addProperty("description", description);

        if (!properties.isEmpty()) {
            JsonObject props = new JsonObject();
            properties.forEach(props::add);
            mechanic.add("properties", props);
        }

        mechanics.add(id, mechanic);
    }

    /**
     * Add mechanic only if not already present (from factory schema).
     */
    private static void addMechanicIfAbsent(JsonObject mechanics, String id, String category, String description,
            Map<String, JsonObject> properties) {
        if (!mechanics.has(id)) {
            addMechanic(mechanics, id, category, description, properties);
        }
    }

    /**
     * Add mechanic schema from a MechanicFactory that provides its own schema.
     */
    private static void addMechanicFromFactory(JsonObject mechanics, String mechanicId, MechanicFactory factory) {
        JsonObject mechanic = new JsonObject();

        String category = factory.getMechanicCategory();
        if (category != null) {
            mechanic.addProperty("category", category);
        }

        String description = factory.getMechanicDescription();
        if (description != null) {
            mechanic.addProperty("description", description);
        }

        // Mark as auto-generated from factory
        mechanic.addProperty("_source", "factory");

        List<MechanicConfigProperty> schema = factory.getConfigSchema();
        if (!schema.isEmpty()) {
            JsonObject props = new JsonObject();
            for (MechanicConfigProperty prop : schema) {
                props.add(prop.name(), convertPropertyToJson(prop));
            }
            mechanic.add("properties", props);
        }

        mechanics.add(mechanicId, mechanic);
    }

    /**
     * Convert a MechanicConfigProperty to JSON representation.
     */
    private static JsonObject convertPropertyToJson(MechanicConfigProperty prop) {
        JsonObject json = new JsonObject();

        // Type mapping
        String type = switch (prop.type()) {
            case STRING -> "string";
            case INTEGER -> "integer";
            case DOUBLE -> "number";
            case BOOLEAN -> "boolean";
            case LIST -> "array";
            case OBJECT -> "object";
            case ENUM -> "string";
        };
        json.addProperty("type", type);

        if (prop.description() != null) {
            json.addProperty("description", prop.description());
        }

        if (prop.defaultValue() != null) {
            if (prop.defaultValue() instanceof Boolean) {
                json.addProperty("default", (Boolean) prop.defaultValue());
            } else if (prop.defaultValue() instanceof Number) {
                json.addProperty("default", (Number) prop.defaultValue());
            } else {
                json.addProperty("default", prop.defaultValue().toString());
            }
        }

        if (prop.min() != null) {
            json.addProperty("min", prop.min());
        }

        if (prop.max() != null) {
            json.addProperty("max", prop.max());
        }

        if (prop.enumRef() != null) {
            json.addProperty("enum", prop.enumRef());
        }

        if (prop.enumValues() != null && !prop.enumValues().isEmpty()) {
            JsonArray values = new JsonArray();
            for (String v : prop.enumValues()) {
                values.add(v);
            }
            json.add("values", values);
        }

        if (prop.required()) {
            json.addProperty("required", true);
        }

        if (prop.nestedProperties() != null && !prop.nestedProperties().isEmpty()) {
            JsonObject nested = new JsonObject();
            for (Map.Entry<String, MechanicConfigProperty> e : prop.nestedProperties().entrySet()) {
                nested.add(e.getKey(), convertPropertyToJson(e.getValue()));
            }
            json.add("properties", nested);
        }

        return json;
    }

    private static JsonObject prop(String type, String desc, Number min, Object defaultVal) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", type);
        prop.addProperty("description", desc);
        if (min != null)
            prop.addProperty("min", min);
        if (defaultVal != null) {
            if (defaultVal instanceof Boolean)
                prop.addProperty("default", (Boolean) defaultVal);
            else if (defaultVal instanceof Number)
                prop.addProperty("default", (Number) defaultVal);
            else
                prop.addProperty("default", defaultVal.toString());
        }
        return prop;
    }

    private static JsonObject propEnum(String type, String desc, Object enumRef) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", type);
        prop.addProperty("description", desc);
        if (enumRef instanceof String) {
            prop.addProperty("enum", (String) enumRef);
        } else if (enumRef instanceof String[]) {
            JsonArray values = new JsonArray();
            for (String v : (String[]) enumRef)
                values.add(v);
            prop.add("values", values);
        }
        return prop;
    }

    private static JsonObject generatePackSchema() {
        JsonObject pack = new JsonObject();

        // generate_model
        JsonObject generateModel = new JsonObject();
        generateModel.addProperty("type", "boolean");
        generateModel.addProperty("description", "Whether to auto-generate model from textures");
        generateModel.addProperty("default", true);
        pack.add("generate_model", generateModel);

        // parent_model
        JsonObject parentModel = new JsonObject();
        parentModel.addProperty("type", "string");
        parentModel.addProperty("description", "Parent model to inherit display properties from");
        parentModel.addProperty("default", "item/generated");
        JsonArray suggestions = new JsonArray();
        String[] parentSuggestions = { "item/generated", "item/handheld", "item/handheld_rod", "block/cube",
                "block/cube_all" };
        for (String s : parentSuggestions)
            suggestions.add(s);
        parentModel.add("suggestions", suggestions);
        pack.add("parent_model", parentModel);

        // textures
        JsonObject textures = new JsonObject();
        textures.addProperty("description", "Texture paths for model generation");
        JsonArray oneOf = new JsonArray();
        JsonObject arrayType = new JsonObject();
        arrayType.addProperty("type", "array");
        arrayType.addProperty("description", "List of texture paths for layered 2D items");
        oneOf.add(arrayType);
        JsonObject objectType = new JsonObject();
        objectType.addProperty("type", "object");
        objectType.addProperty("description", "Named texture map for block models (top, side, bottom, etc.)");
        oneOf.add(objectType);
        textures.add("oneOf", oneOf);
        pack.add("textures", textures);

        // model
        JsonObject model = new JsonObject();
        model.addProperty("type", "string");
        model.addProperty("description", "Path to existing JSON model file (without .json)");
        pack.add("model", model);

        // custom_model_data
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "integer");
        cmd.addProperty("description", "Specific CustomModelData value (auto-generated if not set)");
        pack.add("custom_model_data", cmd);

        // State-specific models
        addPackProp(pack, "blocking_model", "Model for shield blocking state");
        addPackProp(pack, "blocking_texture", "Texture for shield blocking state");
        addPackProp(pack, "cast_model", "Model for cast fishing rod");
        addPackProp(pack, "cast_texture", "Texture for cast fishing rod");
        addPackProp(pack, "charged_model", "Model for charged crossbow");
        addPackProp(pack, "charged_texture", "Texture for charged crossbow");
        addPackProp(pack, "firework_model", "Model for crossbow with firework");
        addPackProp(pack, "firework_texture", "Texture for crossbow with firework");

        // Arrays
        JsonObject pullingModels = new JsonObject();
        pullingModels.addProperty("type", "array");
        pullingModels.addProperty("description", "Models for bow pulling animation stages");
        pack.add("pulling_models", pullingModels);

        JsonObject pullingTextures = new JsonObject();
        pullingTextures.addProperty("type", "array");
        pullingTextures.addProperty("description", "Textures for bow pulling animation stages");
        pack.add("pulling_textures", pullingTextures);

        JsonObject damagedModels = new JsonObject();
        damagedModels.addProperty("type", "array");
        damagedModels.addProperty("description", "Models at different durability levels");
        pack.add("damaged_models", damagedModels);

        JsonObject damagedTextures = new JsonObject();
        damagedTextures.addProperty("type", "array");
        damagedTextures.addProperty("description", "Textures at different durability levels");
        pack.add("damaged_textures", damagedTextures);

        // 1.21.4+ properties
        JsonObject excludePredicates = new JsonObject();
        excludePredicates.addProperty("type", "boolean");
        excludePredicates.addProperty("description", "Exclude from CustomModelData predicates");
        excludePredicates.addProperty("minecraftVersion", "1.21.4+");
        pack.add("exclude_from_predicates", excludePredicates);

        JsonObject excludeItemModel = new JsonObject();
        excludeItemModel.addProperty("type", "boolean");
        excludeItemModel.addProperty("description", "Exclude from item_model component");
        excludeItemModel.addProperty("minecraftVersion", "1.21.4+");
        pack.add("exclude_from_item_model", excludeItemModel);

        JsonObject oversizedGui = new JsonObject();
        oversizedGui.addProperty("type", "boolean");
        oversizedGui.addProperty("description", "Allow oversized rendering in GUI");
        oversizedGui.addProperty("minecraftVersion", "1.21.4+");
        pack.add("oversized_in_gui", oversizedGui);

        JsonObject handAnim = new JsonObject();
        handAnim.addProperty("type", "boolean");
        handAnim.addProperty("description", "Play hand animation on item swap");
        handAnim.addProperty("default", true);
        handAnim.addProperty("minecraftVersion", "1.21.4+");
        pack.add("hand_animation_on_swap", handAnim);

        JsonObject swapScale = new JsonObject();
        swapScale.addProperty("type", "number");
        swapScale.addProperty("description", "Scale of swap animation");
        swapScale.addProperty("default", 1.0);
        swapScale.addProperty("minecraftVersion", "1.21.4+");
        pack.add("swap_animation_scale", swapScale);

        addPackProp(pack, "generated_model_path", "Custom path for generated model output");

        return pack;
    }

    private static void addPackProp(JsonObject pack, String name, String desc) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", "string");
        prop.addProperty("description", desc);
        pack.add(name, prop);
    }
}
