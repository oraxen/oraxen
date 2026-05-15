package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.ResourcePackFormatUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

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
            // Resource pack format 65+ (introduced in 25w31a / 1.21.9) uses top-level min/max fields.
            int min = minFormat > 0 ? minFormat : packFormat;
            int max = maxFormat > 0 ? maxFormat : packFormat;
            pack.addProperty("min_format", min);
            pack.addProperty("max_format", max);
            pack.remove("supported_formats");
        } else if (packFormat >= 18 && minFormat > 0) {
            // 1.20.2-1.21.8 range declaration.
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", minFormat);
            supportedFormats.addProperty("max_inclusive", maxFormat);
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

    public static void mergeOverlayEntriesIntoOutput(List<VirtualFile> output, JsonArray additionalEntries) {
        JsonArray entriesToMerge = new JsonArray();
        if (additionalEntries != null) {
            for (JsonElement entry : additionalEntries) {
                entriesToMerge.add(entry.deepCopy());
            }
        }

        VirtualFile rootMcmeta = null;
        Iterator<VirtualFile> iterator = output.iterator();
        while (iterator.hasNext()) {
            VirtualFile file = iterator.next();
            String path = file.getPath();
            if ("pack.mcmeta".equals(path)) {
                if (rootMcmeta == null) {
                    rootMcmeta = file;
                }
                continue;
            }

            if (!path.endsWith("/pack.mcmeta")) {
                continue;
            }

            addOverlayEntriesFromMcmeta(file, entriesToMerge);
            iterator.remove();
        }

        if (entriesToMerge.isEmpty()) {
            return;
        }

        if (rootMcmeta == null) {
            Logs.logWarning("Cannot merge imported overlay entries because pack.mcmeta was not found in output");
            return;
        }

        JsonObject mcmeta = readVirtualFileJsonObject(rootMcmeta);
        if (mcmeta == null) {
            Logs.logWarning("Failed to merge imported overlay entries into pack.mcmeta");
            return;
        }

        mergeOverlayEntries(mcmeta, entriesToMerge);
        rootMcmeta.setInputStream(new ByteArrayInputStream(GSON.toJson(mcmeta).getBytes(StandardCharsets.UTF_8)));
    }

    private static void addOverlayEntriesFromMcmeta(VirtualFile file, JsonArray entriesToMerge) {
        JsonObject mcmeta = readVirtualFileJsonObject(file);
        if (mcmeta == null || !mcmeta.has("overlays") || !mcmeta.get("overlays").isJsonObject()) {
            return;
        }

        JsonObject overlays = mcmeta.getAsJsonObject("overlays");
        if (!overlays.has("entries") || !overlays.get("entries").isJsonArray()) {
            return;
        }

        for (JsonElement entry : overlays.getAsJsonArray("entries")) {
            entriesToMerge.add(entry.deepCopy());
        }
    }

    @Nullable
    private static JsonObject readVirtualFileJsonObject(VirtualFile file) {
        InputStream inputStream = file.getInputStream();
        if (inputStream == null) {
            return null;
        }

        try {
            byte[] content;
            try (inputStream) {
                content = inputStream.readAllBytes();
            }
            file.setInputStream(new ByteArrayInputStream(content));
            JsonElement parsed = JsonParser.parseString(new String(content, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception exception) {
            if (Settings.DEBUG.toBool()) {
                Logs.logWarning("PackMcmetaUtils.readVirtualFileJsonObject failed for "
                        + file.getPath() + ": " + exception.getMessage());
            }
            return null;
        }
    }

    static void mergeOverlayEntries(JsonObject mcmeta, JsonArray additionalEntries) {
        JsonObject overlays = mcmeta.has("overlays") && mcmeta.get("overlays").isJsonObject()
                ? mcmeta.getAsJsonObject("overlays")
                : new JsonObject();

        JsonArray existingEntries = overlays.has("entries") && overlays.get("entries").isJsonArray()
                ? overlays.getAsJsonArray("entries")
                : new JsonArray();

        // De-duplicate by directory. Existing entries from the user's pack take precedence
        // over freshly-imported ones with the same directory.
        java.util.Set<String> seenDirectories = new java.util.HashSet<>();
        JsonArray mergedEntries = new JsonArray();
        for (JsonElement entry : existingEntries) {
            mergedEntries.add(entry.deepCopy());
            if (entry.isJsonObject() && entry.getAsJsonObject().has("directory")) {
                seenDirectories.add(entry.getAsJsonObject().get("directory").getAsString());
            }
        }
        for (JsonElement entry : additionalEntries) {
            String directory = entry.isJsonObject() && entry.getAsJsonObject().has("directory")
                    ? entry.getAsJsonObject().get("directory").getAsString()
                    : null;
            if (directory != null && !seenDirectories.add(directory)) continue;
            mergedEntries.add(entry.deepCopy());
        }

        overlays.add("entries", mergedEntries);
        mcmeta.add("overlays", overlays);
    }

    /**
     * Updates the pack.mcmeta file for single-pack mode.
     */
    public static void updatePackMcmetaFile(Path mcmetaPath) {
        JsonObject existingMcmeta = readExistingMcmeta(mcmetaPath);

        // Use NMS reflection first (accurate for all versions), fall back to hardcoded mapping
        int packFormat = ResourcePackFormatUtil.getCurrentResourcePackFormat();

        // For modern formats (>=65), createPackMcmeta sets min/max to packFormat.
        // Older formats keep legacy behavior (no explicit supported range in single-pack mode).
        JsonObject mcmeta = createPackMcmeta(packFormat, 0, 0, existingMcmeta);

        writePackMcmeta(mcmetaPath, mcmeta);
    }
}
