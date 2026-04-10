package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackMcmetaUtils {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PackMcmetaUtils() {
    }

    public static JsonObject createPackMcmeta(int packFormat, int minFormat, int maxFormat, @Nullable JsonObject existingMcmeta) {
        JsonObject root = (existingMcmeta != null) ? existingMcmeta.deepCopy() : new JsonObject();

        JsonObject pack = root.has("pack") && root.get("pack").isJsonObject()
                ? root.getAsJsonObject("pack")
                : new JsonObject();

        if (!pack.has("description")) {
            pack.addProperty("description", "§9§lOraxen §8| §7Extend the Game §7www§8.§7oraxen§8.§7com");
        }

        pack.addProperty("pack_format", packFormat);

        if (packFormat >= 65) {
            // 1.21.11+ metadata requires explicit min/max format fields.
            int min = minFormat > 0 ? minFormat : packFormat;
            int max = maxFormat > 0 ? maxFormat : packFormat;
            pack.addProperty("min_format", min);
            pack.addProperty("max_format", max);
            pack.remove("supported_formats");
        } else if (packFormat >= 18 && minFormat > 0) {
            // Legacy range declaration for pack_format 18..64.
            JsonArray supportedFormats = new JsonArray();
            supportedFormats.add(minFormat);
            supportedFormats.add(maxFormat);
            pack.add("supported_formats", supportedFormats);
            pack.remove("min_format");
            pack.remove("max_format");
        } else {
            // Pre-1.20.2 packs do not use range metadata.
            pack.remove("supported_formats");
            pack.remove("min_format");
            pack.remove("max_format");
        }

        root.add("pack", pack);

        return root;
    }

    public static JsonObject readExistingMcmeta(Path mcmetaPath) {
        if (!mcmetaPath.toFile().exists()) {
            return null;
        }

        try {
            String content = Files.readString(mcmetaPath, StandardCharsets.UTF_8);
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            Logs.logWarning("Failed to read existing pack.mcmeta: " + e.getMessage());
            return null;
        }
    }

    public static boolean writePackMcmeta(Path mcmetaPath, JsonObject mcmeta) {
        try {
            Files.writeString(mcmetaPath, GSON.toJson(mcmeta), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            Logs.logWarning("Failed to write pack.mcmeta: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the pack.mcmeta file for single-pack mode.
     */
    public static void updatePackMcmetaFile(Path mcmetaPath) {
        JsonObject existingMcmeta = readExistingMcmeta(mcmetaPath);

        // Use NMS reflection first (accurate for all versions), fall back to hardcoded mapping
        int packFormat = ResourcePackFormatUtil.getCurrentResourcePackFormat();

        // For modern pack formats (>=65), createPackMcmeta will set min/max to packFormat.
        // For older formats this keeps legacy behavior (no supported range in single-pack mode).
        JsonObject mcmeta = createPackMcmeta(packFormat, 0, 0, existingMcmeta);

        writePackMcmeta(mcmetaPath, mcmeta);
    }
}
