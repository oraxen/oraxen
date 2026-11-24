package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import org.bukkit.util.Vector;

record FurnitureBed(double x, double y, double z, boolean skipNight, boolean resetPhantoms,
                    boolean nightOnly, boolean setRespawn) {

    static FurnitureBed parse(Object value) {
        if (!(value instanceof String entry)) return null;
        String[] parts = entry.trim().split("\\s+");
        if (parts.length > 5) return null;
        String[] coordinates = parts[0].split(",", -1);
        if (coordinates.length != 3) return null;

        boolean[] flags = {true, true, true, false};
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("true")) flags[i - 1] = true;
            else if (parts[i].equalsIgnoreCase("false")) flags[i - 1] = false;
            else return null;
        }

        try {
            double x = Double.parseDouble(coordinates[0]);
            double y = Double.parseDouble(coordinates[1]);
            double z = Double.parseDouble(coordinates[2]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
            return new FurnitureBed(x, y, z, flags[0], flags[1], flags[2], flags[3]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    Vector offset() {
        return new Vector(x, y, z);
    }
}
