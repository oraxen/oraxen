package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import io.th0rgal.oraxen.items.ItemMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FurnitureConfigMigration {

    private FurnitureConfigMigration() {}

    /** Migrate before templates are loaded, since the two legacy keys inherited independently. */
    public static Set<ConfigurationSection> migrateBarriers(List<ConfigurationSection> items) {
        Set<ConfigurationSection> updated = new LinkedHashSet<>();
        Map<String, ConfigurationSection> templates = new HashMap<>();
        for (ConfigurationSection item : items) {
            if (ItemMigrator.migrateUppercaseSections(item)) updated.add(item);
            ConfigurationSection furniture = item.getConfigurationSection("mechanics.furniture");
            if (!item.isBoolean("template") || furniture == null) continue;
            ConfigurationSection snapshot = new MemoryConfiguration();
            snapshot.set("barrier", furniture.get("barrier"));
            snapshot.set("barriers", furniture.get("barriers"));
            templates.put(item.getName(), snapshot);
        }

        for (ConfigurationSection item : items) {
            ConfigurationSection furniture = item.getConfigurationSection("mechanics.furniture");
            if (furniture == null) continue;
            ConfigurationSection template = templates.get(item.getString("template"));
            if (template != null && (furniture.contains("barrier") || furniture.contains("barriers"))) {
                ConfigurationSection effective = new MemoryConfiguration();
                for (String key : List.of("barrier", "barriers"))
                    effective.set(key, furniture.contains(key) ? furniture.get(key) : template.get(key));
                if (migrateBarriers(effective)) {
                    furniture.set("barriers", effective.get("barriers"));
                    furniture.set("barrier", null);
                    updated.add(item);
                }
            } else if (migrateBarriers(furniture)) updated.add(item);
        }
        return updated;
    }

    public static boolean migrate(ConfigurationSection item) {
        return migrate(item, null);
    }

    public static boolean migrate(ConfigurationSection item, String inheritedType) {
        if (item == null) return false;
        ConfigurationSection mechanics = item.getConfigurationSection("mechanics");
        if (mechanics == null) return false;
        ConfigurationSection furniture = mechanics.getConfigurationSection("furniture");
        if (furniture == null) return false;

        boolean updated = false;
        updated |= migrateLegacyProperties(furniture, inheritedType);
        updated |= migrateHitbox(furniture);
        updated |= migrateSeat(furniture);
        updated |= migrateBarriers(furniture);
        updated |= migrateLight(furniture);
        return updated;
    }

    private static boolean migrateLegacyProperties(ConfigurationSection furniture, String inheritedType) {
        ConfigurationSection displayProperties = furniture.getConfigurationSection("display_entity_properties");
        ConfigurationSection armorStandProperties = furniture.getConfigurationSection("armor_stand_properties");
        if (displayProperties == null && armorStandProperties == null) return false;

        ConfigurationSection properties = furniture.getConfigurationSection("properties");
        if (properties == null) properties = furniture.createSection("properties");
        boolean armorStand = isArmorStand(furniture, inheritedType);
        // Preserve the section selected by the legacy parser, including missing-axis defaults.
        mergeMissingProperties(properties, armorStand && armorStandProperties != null
                ? armorStandProperties : displayProperties);
        furniture.set("display_entity_properties", null);
        furniture.set("armor_stand_properties", null);
        return true;
    }

    private static boolean isArmorStand(ConfigurationSection furniture, String inheritedType) {
        String defaultType = FurnitureFactory.defaultFurnitureType != null
                ? FurnitureFactory.defaultFurnitureType.name() : "DISPLAY_ENTITY";
        return furniture.getString("type", inheritedType != null ? inheritedType : defaultType).equals("ARMOR_STAND");
    }

    private static boolean migrateHitbox(ConfigurationSection furniture) {
        ConfigurationSection hitbox = furniture.getConfigurationSection("hitbox");
        if (hitbox == null) return false;

        double width = hitbox.getDouble("width", 1.0), height = hitbox.getDouble("height", 1.0);
        // A non-positive legacy hitbox meant "no hitbox".
        if (!furniture.contains("hitboxes"))
            furniture.set("hitboxes", width > 0 && height > 0 ? List.of("0,0,0 " + width + "," + height) : List.of());
        furniture.set("hitbox", null);
        return true;
    }

    private static boolean migrateSeat(ConfigurationSection furniture) {
        ConfigurationSection seat = furniture.getConfigurationSection("seat");
        if (seat == null) return false;

        if (!furniture.contains("seats")) {
            String entry = "0," + (seat.getDouble("height") - 1) + ",0";
            if (seat.contains("yaw")) entry += " " + seat.getDouble("yaw");
            furniture.set("seats", List.of(entry));
        }
        furniture.set("seat", null);
        return true;
    }

    private static boolean migrateBarriers(ConfigurationSection furniture) {
        boolean updated = furniture.contains("barrier");
        List<Object> barriers = new ArrayList<>();
        if (furniture.getBoolean("barrier", false)) barriers.add("0,0,0");
        for (Object barrier : furniture.getList("barriers", List.of())) {
            if (barrier instanceof Map<?, ?> location) {
                barriers.add(coordinate(location, "x") + "," + coordinate(location, "y") + "," + coordinate(location, "z"));
                updated = true;
            } else if ("origin".equals(barrier)) {
                barriers.add("0,0,0");
                updated = true;
            } else barriers.add(barrier);
        }
        if (!updated) return false;

        furniture.set("barriers", barriers);
        furniture.set("barrier", null);
        return true;
    }

    private static boolean migrateLight(ConfigurationSection furniture) {
        if (!furniture.contains("light")) return false;

        if (!furniture.contains("lights"))
            furniture.set("lights", legacyLights(furniture, furniture.getInt("light")));
        furniture.set("light", null);
        return true;
    }

    private static void mergeMissingProperties(ConfigurationSection target, ConfigurationSection source) {
        if (source == null) return;
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child != null) {
                if (!target.contains(key)) target.createSection(key);
                ConfigurationSection targetChild = target.getConfigurationSection(key);
                if (targetChild != null) mergeMissingProperties(targetChild, child);
            } else if (!target.contains(key)) {
                target.set(key, source.get(key));
            }
        }
    }

    /** The legacy light lit the base block and every barrier. */
    private static List<String> legacyLights(ConfigurationSection furniture, int level) {
        Set<String> offsets = new LinkedHashSet<>();
        offsets.add("0,0,0");
        for (Object barrier : furniture.getList("barriers", List.of()))
            if (barrier instanceof String offset)
                offsets.add(offset.trim().replaceAll("\\s*,\\s*", ","));

        List<String> lights = new ArrayList<>();
        for (String offset : offsets)
            lights.add(offset + " " + level);
        return lights;
    }

    private static int coordinate(Map<?, ?> location, String axis) {
        return location.get(axis) instanceof Number number ? number.intValue() : 0;
    }
}
