package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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

        if (packFormat >= 18 && minFormat > 0) {
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", minFormat);
            supportedFormats.addProperty("max_inclusive", maxFormat);
            pack.add("supported_formats", supportedFormats);
        } else {
            pack.remove("supported_formats");
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
     * Returns the supported_formats range for the server's Minecraft version.
     * These ranges match the definitions in {@link PackVersionManager#definePackVersions()}
     * and ensure the single-pack pack.mcmeta has accurate metadata.
     *
     * @return int[2] with {min_inclusive, max_inclusive}, or {0,0} if unsupported
     */
    public static int[] getSupportedFormatRange(MinecraftVersion serverVersion) {
        // Ordered from newest to oldest — first match wins
        if (serverVersion.isAtLeast(new MinecraftVersion("1.21.4"))) return new int[]{46, 999};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.21.2"))) return new int[]{42, 45};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.21")))   return new int[]{34, 41};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.20.5"))) return new int[]{32, 33};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.20.3"))) return new int[]{22, 31};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.20.2"))) return new int[]{18, 21};
        if (serverVersion.isAtLeast(new MinecraftVersion("1.20")))   return new int[]{15, 17};

        // Pre-1.20: supported_formats field not used (introduced in 1.20.2 / format 18)
        return new int[]{0, 0};
    }

    public static void updatePackMcmetaFile(Path mcmetaPath, MinecraftVersion serverVersion) {
        JsonObject existingMcmeta = readExistingMcmeta(mcmetaPath);
        
        int packFormat = ResourcePackFormatUtil.getPackFormatForVersion(serverVersion);
        int[] formatRange = getSupportedFormatRange(serverVersion);
        
        JsonObject mcmeta = createPackMcmeta(packFormat, formatRange[0], formatRange[1], existingMcmeta);
        
        writePackMcmeta(mcmetaPath, mcmeta);
    }
}
