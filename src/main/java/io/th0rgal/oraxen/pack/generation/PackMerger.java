package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Imports complete source layers before resolving their resource priority. */
public class PackMerger {
    private static final Comparator<String> PATH_ORDER = String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
    private static final long MINOR_SCALE = 1L << 31;
    private static final long MAX_FORMAT = Integer.MAX_VALUE * MINOR_SCALE + MINOR_SCALE - 1;
    private final File uploadsDirectory;
    private final List<Layer> layers = new ArrayList<>();
    private final JsonArray mergedOverlayEntries = new JsonArray();
    private final JsonObject languageMetadata = new JsonObject();
    private final JsonObject filterMetadata = new JsonObject();
    private final Map<String, Map<String, byte[]>> overlaySnapshots = new LinkedHashMap<>();
    private boolean convertLegacyItemModels;

    private record Overlay(JsonObject declaration, long min, long max, Map<String, byte[]> files) {}
    private record Layer(Map<String, byte[]> base, List<Overlay> overlays, JsonObject metadata) {}

    public PackMerger(File packFolder) { uploadsDirectory = new File(packFolder, "uploads"); }

    public void setConvertLegacyItemModels(boolean convertLegacyItemModels) {
        this.convertLegacyItemModels = convertLegacyItemModels;
    }

    public List<VirtualFile> mergeUploadedPacks() {
        layers.clear();
        languageMetadata.entrySet().clear();
        filterMetadata.entrySet().clear();
        if (!uploadsDirectory.exists()) uploadsDirectory.mkdirs();
        File[] packs = uploadsDirectory.listFiles(file -> file.isDirectory() || file.getName().toLowerCase(Locale.ROOT).endsWith(".zip"));
        if (packs != null) {
            Arrays.sort(packs, Comparator.comparing(File::getName, PATH_ORDER));
            for (File pack : packs) importPack(pack, true);
        }
        return buildFiles();
    }

    /** ModelEngine has higher priority than uploaded packs. */
    public List<VirtualFile> mergeModelEnginePack(File pack) {
        importPack(pack, false);
        return buildFiles();
    }

    public JsonArray getMergedOverlayEntries() { return PackMcmetaUtils.normalizeOverlayEntries(mergedOverlayEntries); }
    public JsonObject getMergedLanguageMetadata() { return languageMetadata.deepCopy(); }
    public JsonObject getMergedFilterMetadata() { return filterMetadata.deepCopy(); }

    private void importPack(File pack, boolean includeIcon) {
        try {
            Map<String, byte[]> entries = readEntries(pack);
            importEntries(entries, includeIcon, true);
        } catch (IOException exception) {
            Logs.logError("Failed to import resource pack " + pack.getName());
            exception.printStackTrace();
        }
    }

    private void importEntries(Map<String, byte[]> entries, boolean includeIcon, boolean removeCoreShaders) {
            List<String> roots = roots(entries);
            for (String root : roots) {
                JsonObject metadata = object(entries.get(root + "pack.mcmeta"));
                if (metadata == null) metadata = new JsonObject();
                List<Overlay> overlays = new ArrayList<>();
                if (metadata.has("overlays") && metadata.get("overlays").isJsonObject()) {
                    JsonElement declarations = metadata.getAsJsonObject("overlays").get("entries");
                    if (declarations != null && declarations.isJsonArray()) {
                        for (JsonElement value : declarations.getAsJsonArray()) {
                            if (!value.isJsonObject()) continue;
                            JsonObject declaration = value.getAsJsonObject();
                            if (!declaration.has("directory") || !declaration.get("directory").isJsonPrimitive()) continue;
                            String directory = declaration.get("directory").getAsString();
                            if (!safePath(directory) || directory.contains("/")) continue;
                            long[] range = range(declaration);
                            if (range == null || range[0] < 0 || range[0] > range[1]) continue;
                            overlays.add(new Overlay(declaration.deepCopy(), range[0], range[1], collect(entries, root + directory + "/", removeCoreShaders)));
                        }
                    }
                }
                Map<String, byte[]> base = collect(entries, root, removeCoreShaders);
                if (includeIcon && entries.containsKey(root + "pack.png")) base.put("pack.png", entries.get(root + "pack.png"));
                layers.add(new Layer(base, overlays, metadata));
                if (metadata.has("language") && metadata.get("language").isJsonObject())
                    metadata.getAsJsonObject("language").entrySet().forEach(entry ->
                            languageMetadata.add(entry.getKey(), entry.getValue().deepCopy()));
                if (metadata.has("filter") && metadata.get("filter").isJsonObject()) {
                    JsonElement blocks = metadata.getAsJsonObject("filter").get("block");
                    if (blocks != null && blocks.isJsonArray()) {
                        if (!filterMetadata.has("block")) filterMetadata.add("block", new JsonArray());
                        blocks.getAsJsonArray().forEach(block -> filterMetadata.getAsJsonArray("block").add(block.deepCopy()));
                    }
                }
            }
    }

    private Map<String, byte[]> readEntries(File pack) throws IOException {
        Map<String, byte[]> entries = new TreeMap<>(PATH_ORDER);
        if (pack.isDirectory()) {
            Path base = pack.toPath();
            try (var files = Files.walk(base)) {
                for (Path path : files.filter(Files::isRegularFile).sorted().toList()) {
                    String name = base.relativize(path).toString().replace('\\', '/');
                    if (safePath(name)) entries.put(name, Files.readAllBytes(path));
                }
            }
        } else {
            try (ZipInputStream zip = new ZipInputStream(new FileInputStream(pack), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName().replace('\\', '/');
                    if (!entry.isDirectory() && safePath(name)) {
                        byte[] bytes = zip.readAllBytes();
                        // Duplicate ZIP members have no reliable priority. Choose by bytes, independent of entry order.
                        entries.merge(name, bytes, (first, second) -> Arrays.compareUnsigned(first, second) >= 0 ? first : second);
                    }
                    zip.closeEntry();
                }
            }
        }
        return entries;
    }

    private static boolean safePath(String path) {
        return !path.isEmpty() && !path.startsWith("/") && !path.contains(":")
                && Arrays.stream(path.split("/", -1)).noneMatch(part -> part.equals("..") || part.equals(".") || part.equals("__MACOSX"))
                && !path.endsWith("/.DS_Store") && !path.equals(".DS_Store")
                && !path.endsWith("/Thumbs.db") && !path.endsWith("/desktop.ini");
    }

    private List<String> roots(Map<String, byte[]> entries) {
        Set<String> roots = new TreeSet<>(PATH_ORDER);
        Set<String> declaredOverlays = new HashSet<>();
        for (var entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.equals("pack.mcmeta") || name.endsWith("/pack.mcmeta")) {
                String root = name.substring(0, name.length() - "pack.mcmeta".length());
                roots.add(root);
                JsonObject metadata = object(entry.getValue());
                if (metadata == null || !metadata.has("overlays") || !metadata.get("overlays").isJsonObject()) continue;
                JsonElement declarations = metadata.getAsJsonObject("overlays").get("entries");
                if (declarations == null || !declarations.isJsonArray()) continue;
                for (JsonElement value : declarations.getAsJsonArray()) {
                    if (!value.isJsonObject()) continue;
                    JsonElement directory = value.getAsJsonObject().get("directory");
                    if (directory != null && directory.isJsonPrimitive() && directory.getAsJsonPrimitive().isString()
                            && safePath(directory.getAsString()) && !directory.getAsString().contains("/"))
                        declaredOverlays.add(root + directory.getAsString() + "/");
                }
            }
            int index = name.startsWith("assets/") ? 0 : name.indexOf("/assets/") + 1;
            if (index >= 0 && name.startsWith("assets/", index)) roots.add(name.substring(0, index));
        }
        roots.removeAll(declaredOverlays);
        return new ArrayList<>(roots);
    }

    private Map<String, byte[]> collect(Map<String, byte[]> entries, String prefix, boolean removeCoreShaders) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (var entry : entries.entrySet()) {
            if (!entry.getKey().startsWith(prefix + "assets/")) continue;
            String path = entry.getKey().substring(prefix.length());
            if (removeCoreShaders && Boolean.TRUE.equals(Settings.REMOVE_CORE_SHADERS_FROM_IMPORTED_PACKS.getValue()) && PackFileCollector.isCoreShaderPath(path)) continue;
            result.put(path, entry.getValue());
        }
        return result;
    }

    /** Compose generated and local resources, including their native overlay replacement rules. */
    public List<VirtualFile> mergeWithHigherPriorityFiles(List<VirtualFile> higherFiles) {
        return mergeWithHigherPriorityFiles(higherFiles, false);
    }

    /**
     * Compose generated and local resources, optionally retaining overlay files declared by
     * the local pack metadata at their original paths. The compiled snapshots still include
     * those overlays so imported pack priority and additive-resource composition are unchanged.
     */
    public List<VirtualFile> mergeWithHigherPriorityFiles(List<VirtualFile> higherFiles, boolean preserveNativeOverlays) {
        Map<String, byte[]> entries = new TreeMap<>(PATH_ORDER);
        for (VirtualFile file : higherFiles) {
            try (InputStream input = file.getInputStream()) {
                if (input == null) continue;
                byte[] bytes = input.readAllBytes();
                file.setInputStream(new ByteArrayInputStream(bytes));
                byte[] first = entries.get(file.getPath());
                if (first == null) entries.put(file.getPath(), bytes);
                else if (PackAssetMerger.isAdditivePath(file.getPath()) && !validResource(file.getPath(), object(first))
                        && validResource(file.getPath(), object(bytes))) entries.put(file.getPath(), bytes);
                else if (shouldMergeNative(file.getPath())) {
                    JsonObject high = object(first);
                    JsonObject low = object(bytes);
                    if (high == null && low != null) entries.put(file.getPath(), bytes);
                    else if (high != null && low != null) entries.put(file.getPath(), PackAssetMerger.mergeJson(file.getPath(), low, high).toString().getBytes(StandardCharsets.UTF_8));
                } else if (Boolean.TRUE.equals(Settings.MERGE_DUPLICATES.toBool())) {
                    JsonObject high = object(first);
                    JsonObject low = object(bytes);
                    if (high != null && low != null) {
                        try {
                            if (file.getPath().matches("assets/[^/]+/items/.+\\.json"))
                                entries.put(file.getPath(), ItemModelMerger.mergeDefinition(high, low, file.getPath()).toString().getBytes(StandardCharsets.UTF_8));
                            else if (file.getPath().matches("assets/[^/]+/models/item/.+\\.json"))
                                entries.put(file.getPath(), ItemModelMerger.mergeLegacy(high, low, file.getPath()).toString().getBytes(StandardCharsets.UTF_8));
                        } catch (RuntimeException exception) {
                            Logs.logWarning("Failed to merge duplicate item resource " + file.getPath() + ". Keeping the higher priority file.");
                        }
                    }
                }
            } catch (IOException exception) {
                Logs.logError("Failed to read resource " + file.getPath());
                exception.printStackTrace();
            }
        }
        Map<String, byte[]> preservedOverlayFiles = preserveNativeOverlays
                ? collectDeclaredOverlayFiles(entries)
                : Collections.emptyMap();
        importEntries(entries, true, false);
        List<VirtualFile> result = buildFiles();
        result.addAll(virtualFiles(preservedOverlayFiles));
        return result;
    }

    private List<VirtualFile> buildFiles() {
        while (!mergedOverlayEntries.isEmpty()) mergedOverlayEntries.remove(0);
        overlaySnapshots.clear();
        Map<String, byte[]> base = resolve(null);
        TreeSet<Long> boundaries = new TreeSet<>();
        for (Layer layer : layers) for (Overlay overlay : layer.overlays) {
            boundaries.add(overlay.min);
            if (overlay.max < MAX_FORMAT) boundaries.add(overlay.max + 1);
        }
        List<Long> points = new ArrayList<>(boundaries);
        Map<String, byte[]> result = new LinkedHashMap<>(base);
        Set<String> usedDirectories = new HashSet<>();
        for (Layer layer : layers) {
            for (Overlay overlay : layer.overlays) {
                usedDirectories.add(overlay.declaration.get("directory").getAsString());
            }
        }
        int nextOverlayDirectory = 0;
        for (int index = 0; index < points.size(); index++) {
            long min = points.get(index);
            long max = index + 1 < points.size() ? points.get(index + 1) - 1 : MAX_FORMAT;
            if (layers.stream().noneMatch(layer -> layer.overlays.stream().anyMatch(overlay -> overlay.min <= min && min <= overlay.max))) continue;
            Map<String, byte[]> snapshot = resolve(min);
            // Missing files still inherit the base pack. Mask removed additive
            // resources and metadata with an empty resource in the overlay.
            base.forEach((path, bytes) -> {
                if ((PackAssetMerger.isAdditivePath(path) || path.endsWith(".png.mcmeta")) && !snapshot.containsKey(path))
                    snapshot.put(path, emptyResource(path).toString().getBytes(StandardCharsets.UTF_8));
            });
            String directory;
            do {
                directory = "oraxen_import_" + nextOverlayDirectory++;
            } while (usedDirectories.contains(directory));
            usedDirectories.add(directory);
            overlaySnapshots.put(directory, snapshot);
            JsonObject declaration = new JsonObject();
            declaration.addProperty("directory", directory);
            addRange(declaration, min, max);
            mergedOverlayEntries.add(declaration);
            String snapshotDirectory = directory;
            snapshot.forEach((path, bytes) -> result.put(snapshotDirectory + "/" + path, bytes));
        }
        return virtualFiles(result);
    }

    private static Map<String, byte[]> collectDeclaredOverlayFiles(Map<String, byte[]> entries) {
        JsonObject metadata = object(entries.get("pack.mcmeta"));
        if (metadata == null || !metadata.has("overlays") || !metadata.get("overlays").isJsonObject()) {
            return Collections.emptyMap();
        }

        JsonElement declarations = metadata.getAsJsonObject("overlays").get("entries");
        if (declarations == null || !declarations.isJsonArray()) {
            return Collections.emptyMap();
        }

        Set<String> directories = new LinkedHashSet<>();
        for (JsonElement value : declarations.getAsJsonArray()) {
            if (!value.isJsonObject()) continue;
            JsonElement directoryValue = value.getAsJsonObject().get("directory");
            if (directoryValue == null || !directoryValue.isJsonPrimitive()
                    || !directoryValue.getAsJsonPrimitive().isString()) continue;
            String directory = directoryValue.getAsString();
            if (safePath(directory) && !directory.contains("/")) directories.add(directory);
        }

        Map<String, byte[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            for (String directory : directories) {
                if (entry.getKey().startsWith(directory + "/")) {
                    result.putIfAbsent(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }
        return result;
    }

    private Map<String, byte[]> resolve(Long version) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (Layer layer : layers) {
            applyFilters(result, layer.metadata);
            Map<String, byte[]> active = new LinkedHashMap<>(layer.base);
            if (version != null) for (Overlay overlay : layer.overlays)
                if (overlay.min <= version && version <= overlay.max) replaceAll(active, overlay.files);
            if (convertLegacyItemModels)
                active = ItemModelMerger.convertLegacyLayer(active, result);
            mergeAll(result, active);
        }
        return result;
    }

    private static void applyFilters(Map<String, byte[]> files, JsonObject metadata) {
        if (!metadata.has("filter") || !metadata.get("filter").isJsonObject()) return;
        JsonElement blocks = metadata.getAsJsonObject("filter").get("block");
        if (blocks == null || !blocks.isJsonArray()) return;
        for (JsonElement block : blocks.getAsJsonArray()) {
            if (!block.isJsonObject()) continue;
            try {
                JsonObject rule = block.getAsJsonObject();
                Pattern namespace = Pattern.compile(rule.has("namespace") ? rule.get("namespace").getAsString() : ".*");
                Pattern path = Pattern.compile(rule.has("path") ? rule.get("path").getAsString() : ".*");
                files.keySet().removeIf(file -> {
                    String[] parts = file.split("/", 3);
                    return parts.length == 3 && parts[0].equals("assets") && namespace.matcher(parts[1]).matches() && path.matcher(parts[2]).matches();
                });
            } catch (RuntimeException exception) {
                Logs.logWarning("Invalid resource pack filter was ignored");
            }
        }
    }

    private static void replaceAll(Map<String, byte[]> lower, Map<String, byte[]> higher) {
        higher.forEach((path, bytes) -> {
            if (!path.endsWith(".mcmeta")) lower.remove(path + ".mcmeta");
            lower.put(path, bytes);
        });
    }

    private static void mergeAll(Map<String, byte[]> lower, Map<String, byte[]> higher) {
        higher.forEach((path, bytes) -> {
            if (!path.endsWith(".mcmeta")) lower.remove(path + ".mcmeta");
            if (PackAssetMerger.isAdditivePath(path) && !validResource(path, object(bytes))) return;
            if (shouldMergeNative(path)) {
                JsonObject high = object(bytes);
                JsonObject low = object(lower.get(path));
                if (!validResource(path, high)) return;
                if (high != null && low != null) bytes = PackAssetMerger.mergeJson(path, low, high).toString().getBytes(StandardCharsets.UTF_8);
            } else if (Boolean.TRUE.equals(Settings.MERGE_DUPLICATES.toBool())) {
                JsonObject high = object(bytes);
                JsonObject low = object(lower.get(path));
                if (high != null && low != null) {
                    try {
                        if (path.matches("assets/[^/]+/items/.+\\.json"))
                            bytes = ItemModelMerger.mergeDefinition(high, low, path).toString().getBytes(StandardCharsets.UTF_8);
                        else if (path.matches("assets/[^/]+/models/item/.+\\.json"))
                            bytes = ItemModelMerger.mergeLegacy(high, low, path).toString().getBytes(StandardCharsets.UTF_8);
                    } catch (RuntimeException exception) {
                        Logs.logWarning("Failed to merge duplicate item resource " + path + ". Keeping the higher priority file.");
                    }
                }
            }
            lower.put(path, bytes);
        });
    }

    private static boolean shouldMergeNative(String path) {
        return PackAssetMerger.isAdditivePath(path)
                && (!path.contains("/font/") || !Boolean.FALSE.equals(Settings.MERGE_DUPLICATE_FONTS.toBool()));
    }

    private static boolean validResource(String path, JsonObject value) {
        if (value == null) return false;
        if (path.contains("/lang/")) return value.entrySet().stream().allMatch(entry -> entry.getValue().isJsonPrimitive());
        if (path.contains("/font/")) return value.has("providers") && value.get("providers").isJsonArray()
                && value.getAsJsonArray("providers").asList().stream().allMatch(JsonElement::isJsonObject);
        if (path.contains("/atlases/")) return value.has("sources") && value.get("sources").isJsonArray()
                && value.getAsJsonArray("sources").asList().stream().allMatch(JsonElement::isJsonObject);
        if (path.contains("/blockstates/")) return value.has("variants") && value.get("variants").isJsonObject()
                || value.has("multipart") && value.get("multipart").isJsonArray();
        return true;
    }

    private static JsonObject emptyResource(String path) {
        JsonObject value = new JsonObject();
        if (path.contains("/font/")) value.add("providers", new JsonArray());
        else if (path.contains("/atlases/")) value.add("sources", new JsonArray());
        else if (path.contains("/blockstates/")) value.add("variants", new JsonObject());
        return value;
    }

    private static List<VirtualFile> virtualFiles(Map<String, byte[]> files) {
        List<VirtualFile> result = new ArrayList<>();
        files.forEach((path, bytes) -> {
            int slash = path.lastIndexOf('/');
            result.add(new VirtualFile(slash < 0 ? "" : path.substring(0, slash), path.substring(slash + 1), new ByteArrayInputStream(bytes)));
        });
        return result;
    }

    private static JsonObject object(byte[] bytes) {
        if (bytes == null) return null;
        try {
            JsonElement value = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            return value.isJsonObject() ? value.getAsJsonObject() : null;
        } catch (RuntimeException exception) { return null; }
    }

    private static long format(JsonElement value, boolean upper) {
        if (value.isJsonArray()) {
            JsonArray pair = value.getAsJsonArray();
            if (pair.isEmpty() || pair.size() > 2) throw new IllegalArgumentException("Invalid format pair");
            long major = pair.get(0).getAsLong();
            long minor = pair.size() > 1 ? pair.get(1).getAsLong() : upper ? MINOR_SCALE - 1 : 0;
            if (major < 0 || major > Integer.MAX_VALUE || minor < 0 || minor >= MINOR_SCALE)
                throw new IllegalArgumentException("Format is outside the supported integer range");
            return major * MINOR_SCALE + minor;
        }
        long major = value.getAsLong();
        if (major < 0 || major > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid major format");
        return major * MINOR_SCALE + (upper ? MINOR_SCALE - 1 : 0);
    }

    private static long[] range(JsonObject declaration) {
        try {
            if (declaration.has("min_format") && declaration.has("max_format"))
                return new long[]{format(declaration.get("min_format"), false), format(declaration.get("max_format"), true)};
            JsonElement formats = declaration.get("formats");
            if (formats == null) return null;
            if (formats.isJsonObject()) {
                JsonObject range = formats.getAsJsonObject();
                return new long[]{format(range.get("min_inclusive"), false), format(range.get("max_inclusive"), true)};
            }
            if (formats.isJsonArray()) {
                JsonArray range = formats.getAsJsonArray();
                return new long[]{format(range.get(0), false), format(range.get(1), true)};
            }
            long value = format(formats, false);
            return new long[]{value, value + MINOR_SCALE - 1};
        } catch (RuntimeException exception) { return null; }
    }

    private static JsonElement formatValue(long value, boolean upper) {
        if (value % MINOR_SCALE == (upper ? MINOR_SCALE - 1 : 0)) return new JsonPrimitive(value / MINOR_SCALE);
        JsonArray pair = new JsonArray();
        pair.add(value / MINOR_SCALE);
        pair.add(value % MINOR_SCALE);
        return pair;
    }

    private static void addRange(JsonObject declaration, long min, long max) {
        declaration.add("min_format", formatValue(min, false));
        declaration.add("max_format", formatValue(max, true));
        if (min / MINOR_SCALE < 65) {
            JsonObject legacy = new JsonObject();
            legacy.addProperty("min_inclusive", min / MINOR_SCALE);
            legacy.addProperty("max_inclusive", max / MINOR_SCALE);
            declaration.add("formats", legacy);
        }
    }
}
