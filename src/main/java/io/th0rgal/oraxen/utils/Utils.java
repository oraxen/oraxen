package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.settings.Plugin;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final char COLOR_CHAR = ChatColor.COLOR_CHAR;
    private static final boolean HEX_SUPPORTED = (boolean) Plugin.HEX_SUPPORTED.getValue();

    public static String handleColors(String message) {
        return HEX_SUPPORTED && message.contains(Plugin.HEX_PREFIX.toLegacyString()) && message.contains(Plugin.HEX_SUFFIX.toLegacyString())
                ? ChatColor.translateAlternateColorCodes('&',
                translateHexColorCodes(Plugin.HEX_PREFIX.toLegacyString(), message, Plugin.HEX_SUFFIX.toLegacyString()))
                : ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String handleColors(String message, boolean forceLegacyTranslate) {
        return (forceLegacyTranslate)
                ? ChatColor.translateAlternateColorCodes('&', message)
                : handleColors(message);
    }

    // all credits go to https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-3867804
    private static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    public static List<String> toLowercaseList(String... values) {
        ArrayList<String> list = new ArrayList<>();
        for (String value : values)
            list.add(value.toLowerCase());
        return list;
    }

    public static String[] toLowercase(String... values) {
        for (int index = 0; index < values.length; index++)
            values[index] = values[index].toLowerCase();
        return values;
    }

    public static long getVersion(String format) {
        return Long.parseLong(OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format)));
    }

    public static int getCode(MultipleFacing blockData) {
        final List<BlockFace> properties = Arrays
                .asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP);
        int sum = 0;
        for (BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, properties.indexOf(blockFace));
        return sum;
    }

    public static JsonObject getBlockstateWhenFields(int code) {
        JsonObject whenJson = new JsonObject();
        final String[] properties = new String[]{"up", "down", "north", "south", "west", "east"};
        for (int i = 0; i < properties.length; i++)
            whenJson.addProperty(properties[properties.length - 1 - i], (code & 0x1 << i) != 0);
        return whenJson;
    }

    public static void setBlockFacing(MultipleFacing blockData, int code) {
        final BlockFace[] properties = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH,
                BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP};
        for (int i = 0; i < properties.length; i++) {
            blockData.setFace(properties[i], (code & 0x1 << i) != 0);
        }
    }

    public static void writeStringToFile(File file, String content) {
        try {
            file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}