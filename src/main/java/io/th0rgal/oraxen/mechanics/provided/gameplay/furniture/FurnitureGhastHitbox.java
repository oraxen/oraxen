package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.util.Vector;

record FurnitureGhastHitbox(double x, double y, double z, double scale) {

    static FurnitureGhastHitbox parse(Object value) {
        if (!(value instanceof String entry)) return null;
        String[] parts = entry.trim().split("\\s+");
        if (parts.length != 2) return null;
        String[] coordinates = parts[0].split(",", -1);
        if (coordinates.length != 3) return null;

        try {
            double x = Double.parseDouble(coordinates[0]);
            double y = Double.parseDouble(coordinates[1]);
            double z = Double.parseDouble(coordinates[2]);
            double scale = Double.parseDouble(parts[1]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(scale) || scale < 0.0625 || scale > 1) return null;
            return new FurnitureGhastHitbox(x, y, z, scale);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    Vector offset() {
        return new Vector(x, y, z);
    }
}
