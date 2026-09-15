package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Utils {

    private Utils() {
    }

    public static Color toColor(String string) {
        return toColor(string, null);
    }

    /**
     * Parses a color from {@code #RRGGBB}/{@code 0xRRGGBB} hex or {@code R,G,B} form.
     * Malformed values do not fail the caller: they log a warning (with the given context,
     * e.g. an item id) and default to white.
     */
    public static Color toColor(String string, @Nullable String context) {
        if (string.startsWith("#") || string.startsWith("0x")) {
            try {
                // Integer#decode understands both the "#" and "0x" prefixes
                return Color.fromRGB(Integer.decode(string));
            } catch (IllegalArgumentException e) {
                return warnInvalidColor(string, context);
            }
        }
        else if (string.contains(",")) {
            String[] newString = string.replace(", ", ",").split(",", 3);
            try {
                int r = Integer.parseInt(newString[0]);
                int g = Integer.parseInt(newString[1]);
                int b = Integer.parseInt(newString[2]);
                return Color.fromRGB(r, g, b);
            } catch (IllegalArgumentException e) {
                return warnInvalidColor(string, context);
            }
        }
        return warnInvalidColor(string, context);
    }

    private static Color warnInvalidColor(String string, @Nullable String context) {
        Logs.logWarning("Invalid color value \"" + string + "\"" + (context != null ? " in " + context : "")
                + ": expected \"#RRGGBB\", \"0xRRGGBB\" or \"R,G,B\". Defaulting to white.");
        return Color.WHITE;
    }

    public static void swingHand(Player player, EquipmentSlot hand) {
        if (hand == EquipmentSlot.HAND) player.swingMainHand();
        else player.swingOffHand();
    }

    public static float customRound(double value, float step) {
        float roundedValue = Math.round(value / step) * step;
        float remainder = (float) (value % step);

        if (remainder > step / 2) roundedValue += step;
        return Float.parseFloat(String.format("%.2f", roundedValue).replace(",", "."));
    }

    public static IntegerRange parseToRange(String string) {
        return parseToRange(string, new IntegerRange(1,1));
    }

    public static IntegerRange parseToRange(String string, IntegerRange integerRange) {
        int minAmount, maxAmount;
        try {
            minAmount = Integer.parseInt(StringUtils.substringBefore(string, ".."));
        } catch (NumberFormatException e) {
            minAmount = 1;
        }

        try {
            maxAmount = Integer.parseInt(StringUtils.substringAfter(string, ".."));
        } catch (NumberFormatException e) {
            maxAmount = Math.max(minAmount, 1);
        }

        minAmount = Math.max(0, minAmount);
        maxAmount = Math.max(0, maxAmount);

        return new IntegerRange(minAmount, maxAmount);
    }

    public static <T> T getOrDefault(List<T> list, int index, T defaultValue) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return defaultValue;
        }
    }
}
