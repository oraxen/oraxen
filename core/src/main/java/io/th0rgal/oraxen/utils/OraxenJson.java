package io.th0rgal.oraxen.utils;

import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class OraxenJson {
    public static boolean isValidJson(File file) {
        try (FileReader reader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(reader)) {
            JsonParser.parseReader(reader);
            return true;
        } catch (IOException e) {
            Logs.logError("Error loading JSON configuration file: " + file.getPath());
            Logs.logError("Ensure that your config is formatted correctly:");
            Logs.logWarning(e.getMessage());
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
