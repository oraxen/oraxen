package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Utils {

    public static int getCode(MultipleFacing blockData) {
        final List<BlockFace> properties = Arrays.asList
                (BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP);
        int sum = 0;
        for (BlockFace blockFace : blockData.getFaces())
            sum += (int)Math.pow(2, properties.indexOf(blockFace));
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
        final BlockFace[] properties = new BlockFace[]
                {BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.UP};
        for (int i = 0; i < properties.length; i++) {
            blockData.setFace(properties[i], (code & 0x1 << i) != 0);
        }
    }

    public static void writeStringToFile(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
