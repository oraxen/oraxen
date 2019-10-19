package io.th0rgal.oraxen.utils;

import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Utils {

    public static JsonObject getBlockstateWhenFields(int code) {
        JsonObject whenJson = new JsonObject();
        boolean[] fields = new boolean[6];
        String[] properties = new String[]{"UP", "DOWN", "NORTH", "SOUTH", "WEST", "EAST"};
        for (int i = 0; i < fields.length; i++) {
            int flag = 0x1 << i;
            whenJson.addProperty(properties[fields.length -1 -i], (code & flag) != 0);
        }
        return whenJson;
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
