package io.th0rgal.oraxen.configs;

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
}
