package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FurnitureConfigMigration {

    private FurnitureConfigMigration() {}

    public static boolean migrate(ConfigurationSection item) {
        if (item == null) return false;
        ConfigurationSection mechanics = item.getConfigurationSection("mechanics");
        if (mechanics == null) return false;
        ConfigurationSection furniture = mechanics.getConfigurationSection("furniture");
        if (furniture == null) return false;

        boolean updated = false;
        updated |= migrateLegacyProperties(furniture);
        updated |= migrateHitbox(furniture);
        updated |= migrateSeat(furniture);
        updated |= migrateLight(furniture);
        return updated;
    }

    private static boolean migrateLegacyProperties(ConfigurationSection furniture) {
        ConfigurationSection displayProperties = furniture.getConfigurationSection("display_entity_properties");
        ConfigurationSection armorStandProperties = furniture.getConfigurationSection("armor_stand_properties");
        if (displayProperties == null && armorStandProperties == null) return false;

        ConfigurationSection properties = furniture.getConfigurationSection("properties");
        if (properties == null) properties = furniture.createSection("properties");
        boolean armorStand = isArmorStand(furniture);
        // Preserve explicit unified properties, then prefer the legacy section for the selected type.
        mergeMissingProperties(properties, armorStand ? armorStandProperties : displayProperties);
        mergeMissingProperties(properties, armorStand ? displayProperties : armorStandProperties);
        furniture.set("display_entity_properties", null);
        furniture.set("armor_stand_properties", null);
        return true;
    }

    private static boolean isArmorStand(ConfigurationSection furniture) {
        String defaultType = FurnitureFactory.defaultFurnitureType != null
                ? FurnitureFactory.defaultFurnitureType.name() : "DISPLAY_ENTITY";
        return furniture.getString("type", defaultType).equals("ARMOR_STAND");
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
            if (barrier instanceof Map<?, ?> location)
                offsets.add(coordinate(location, "x") + "," + coordinate(location, "y") + "," + coordinate(location, "z"));

        List<String> lights = new ArrayList<>();
        for (String offset : offsets)
            lights.add(offset + " " + level);
        return lights;
    }

    private static int coordinate(Map<?, ?> location, String axis) {
        return location.get(axis) instanceof Number number ? number.intValue() : 0;
    }
}
