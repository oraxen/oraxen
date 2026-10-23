package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.configs.Settings;
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

        int min = minFormat > 0 ? minFormat : packFormat;
        int max = maxFormat > 0 ? maxFormat : packFormat;
        pack.addProperty("pack_format", packFormat);

        if (max >= 65) {
            // Resource pack format 65+ (introduced in 25w31a / 1.21.9) requires top-level min/max fields.
            pack.addProperty("min_format", min);
            pack.addProperty("max_format", max);
        } else {
            pack.remove("min_format");
            pack.remove("max_format");
        }

        if (min < 65 && max >= 18 && min < max) {
            // Keep the legacy range declaration whenever the pack supports an actual range of
            // pre-1.21.9 formats. Older clients ignore min_format/max_format, while 1.21.9+
            // requires both forms for a range crossing the format-65 metadata boundary.
            // A degenerate single-format range (min == max) is skipped: it would only duplicate
            // pack_format and needlessly narrow single-pack acceptance to exactly that format.
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", min);
            supportedFormats.addProperty("max_inclusive", max);
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
        // Native overlays later in the list have higher priority. Local overlays belong
        // above imported snapshots, just like their base resources.
        for (JsonElement entry : existingEntries) mergedEntries.add(entry.deepCopy());

        mergedEntries = normalizeOverlayEntries(mergedEntries);

        overlays.add("entries", mergedEntries);
        mcmeta.add("overlays", overlays);
    }

    /** Normalize the declaration style for the complete overlay list. */
    public static JsonArray normalizeOverlayEntries(JsonArray entries) {
        JsonArray normalized = entries.deepCopy();
        boolean needsLegacyFormats = false;
        for (JsonElement value : normalized) {
            if (!value.isJsonObject()) continue;
            JsonObject entry = value.getAsJsonObject();
            JsonElement formats = entry.get("formats");
            if ((!entry.has("min_format") || !entry.has("max_format")) && formats != null) {
                JsonElement min = null;
                JsonElement max = null;
                if (formats.isJsonObject()) {
                    min = formats.getAsJsonObject().get("min_inclusive");
                    max = formats.getAsJsonObject().get("max_inclusive");
                } else if (formats.isJsonArray() && formats.getAsJsonArray().size() == 2) {
                    min = formats.getAsJsonArray().get(0);
                    max = formats.getAsJsonArray().get(1);
                } else if (formats.isJsonPrimitive()) {
                    min = formats;
                    max = formats;
                }
                if (!entry.has("min_format") && formatMajor(min) != null) entry.add("min_format", min.deepCopy());
                if (!entry.has("max_format") && formatMajor(max) != null) entry.add("max_format", max.deepCopy());
            }
            Integer min = formatMajor(entry.get("min_format"));
            Integer max = formatMajor(entry.get("max_format"));
            if (min != null && max != null && min <= max && min < 65) needsLegacyFormats = true;
        }
        for (JsonElement value : normalized) {
            if (!value.isJsonObject()) continue;
            JsonObject entry = value.getAsJsonObject();
            Integer min = formatMajor(entry.get("min_format"));
            Integer max = formatMajor(entry.get("max_format"));
            if (min == null || max == null || min > max) continue;
            if (needsLegacyFormats) {
                JsonObject formats = new JsonObject();
                formats.addProperty("min_inclusive", min);
                formats.addProperty("max_inclusive", max);
                entry.add("formats", formats);
            } else {
                entry.remove("formats");
            }
        }
        return normalized;
    }

    @Nullable
    private static Integer formatMajor(@Nullable JsonElement format) {
        try {
            if (format == null) return null;
            JsonElement major = format;
            if (format.isJsonArray()) {
                JsonArray pair = format.getAsJsonArray();
                if (pair.isEmpty() || pair.size() > 2) return null;
                major = pair.get(0);
            }
            if (!major.isJsonPrimitive() || !major.getAsJsonPrimitive().isNumber()) return null;
            long value = major.getAsLong();
            return value >= 0 && value <= Integer.MAX_VALUE ? (int) value : null;
        } catch (RuntimeException exception) {
            return null;
        }
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
