package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.utils.VectorUtils;
import org.bukkit.util.Vector;

record FurnitureBed(double x, double y, double z, boolean skipNight, boolean resetPhantoms,
                    boolean nightOnly, boolean setRespawn) {

    static FurnitureBed parse(Object value) {
        if (!(value instanceof String entry)) return null;
        String[] parts = entry.trim().split("\\s+");
        if (parts.length > 5) return null;
        double[] coordinates = VectorUtils.parseCoordinates(parts[0]);
        if (coordinates == null) return null;

        boolean[] flags = {true, true, true, false};
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("true")) flags[i - 1] = true;
            else if (parts[i].equalsIgnoreCase("false")) flags[i - 1] = false;
            else return null;
        }

        return new FurnitureBed(coordinates[0], coordinates[1], coordinates[2], flags[0], flags[1], flags[2], flags[3]);
    }

    Vector offset() {
        return new Vector(x, y, z);
    }
}
