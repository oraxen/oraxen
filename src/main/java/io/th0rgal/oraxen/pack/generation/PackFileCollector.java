package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Collects files from the pack folder into VirtualFile objects and provides
 * static pack verification utilities.
 * Extracted from ResourcePack to reduce class size.
 */
class PackFileCollector {

    private final File packFolder;
    private final PackExclusions exclusions;

    PackFileCollector(File packFolder) {
        this(packFolder, PackExclusions.load());
    }

    PackFileCollector(File packFolder, PackExclusions exclusions) {
        this.packFolder = packFolder;
        this.exclusions = exclusions;
    }

    void getAllFiles(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null)
            for (final File file : files) {
                if (shouldIgnorePackFile(file, packFolder))
                    continue;
                if (file.isDirectory())
                    getAllFiles(file, fileList, newFolder, excluded);
                else if (!blacklist.contains(file.getName()))
                    readFileToVirtuals(fileList, file, newFolder);
            }
    }

    void getFilesInFolder(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null)
            for (final File file : files)
                if (!file.isDirectory() && !blacklist.contains(file.getName()) && !shouldIgnorePackFile(file, packFolder))
                    readFileToVirtuals(fileList, file, newFolder);
    }

    static boolean shouldIgnorePackFile(File file) {
        return shouldIgnorePackFile(file, null);
    }

    static boolean shouldIgnorePackFile(File file, File packFolder) {
        String name = file.getName();
        if (".DS_Store".equals(name) || "Thumbs.db".equalsIgnoreCase(name) || "desktop.ini".equalsIgnoreCase(name))
            return true;

        if (file.isDirectory() && "__MACOSX".equals(name))
            return true;

        // Exclude .zip files only at the pack folder root — these are generated outputs
        // (main pack.zip and multi-version zips like pack_1_20.zip). User .zip assets in
        // subdirectories (e.g., assets/) are preserved.
        if (!file.isDirectory() && name.toLowerCase().endsWith(".zip")
                && packFolder != null && packFolder.equals(file.getParentFile()))
            return true;

        return false;
    }

    static Map<String, Set<String>> verifyPackFormatting(List<VirtualFile> output) {
        if (Settings.DEBUG.toBool()) Logs.logInfo("Verifying formatting for textures and models...");
        Map<String, List<VirtualFile>> textures = new LinkedHashMap<>();
        Map<String, Set<String>> texturePaths = new LinkedHashMap<>();
        Map<String, Set<String>> mcmeta = new LinkedHashMap<>();
        Map<String, List<VirtualFile>> models = new LinkedHashMap<>();
        Map<String, Set<String>> malformedFiles = new LinkedHashMap<>();
        for (VirtualFile virtualFile : output) {
            PackPipeline.AssetPath assetPath = PackPipeline.splitAssetPath(virtualFile.getPath());
            if (assetPath == null) continue;
            String path = assetPath.path();
            if (path.matches("assets/.*/models/.*.json"))
                models.computeIfAbsent(assetPath.prefix(), ignored -> new ArrayList<>()).add(virtualFile);
            else if (path.matches("assets/.*/textures/.*.png.mcmeta"))
                mcmeta.computeIfAbsent(assetPath.prefix(), ignored -> new HashSet<>()).add(path);
            else if (path.matches("assets/.*/textures/.*.png")) {
                textures.computeIfAbsent(assetPath.prefix(), ignored -> new ArrayList<>()).add(virtualFile);
                texturePaths.computeIfAbsent(assetPath.prefix(), ignored -> new HashSet<>()).add(path);
            }
        }

        if (models.isEmpty() && !textures.isEmpty())
            return Collections.emptyMap();

        for (Map.Entry<String, List<VirtualFile>> layer : models.entrySet()) {
            for (VirtualFile model : layer.getValue()) {
                PackPipeline.AssetPath assetPath = PackPipeline.splitAssetPath(model.getPath());
                String path = assetPath.path();
                if (!path.matches("[a-z0-9/._-]+")) {
                    Logs.logWarning("Found invalid model at <blue>" + model.getPath());
                    Logs.logError("Model-paths must only contain characters [a-z0-9/._-]");
                    addMalformed(malformedFiles, assetPath, path);
                }

                String content;
                try {
                    InputStream inputStream = model.getInputStream();
                    if (inputStream == null) {
                        content = "";
                    } else {
                        byte[] data;
                        try (inputStream) {
                            data = inputStream.readAllBytes();
                        }
                        // Important: restore stream for later zip writing
                        model.setInputStream(new ByteArrayInputStream(data));
                        content = new String(data, StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    content = "";
                }

                if (!content.isEmpty()) {
                    JsonObject jsonModel;
                    try {
                        jsonModel = JsonParser.parseString(content).getAsJsonObject();
                    } catch (JsonSyntaxException e) {
                        Logs.logError("Found malformed json at <red>" + model.getPath() + "</red>");
                        Logs.logWarning("Skipping a resource pack model that could not be processed.");
                        Logs.debug(e);
                        addMalformed(malformedFiles, assetPath, path);
                        continue;
                    }
                    if (jsonModel.has("textures")) {
                        for (JsonElement element : jsonModel.getAsJsonObject("textures").entrySet().stream()
                                .map(Map.Entry::getValue).toList()) {
                            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                                Logs.logWarning("Found invalid texture-path inside model-file <blue>"
                                        + model.getPath() + "</blue>: " + element);
                                addMalformed(malformedFiles, assetPath, path);
                                continue;
                            }

                            String jsonTexture = element.getAsString();
                            if (isMissingTexture(jsonTexture,
                                    texturePaths.getOrDefault(layer.getKey(), Collections.emptySet()))) {
                                Logs.logWarning("Found invalid texture-path inside model-file <blue>"
                                        + model.getPath() + "</blue>: " + jsonTexture);
                                Logs.logWarning("Verify that you have a texture in said path.", true);
                                addMalformed(malformedFiles, assetPath, path);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, List<VirtualFile>> layer : textures.entrySet()) {
            for (VirtualFile texture : layer.getValue()) {
                PackPipeline.AssetPath assetPath = PackPipeline.splitAssetPath(texture.getPath());
                String path = assetPath.path();
                if (!path.matches("[a-z0-9/._-]+")) {
                    Logs.logWarning("Found invalid texture at <blue>" + texture.getPath());
                    Logs.logError("Texture-paths must only contain characters [a-z0-9/._-]");
                    addMalformed(malformedFiles, assetPath, path);
                }
                if (!path.matches(".*_layer_.*.png")) {
                    if (mcmeta.getOrDefault(layer.getKey(), Collections.emptySet()).contains(path + ".mcmeta"))
                        continue;
                    try {
                        InputStream inputStream = texture.getInputStream();
                        if (inputStream == null) {
                            Logs.logWarning("Found unreadable texture at <blue>" + texture.getPath() + "</blue>");
                            addMalformed(malformedFiles, assetPath, path);
                            continue;
                        }

                        byte[] data;
                        try (inputStream) {
                            data = inputStream.readAllBytes();
                        }
                        // Important: restore stream for later zip writing
                        texture.setInputStream(new ByteArrayInputStream(data));

                        // ImageIO.read returns null if there is no suitable reader
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                        if (image == null) {
                            Logs.logWarning("Found unreadable texture at <blue>" + texture.getPath() + "</blue>");
                            Logs.logWarning("Image format may be corrupt or unsupported by ImageIO.", true);
                            addMalformed(malformedFiles, assetPath, path);
                            continue;
                        }
                    } catch (Exception e) {
                        // Be resilient when validating packs: bad files should not crash pack
                        // generation
                        Logs.logWarning("Failed to validate texture <blue>" + texture.getPath() + "</blue>");
                        if (Settings.DEBUG.toBool())
                            Logs.logWarning("Skipping a resource pack texture that could not be processed.");
                            Logs.debug(e);
                        addMalformed(malformedFiles, assetPath, path);
                    }
                }
            }
        }

        if (!malformedFiles.isEmpty()) {
            Logs.logError("Pack contains malformed texture(s) and/or model(s)");
            Logs.logError("These need to be fixed, otherwise the resourcepack will be broken");
        } else
            Logs.logSuccess("No broken models or textures were found in the resourcepack");

        return malformedFiles;
    }

    private static void addMalformed(Map<String, Set<String>> malformedFiles,
                                     PackPipeline.AssetPath assetPath, String path) {
        malformedFiles.computeIfAbsent(assetPath.prefix(), ignored -> new HashSet<>()).add(path);
    }

    static String modelPathToPackPath(String modelPath) {
        int separator = modelPath.indexOf(':');
        String namespace = separator < 0 ? "minecraft" : modelPath.substring(0, separator);
        String texturePath = separator < 0 ? modelPath : modelPath.substring(separator + 1);
        texturePath = texturePath.endsWith(".png") ? texturePath : texturePath + ".png";
        return "assets/" + namespace + "/textures/" + texturePath;
    }

    private static boolean isMissingTexture(String reference, Set<String> texturePaths) {
        if (reference.startsWith("#")) return false;

        int separator = reference.indexOf(':');
        String namespace = separator < 0 ? "minecraft" : reference.substring(0, separator);
        String path = separator < 0 ? reference : reference.substring(separator + 1);
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/._-]+")) return true;
        if (texturePaths.contains(modelPathToPackPath(reference))) return false;

        // Vanilla textures are supplied by the client, but custom namespaces must exist in the pack.
        if (!namespace.equals("minecraft")) return true;
        if (path.startsWith("item/") || path.startsWith("block/") || path.startsWith("entity/")) return false;
        String name = path.substring(path.lastIndexOf('/') + 1);
        int extension = name.lastIndexOf('.');
        if (extension >= 0) name = name.substring(0, extension);
        return Material.matchMaterial(name.toUpperCase(Locale.ROOT)) == null;
    }

    private void readFileToVirtuals(final Collection<VirtualFile> output, File file, String newFolder) {
        String relativePath = packFolder.toPath().relativize(file.toPath()).toString();
        if (!relativePath.equals("pack.mcmeta") && exclusions.matches(relativePath)) return;
        try {
            final InputStream fis;
            if (file.getName().endsWith(".json"))
                fis = processJsonFile(file);
            else
                fis = new FileInputStream(file);

            String parentPath = getZipFilePath(file.getParentFile().getCanonicalPath(), newFolder);
            output.add(new VirtualFile(parentPath, file.getName(), fis));
        } catch (IOException e) {
            Logs.logWarning("Skipping a resource pack file after an I/O failure.");
            Logs.debug(e);
        }
    }

    private InputStream processJsonFile(File file) throws IOException {
        InputStream newStream;
        String content;
        if (!file.exists())
            return new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        try {
            content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            Logs.logError("Error while reading file " + file.getPath());
            Logs.logError("It seems to be malformed!");
            newStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            newStream.close();
            return newStream;
        }

        String normalisedPath = file.getPath().replace("\\", "/");

        // If the json file is a font file, do not format it through MiniMessage
        if (normalisedPath.split("assets/.*/font/").length > 1) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        // Only lang files are at risk of double-glyph expansion, because their values
        // are re-rendered through MiniMessage at runtime by the chat/translation system.
        // Restrict the structural processor to that scope; other JSON keeps the legacy
        // raw-text path so byte output (and hash-based caches) stay unchanged.
        return isLangFile(normalisedPath) ? processLangJson(content) : processJson(content);
    }

    private static boolean isLangFile(String normalisedPath) {
        return normalisedPath.matches(".*assets/[^/]+/lang/[^/]+\\.json");
    }

    static InputStream processJson(String content) {
        String parsedContent = AdventureUtils.parseLegacyThroughMiniMessage(content).replace("\\<", "<");
        return new ByteArrayInputStream(parsedContent.getBytes(StandardCharsets.UTF_8));
    }

    static InputStream processLangJson(String content) {
        String parsedContent = processLangJsonContent(content);
        return new ByteArrayInputStream(parsedContent.getBytes(StandardCharsets.UTF_8));
    }

    // Default Gson HTML-escapes '<' and '>' to '\u003c' / '\u003e', which would
    // (a) bloat lang files now full of MiniMessage tags, (b) change pack hashes
    // and force re-downloads, and (c) make the trailing `\<` cleanup a no-op.
    // Disable HTML escaping so MiniMessage tags survive serialization unchanged.
    private static final Gson LANG_JSON_GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static String processLangJsonContent(String content) {
        try {
            JsonElement parsedJson = JsonParser.parseString(content);
            Map<String, String> cache = new HashMap<>();
            processJsonElement(parsedJson, cache);
            return LANG_JSON_GSON.toJson(parsedJson).replace("\\<", "<");
        } catch (JsonSyntaxException syntaxError) {
            Logs.debug("Lang JSON failed structural parse, falling back to raw MiniMessage: " + syntaxError.getMessage());
            return AdventureUtils.parseLegacyThroughMiniMessage(content).replace("\\<", "<");
        }
    }

    private static void processJsonElement(JsonElement element, Map<String, String> cache) {
        // Iterative walk to avoid recursion overflow on deep documents.
        Deque<JsonElement> stack = new ArrayDeque<>();
        stack.push(element);
        while (!stack.isEmpty()) {
            JsonElement current = stack.pop();
            if (current == null || current.isJsonNull() || current.isJsonPrimitive()) continue;

            if (current.isJsonArray()) {
                JsonArray array = current.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement child = array.get(i);
                    if (child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString())
                        array.set(i, parseJsonString(child.getAsString(), cache));
                    else
                        stack.push(child);
                }
                continue;
            }

            JsonObject jsonObject = current.getAsJsonObject();
            // Snapshot keys first because we may mutate the map under iteration.
            List<String> keys = new ArrayList<>(jsonObject.keySet());
            for (String key : keys) {
                JsonElement child = jsonObject.get(key);
                if (child == null || child.isJsonNull()) continue;

                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    jsonObject.add(key, parseJsonString(child.getAsString(), cache));
                } else {
                    stack.push(child);
                }
            }
        }
    }

    private static JsonElement parseJsonString(String value, Map<String, String> cache) {
        String transformed = cache.computeIfAbsent(value,
                v -> AdventureUtils.parseLegacyThroughMiniMessage(v).replace("\\<", "<"));
        return new JsonPrimitive(transformed);
    }

    private String getZipFilePath(String path, String newFolder) throws IOException {
        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        if (newFolder.equals(packFolder.getCanonicalPath()))
            return "";
        String prefix = newFolder.isEmpty() ? newFolder : newFolder + "/";
        return prefix + path.substring(packFolder.getCanonicalPath().length() + 1);
    }
}
