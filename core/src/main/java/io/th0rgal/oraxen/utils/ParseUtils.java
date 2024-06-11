package io.th0rgal.oraxen.utils;

public class ParseUtils {

    public static Double parseDouble(String string, double defaultValue) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Float parseFloat(String string, float defaultValue) {
        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Integer parseInt(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) value = min;
        if (!(min < max)) {
            if (Float.isNaN(min)) {
                throw new IllegalArgumentException("min is NaN");
            }

            if (Float.isNaN(max)) {
                throw new IllegalArgumentException("max is NaN");
            }

            if (Float.compare(min, max) > 0) {
                throw new IllegalArgumentException(min + " > " + max);
            }
        }

        return Math.min(max, Math.max(value, min));
    }
}
