package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

    public static int getCode(MultipleFacing blockData) {
        final String[] properties = new String[]{"EAST", "WEST", "SOUTH", "NORTH", "DOWN", "UP"};
        //for (blockData.getFaces()
        return  0;
    }

    public static JsonObject getBlockstateWhenFields(int code) {
        JsonObject whenJson = new JsonObject();
        final String[] properties = new String[]{"up", "down", "north", "south", "west", "east"};
        for (int i = 0; i < properties.length; i++) {
            int flag = 0x1 << i;
            whenJson.addProperty(properties[properties.length - 1 - i], (code & flag) != 0);
        }
        return whenJson;
    }

    public static void setBlockFacing(MultipleFacing blockData, int code) {
        final String[] properties = new String[]{"EAST", "WEST", "SOUTH", "NORTH", "DOWN", "UP"};
        for (int i = 0; i < properties.length; i++)
            blockData.setFace(BlockFace.valueOf(properties[i]), ((code & (0x1 << i)) != 0));
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
