package io.th0rgal.oraxen.utils;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    private Utils() {
    }

    public static Color toColor(String string) {
        if (string.startsWith("#") || string.startsWith("0x")) {
            try {
                // Integer#decode understands both the "#" and "0x" prefixes
                return Color.fromRGB(Integer.decode(string));
            } catch (IllegalArgumentException e) {
                return Color.WHITE;
            }
        }
        else if (string.contains(",")) {
            String[] newString = string.replace(", ", ",").split(",", 3);
            try {
                int r = Integer.parseInt(newString[0]);
                int g = Integer.parseInt(newString[1]);
                int b = Integer.parseInt(newString[2]);
                return Color.fromRGB(r, g, b);
            } catch (NumberFormatException e) {
                return Color.WHITE;
            }
        }
        return Color.WHITE;
    }

    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length());
        } else {
            return string;
        }
    }

    public static List<String> toLowercaseList(final String... values) {
        final ArrayList<String> list = new ArrayList<>();
        for (final String value : values)
            list.add(value.toLowerCase(Locale.ROOT));
        return list;
    }

    public static String[] toLowercase(final String... values) {
        for (int index = 0; index < values.length; index++)
            values[index] = values[index].toLowerCase(Locale.ROOT);
        return values;
    }

    public static long getVersion(final String format) {
        return Long.parseLong(OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format)));
    }

    public static char firstEmpty(Map<String, Character> map, int min) {
        List<Integer> newMap = map.values().stream().map(c -> (int) c).sorted().toList();
        while (newMap.contains(min)) min++;
        return (char) min;
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

    public static <T> T getOrNull(List<T> list, int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public static <T> T getOrDefault(List<T> list, int index, T defaultValue) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            return defaultValue;
        }
    }
}
