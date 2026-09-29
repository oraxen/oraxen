package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureFactory;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public final class FurnitureConfigMigration {

    private FurnitureConfigMigration() {}

    public static boolean migrate(ConfigurationSection item) {
        if (item == null) return false;
        ConfigurationSection mechanics = item.getConfigurationSection("mechanics");
        if (mechanics == null) return false;
        ConfigurationSection furniture = mechanics.getConfigurationSection("furniture");
        if (furniture == null) return false;

        boolean updated = false;
        ConfigurationSection displayProperties = furniture.getConfigurationSection("display_entity_properties");
        ConfigurationSection armorStandProperties = furniture.getConfigurationSection("armor_stand_properties");
        if (displayProperties != null || armorStandProperties != null) {
            ConfigurationSection properties = furniture.getConfigurationSection("properties");
            if (properties == null) properties = furniture.createSection("properties");
            String defaultType = FurnitureFactory.defaultFurnitureType != null
                    ? FurnitureFactory.defaultFurnitureType.name() : "DISPLAY_ENTITY";
            boolean armorStand = furniture.getString("type", defaultType).equals("ARMOR_STAND");
            // Preserve explicit unified properties, then prefer the legacy section for the selected type.
            mergeMissingProperties(properties, armorStand ? armorStandProperties : displayProperties);
            mergeMissingProperties(properties, armorStand ? displayProperties : armorStandProperties);
            furniture.set("display_entity_properties", null);
            furniture.set("armor_stand_properties", null);
            updated = true;
        }

        ConfigurationSection hitbox = furniture.getConfigurationSection("hitbox");
        if (hitbox != null) {
            if (!furniture.contains("hitboxes"))
                furniture.set("hitboxes", List.of("0,0,0 " + hitbox.getDouble("width", 1.0) + "," + hitbox.getDouble("height", 1.0)));
            furniture.set("hitbox", null);
            updated = true;
        }

        ConfigurationSection seat = furniture.getConfigurationSection("seat");
        if (seat != null) {
            if (!furniture.contains("seats")) {
                String entry = "0," + (seat.getDouble("height") - 1) + ",0";
                if (seat.contains("yaw")) entry += " " + seat.getDouble("yaw");
                furniture.set("seats", List.of(entry));
            }
            furniture.set("seat", null);
            updated = true;
        }

        if (furniture.contains("light")) {
            if (!furniture.contains("lights"))
                furniture.set("lights", List.of("0,0,0 " + furniture.getInt("light")));
            furniture.set("light", null);
            updated = true;
        }
        return updated;
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
}
