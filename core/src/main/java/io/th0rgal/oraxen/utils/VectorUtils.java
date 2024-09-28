package io.th0rgal.oraxen.utils;

import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;

public class VectorUtils {

    public static Quaternionf getQuaternionfFromString(String quaternion, float defaultValue) {
        List<Float> floats = new java.util.ArrayList<>(Arrays.stream(quaternion.replace(" ", "").split(",")).map(s -> ParseUtils.parseFloat(s, defaultValue)).toList());
        while (floats.size() < 4) floats.add(defaultValue);
        return new Quaternionf(floats.get(0), floats.get(1), floats.get(2), floats.get(3));
    }

    public static Vector3f getVector3fFromString(String vector, float defaultValue) {
        List<Float> floats = new java.util.ArrayList<>(Arrays.stream(vector.replace(" ", "").split(",")).map(s -> ParseUtils.parseFloat(s, defaultValue)).toList());
        while (floats.size() < 3) floats.add(defaultValue);
        return new Vector3f(floats.get(0), floats.get(1), floats.get(2));
    }

    public static Vector getVectorFromString(String vector, float defaultValue) {
        List<Float> floats = new java.util.ArrayList<>(Arrays.stream(vector.replace(" ", "").split(",")).map(s -> ParseUtils.parseFloat(s, defaultValue)).toList());
        while (floats.size() < 3) floats.add(defaultValue);
        return new Vector(floats.get(0), floats.get(1), floats.get(2));
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
