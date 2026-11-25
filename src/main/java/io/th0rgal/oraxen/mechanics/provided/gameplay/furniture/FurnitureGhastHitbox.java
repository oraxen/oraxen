package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.VectorUtils;
import org.bukkit.util.Vector;

record FurnitureGhastHitbox(double x, double y, double z, double scale) {

    static FurnitureGhastHitbox parse(Object value) {
        if (!(value instanceof String entry)) return null;
        String[] parts = entry.trim().split("\\s+");
        if (parts.length != 2) return null;
        double[] coordinates = VectorUtils.parseCoordinates(parts[0]);
        if (coordinates == null) return null;

        try {
            double scale = Double.parseDouble(parts[1]);
            if (!Double.isFinite(scale) || scale < 0.0625 || scale > 1) return null;
            return new FurnitureGhastHitbox(coordinates[0], coordinates[1], coordinates[2], scale);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    Vector offset() {
        return new Vector(x, y, z);
    }
}
