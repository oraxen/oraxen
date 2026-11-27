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
    private final PackExclusions exclusions;
    private final List<Layer> layers = new ArrayList<>();
    private final JsonArray mergedOverlayEntries = new JsonArray();
    private final JsonObject languageMetadata = new JsonObject();
    private final JsonObject filterMetadata = new JsonObject();
    private final JsonObject sodiumMetadata = new JsonObject();
    private final Map<String, Map<String, byte[]>> overlaySnapshots = new LinkedHashMap<>();
    private boolean convertLegacyItemModels;
    private boolean shaderWarningShown;

    private record Overlay(JsonObject declaration, long min, long max, Map<String, byte[]> files) {}
    private record Layer(Map<String, byte[]> base, List<Overlay> overlays, JsonObject metadata, String source) {}
    private record PackOverride(String path, String previous, String replacement) {}

    public PackMerger(File packFolder) { this(packFolder, PackExclusions.load()); }

    PackMerger(File packFolder, PackExclusions exclusions) {
        uploadsDirectory = new File(packFolder, "uploads");
        this.exclusions = exclusions;
    }

    public void setConvertLegacyItemModels(boolean convertLegacyItemModels) {
        this.convertLegacyItemModels = convertLegacyItemModels;
    }

    public List<VirtualFile> mergeUploadedPacks() {
        layers.clear();
        languageMetadata.entrySet().clear();
        filterMetadata.entrySet().clear();
        sodiumMetadata.entrySet().clear();
        if (!uploadsDirectory.exists()) uploadsDirectory.mkdirs();
        File[] packs = uploadsDirectory.listFiles(file -> file.isDirectory() || file.getName().toLowerCase(Locale.ROOT).endsWith(".zip"));
        if (packs != null) {
            Arrays.sort(packs, Comparator.comparing(File::getName, PATH_ORDER));
            for (File pack : packs) importPack(pack, true);
        }
        Set<PackOverride> overrides = Boolean.TRUE.equals(Settings.DEBUG.toBool()) ? new LinkedHashSet<>() : null;
        List<VirtualFile> result = buildFiles(overrides);
        if (overrides != null) for (PackOverride override : overrides) {
            Logs.logInfo("[Debug] Resource pack override '" + logLabel(override.path()) + "' from '"
                    + logLabel(override.previous()) + "' replaced by '" + logLabel(override.replacement()) + "'.");
        }
        return result;
    }

    private static String logLabel(String value) { return value.replace("\\", "\\\\").replace("<", "\\<").replace("\r", "\\r").replace("\n", "\\n"); }

    /** ModelEngine has higher priority than uploaded packs. */
    public List<VirtualFile> mergeModelEnginePack(File pack) {
        importPack(pack, false);
        return buildFiles(null);
    }

    public JsonArray getMergedOverlayEntries() { return PackMcmetaUtils.normalizeOverlayEntries(mergedOverlayEntries); }
    public JsonObject getMergedLanguageMetadata() { return languageMetadata.deepCopy(); }
    public JsonObject getMergedFilterMetadata() { return filterMetadata.deepCopy(); }
    public JsonObject getMergedSodiumMetadata() { return sodiumMetadata.deepCopy(); }

    private void importPack(File pack, boolean includeIcon) {
        try {
            Map<String, byte[]> entries = readEntries(pack);
            importEntries(entries, includeIcon, true, pack.getName());
        } catch (IOException | IllegalArgumentException exception) {
            Logs.logError("Failed to import resource pack " + pack.getName());
            Logs.debug(exception);
        }
    }

    private void importEntries(Map<String, byte[]> entries, boolean includeIcon, boolean applyExclusions, String source) {
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
                            overlays.add(new Overlay(declaration.deepCopy(), range[0], range[1], collect(entries, root + directory + "/", applyExclusions)));
                        }
                    }
                }
                Map<String, byte[]> base = collect(entries, root, applyExclusions);
                if (includeIcon && (!applyExclusions || !exclusions.matches("pack.png")) && entries.containsKey(root + "pack.png")) base.put("pack.png", entries.get(root + "pack.png"));
                layers.add(new Layer(base, overlays, metadata,
                        root.isEmpty() ? source : source + "/" + root.substring(0, root.length() - 1)));
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
                mergeSodiumMetadata(metadata.get("sodium"));
            }
    }

    private void mergeSodiumMetadata(JsonElement section) {
        if (section == null || !section.isJsonObject()) return;
        for (var entry : section.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            boolean ignoredShaders = key.equals("ignored_shaders");
            if (!value.isJsonArray()) {
                if (!ignoredShaders) sodiumMetadata.add(key, value.deepCopy());
                continue;
            }

            JsonElement previous = sodiumMetadata.get(key);
            JsonArray merged = previous != null && previous.isJsonArray() ? previous.getAsJsonArray() : new JsonArray();
            Set<JsonElement> seen = new HashSet<>();
            merged.forEach(seen::add);
            for (JsonElement element : value.getAsJsonArray()) {
                if (ignoredShaders && (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())) continue;
                if (seen.add(element)) merged.add(element.deepCopy());
            }
            sodiumMetadata.add(key, merged);
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

    private Map<String, byte[]> collect(Map<String, byte[]> entries, String prefix, boolean applyExclusions) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (var entry : entries.entrySet()) {
            if (!entry.getKey().startsWith(prefix + "assets/")) continue;
            String path = entry.getKey().substring(prefix.length());
            if (applyExclusions && exclusions.matches(path)) continue;
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
        List<Map<String, byte[]>> duplicateShaders = new ArrayList<>();
        Map<String, Integer> shaderDuplicates = new HashMap<>();
        for (VirtualFile file : higherFiles) {
            try (InputStream input = file.getInputStream()) {
                if (input == null) continue;
                byte[] bytes = input.readAllBytes();
                file.setInputStream(new ByteArrayInputStream(bytes));
                byte[] first = entries.get(file.getPath());
                String type = PackAssetMerger.additiveType(file.getPath());
                if (first == null) entries.put(file.getPath(), bytes);
                else if (Boolean.TRUE.equals(Settings.MERGE_SHADERS.toBool())
                        && CoreShaderMerger.isCoreShader(shaderResourcePath(file.getPath()))) {
                    int depth = shaderDuplicates.merge(file.getPath(), 1, Integer::sum) - 1;
                    while (duplicateShaders.size() <= depth) duplicateShaders.add(new LinkedHashMap<>());
                    duplicateShaders.get(depth).put(file.getPath(), bytes);
                } else if (!type.isEmpty() && !validResource(type, object(first))
                        && validResource(type, object(bytes))) entries.put(file.getPath(), bytes);
                else if (shouldMergeNative(type)) {
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
                Logs.debug(exception);
            }
        }
        Map<String, byte[]> declaredOverlayFiles = collectDeclaredOverlayFiles(entries);
        duplicateShaders.forEach(layer -> layer.keySet().removeIf(path ->
                !CoreShaderMerger.isCoreShader(path) && !declaredOverlayFiles.containsKey(path)));
        Map<String, byte[]> shaderEntries = new LinkedHashMap<>();
        entries.forEach((path, bytes) -> {
            if (CoreShaderMerger.isCoreShader(path) || declaredOverlayFiles.containsKey(path)) shaderEntries.put(path, bytes);
        });
        for (int index = duplicateShaders.size() - 1; index >= 0; index--)
            mergeShaderGroups(duplicateShaders.get(index), shaderEntries).forEach(shaderEntries::put);
        shaderEntries.forEach(entries::put);
        Map<String, byte[]> preservedOverlayFiles = preserveNativeOverlays
                ? collectDeclaredOverlayFiles(entries)
                : Collections.emptyMap();
        importEntries(entries, true, false, "local pack");
        List<VirtualFile> result = buildFiles(null);
        result.addAll(virtualFiles(preservedOverlayFiles));
        return result;
    }

    private List<VirtualFile> buildFiles(Set<PackOverride> overrides) {
        while (!mergedOverlayEntries.isEmpty()) mergedOverlayEntries.remove(0);
        overlaySnapshots.clear();
        Map<String, byte[]> base = resolve(null, overrides);
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
            Map<String, byte[]> snapshot = resolve(min, overrides);
            // Missing files still inherit the base pack. Mask removed additive
            // resources and metadata with an empty resource in the overlay. Equipment has no valid empty definition.
            base.forEach((path, bytes) -> {
                String type = PackAssetMerger.additiveType(path);
                if (((!type.isEmpty() && !type.equals("equipment")) || path.endsWith(".png.mcmeta")) && !snapshot.containsKey(path))
                    snapshot.put(path, emptyResource(type).toString().getBytes(StandardCharsets.UTF_8));
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

    private Map<String, byte[]> resolve(Long version, Set<PackOverride> overrides) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        Map<String, String> sources = overrides == null ? null : new HashMap<>();
        for (Layer layer : layers) {
            applyFilters(result, layer.metadata);
            Map<String, byte[]> active = new LinkedHashMap<>(layer.base);
            if (version != null) for (Overlay overlay : layer.overlays)
                if (overlay.min <= version && version <= overlay.max) replaceAll(active, overlay.files);
            if (convertLegacyItemModels)
                active = ItemModelMerger.convertLegacyLayer(active, result,
                        Boolean.TRUE.equals(Settings.MERGE_DUPLICATES.toBool()));
            mergeAll(result, active, layer.source, sources, overrides);
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

    private void mergeAll(Map<String, byte[]> lower, Map<String, byte[]> higher, String source,
                          Map<String, String> sources, Set<PackOverride> overrides) {
        Map<String, byte[]> shaders = mergeShaderGroups(lower, higher);
        higher.forEach((path, bytes) -> {
            if (!path.endsWith(".mcmeta")) lower.remove(path + ".mcmeta");
            String type = PackAssetMerger.additiveType(path);
            JsonObject high = type.isEmpty() ? null : object(bytes);
            if (!type.isEmpty() && !validResource(type, high)) return;
            byte[] previous = lower.get(path);
            byte[] incoming = bytes;
            boolean merged = false;
            if (shaders.containsKey(path)) {
                bytes = shaders.get(path);
                merged = true;
            } else if (shouldMergeNative(type)) {
                JsonObject low = object(previous);
                if (low != null) {
                    bytes = PackAssetMerger.mergeJson(path, low, high).toString().getBytes(StandardCharsets.UTF_8);
                    merged = true;
                }
            } else if (Boolean.TRUE.equals(Settings.MERGE_DUPLICATES.toBool())) {
                high = object(bytes);
                JsonObject low = object(previous);
                if (high != null && low != null) {
                    try {
                        JsonObject combined = null;
                        if (path.matches("assets/[^/]+/items/.+\\.json"))
                            combined = ItemModelMerger.mergeDefinition(high, low, path);
                        else if (path.matches("assets/[^/]+/models/item/.+\\.json"))
                            combined = ItemModelMerger.mergeLegacy(high, low, path);
                        if (combined != null) {
                            bytes = combined.toString().getBytes(StandardCharsets.UTF_8);
                            merged = sources == null || !combined.equals(high);
                        }
                    } catch (RuntimeException exception) {
                        Logs.logWarning("Failed to merge duplicate item resource " + path + ". Keeping the higher priority file.");
                    }
                }
            }
            if (sources != null) {
                String previousSource = sources.put(path, source);
                if (!merged && previous != null && !Arrays.equals(previous, incoming))
                    overrides.add(new PackOverride(path, previousSource, source));
            }
            lower.put(path, bytes);
        });
    }

    private static String shaderResourcePath(String path) {
        int assets = path.indexOf("assets/");
        return assets < 0 ? path : path.substring(assets);
    }

    private Map<String, byte[]> mergeShaderGroups(Map<String, byte[]> lower, Map<String, byte[]> higher) {
        Map<String, byte[]> merged = new LinkedHashMap<>();
        if (!Boolean.TRUE.equals(Settings.MERGE_SHADERS.toBool())) return merged;
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        for (String path : higher.keySet()) {
            if (!CoreShaderMerger.isCoreShader(shaderResourcePath(path))) continue;
            String group = path.substring(0, path.lastIndexOf('.'));
            groups.computeIfAbsent(group, ignored -> new LinkedHashSet<>()).add(path);
        }
        // Program files can link vertex and fragment stages with different names.
        for (Map<String, byte[]> source : List.of(lower, higher)) {
            for (var entry : source.entrySet()) {
                String path = entry.getKey();
                if (!path.endsWith(".json") || !CoreShaderMerger.isCoreShader(shaderResourcePath(path))) continue;
                JsonObject program = object(entry.getValue());
                if (program == null) continue;
                String group = path.substring(0, path.lastIndexOf('.'));
                Set<String> connected = groups.computeIfAbsent(group, ignored -> new LinkedHashSet<>());
                for (String stage : List.of("vertex", "fragment")) {
                    if (!program.has(stage) || !program.get(stage).isJsonPrimitive()) continue;
                    String name = program.get(stage).getAsString();
                    String linked;
                    int namespace = name.indexOf(':');
                    if (namespace >= 0) {
                        linked = path.substring(0, path.indexOf("assets/")) + "assets/"
                                + name.substring(0, namespace) + "/shaders/" + name.substring(namespace + 1);
                    } else {
                        linked = path.substring(0, path.lastIndexOf('/') + 1) + name;
                    }
                    Set<String> other = groups.get(linked);
                    if (other != null && other != connected) {
                        connected.addAll(other);
                        groups.replaceAll((key, value) -> value == other ? connected : value);
                    }
                }
            }
        }
        for (Set<String> group : new LinkedHashSet<>(groups.values())) {
            Map<String, byte[]> candidate = new LinkedHashMap<>();
            boolean changed = false;
            try {
                for (String path : group) {
                    byte[] low = lower.get(path);
                    byte[] high = higher.get(path);
                    if (low == null || Arrays.equals(low, high)) continue;
                    candidate.put(path, CoreShaderMerger.merge(shaderResourcePath(path), low, high));
                    changed = true;
                }
                merged.putAll(candidate);
                if (changed && !shaderWarningShown) {
                    Logs.logWarning("Shader merging is experimental and limited. Turn of shader merging in the settings or contact the Support team when running into shader issues.");
                    shaderWarningShown = true;
                }
            } catch (RuntimeException exception) {
                String path = group.iterator().next();
                Logs.logWarning("Failed to merge core shader " + path + ". Keeping the higher priority file.");
                Logs.debug(exception);
            }
        }
        return merged;
    }

    private static boolean shouldMergeNative(String type) {
        return !type.isEmpty() && (!type.equals("font") || !Boolean.FALSE.equals(Settings.MERGE_DUPLICATE_FONTS.toBool()));
    }

    private static boolean validResource(String type, JsonObject value) {
        if (value == null) return false;
        if (type.equals("lang")) return value.entrySet().stream().allMatch(entry -> entry.getValue().isJsonPrimitive());
        if (type.equals("font")) return value.has("providers") && value.get("providers").isJsonArray()
                && value.getAsJsonArray("providers").asList().stream().allMatch(JsonElement::isJsonObject);
        if (type.equals("atlases")) return value.has("sources") && value.get("sources").isJsonArray()
                && value.getAsJsonArray("sources").asList().stream().allMatch(JsonElement::isJsonObject);
        if (type.equals("blockstates")) return value.has("variants") && value.get("variants").isJsonObject()
                || value.has("multipart") && value.get("multipart").isJsonArray();
        if (type.equals("equipment")) return PackAssetMerger.hasEquipmentLayers(value);
        return true;
    }

    private static JsonObject emptyResource(String type) {
        JsonObject value = new JsonObject();
        if (type.equals("font")) value.add("providers", new JsonArray());
        else if (type.equals("atlases")) value.add("sources", new JsonArray());
        else if (type.equals("blockstates")) value.add("variants", new JsonObject());
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

    static long[] range(JsonObject declaration) {
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
