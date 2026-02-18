package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

        // Only add supported_formats when explicitly requested (minFormat > 0).
        // When minFormat is 0, leave any existing supported_formats untouched
        // so user customizations in pack.mcmeta are preserved.
        if (packFormat >= 18 && minFormat > 0) {
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", minFormat);
            supportedFormats.addProperty("max_inclusive", maxFormat);
            pack.add("supported_formats", supportedFormats);
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

    public static void writePackMcmeta(Path mcmetaPath, JsonObject mcmeta) {
        try {
            Files.writeString(mcmetaPath, GSON.toJson(mcmeta), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logs.logWarning("Failed to write pack.mcmeta: " + e.getMessage());
        }
    }

    /**
     * Updates the pack.mcmeta file for single-pack mode.
     * Does NOT add supported_formats — single-pack mode targets only
     * the server's version. Adding supported_formats would narrow the
     * declared range and cause ViaVersion clients to see it as incompatible.
     */
    public static void updatePackMcmetaFile(Path mcmetaPath) {
        JsonObject existingMcmeta = readExistingMcmeta(mcmetaPath);

        // Use NMS reflection first (accurate for all versions), fall back to hardcoded mapping
        int packFormat = ResourcePackFormatUtil.getCurrentResourcePackFormat();

        // Pass 0 for minFormat to skip supported_formats in single-pack mode
        JsonObject mcmeta = createPackMcmeta(packFormat, 0, 0, existingMcmeta);

        writePackMcmeta(mcmetaPath, mcmeta);
    }
}
