package io.th0rgal.oraxen.utils;

import org.bukkit.util.Vector;

public class VectorUtils {

    private VectorUtils() {}

    public static double[] parseCoordinates(String value) {
        if (value == null) return null;
        String[] parts = value.split(",", -1);
        if (parts.length != 3) return null;
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
            return new double[]{x, y, z};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static void rotateAroundAxisX(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double y = v.getY() * cos - v.getZ() * sin;
        double z = v.getY() * sin + v.getZ() * cos;
        v.setY(y).setZ(z);
    }

    public static void rotateAroundAxisY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        v.setX(x).setZ(z);
    }

}
