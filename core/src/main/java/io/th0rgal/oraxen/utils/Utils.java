package io.th0rgal.oraxen.utils;

import dev.jorel.commandapi.wrappers.IntegerRange;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            return Color.fromRGB(Integer.parseInt(string.substring(1), 16));
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

    public static String getParentDirs(String string) {
        return Utils.getStringBeforeLastInSplit(string, "/");
    }

    public static String removeParentDirs(String s) {
        return Utils.getLastStringInSplit(s, "/");
    }

    /**
     * Removes extension AND parent directories
     * @param s The path or filename including extension
     * @return Purely the filename, no extension or path
     */
    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) filename = s;
        else filename = s.substring(lastSeparatorIndex + 1);

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    public static String removeExtensionOnly(String s) {
        // Remove the extension.
        int extensionIndex = s.lastIndexOf(".");
        if (extensionIndex == -1)
            return s;

        return s.substring(0, extensionIndex);
    }

    public static String getFileNameOnly(String s) {
        s = Utils.removeParentDirs(s);
        s = Utils.removeExtensionOnly(s);
        return s;
    }

    public static String getLastStringInSplit(String string, String split) {
        String[] splitString = string.split(split);
        return splitString.length > 0 ? splitString[splitString.length - 1] : "";
    }

    public static String getStringBeforeLastInSplit(String string, String split) {
        String[] splitString = string.split(split);
        if (splitString.length == 0) return string;
        else return string.replace(splitString[splitString.length - 1], "");
    }

    public static void writeStringToFile(final File file, final String content) {
        file.getParentFile().mkdirs();
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
}
