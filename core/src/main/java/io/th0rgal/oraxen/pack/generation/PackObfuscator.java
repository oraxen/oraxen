package io.th0rgal.oraxen.pack.generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PackObfuscator {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private PackObfuscator() {
    }

    public static void obfuscate(List<VirtualFile> output) {
        Object rawType = Settings.PACK_OBFUSCATION_TYPE.getValue();
        obfuscate(output, rawType == null ? null : rawType.toString(), true);
    }

    static void obfuscate(List<VirtualFile> output, String rawType) {
        obfuscate(output, rawType, true);
    }

    static void obfuscate(List<VirtualFile> output, String rawType, boolean logSummary) {
        Type type = Type.from(rawType);
        if (type == Type.NONE || output == null || output.isEmpty()) return;

        try {
            new Pass(type, logSummary).run(output);
        } catch (Exception exception) {
            Logs.logError("Failed to obfuscate resource pack. Keeping readable pack output.");
            if (Settings.DEBUG.toBool()) exception.printStackTrace();
        }
    }

    private enum Type {
        NONE,
        SIMPLE,
        FULL;

        private static Type from(String raw) {
            if (raw == null) return NONE;
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (normalized.equals("FALSE") || normalized.equals("OFF") || normalized.equals("DISABLED")) return NONE;
            try {
                return Type.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                Logs.logWarning("Invalid pack obfuscation type <gold>" + raw + "</gold>; using NONE");
                return NONE;
            }
        }
    }

    private static final class Pass {
        private final Type type;
        private final boolean logSummary;
        private final Map<String, String> namespaceMap = new LinkedHashMap<>();
        private final Map<String, String> modelKeys = new LinkedHashMap<>();
        private final Map<String, String> textureKeys = new LinkedHashMap<>();
        private final Map<String, String> soundKeys = new LinkedHashMap<>();
        private final Map<String, String> filePaths = new LinkedHashMap<>();

        private Pass(Type type, boolean logSummary) {
            this.type = type;
            this.logSummary = logSummary;
        }

        private void run(List<VirtualFile> output) throws Exception {
            List<FileData> files = materialize(output);
            collectRenames(files);
            if (filePaths.isEmpty()) return;

            for (FileData file : files) {
                String originalPath = file.path;
                if (file.isJson()) rewriteJson(file);
                String rewrittenPath = filePaths.getOrDefault(originalPath, originalPath);
                if (!rewrittenPath.equals(originalPath)) file.virtualFile.setPath(rewrittenPath);
                file.path = rewrittenPath;
                file.virtualFile.setInputStream(new ByteArrayInputStream(file.content));
            }

            if (logSummary) {
                Logs.logInfo("Obfuscated resource pack keys: " + modelKeys.size() + " models, "
                        + textureKeys.size() + " textures, " + soundKeys.size() + " sounds");
            }
        }

        private List<FileData> materialize(List<VirtualFile> output) throws Exception {
            List<FileData> files = new ArrayList<>(output.size());
            for (VirtualFile virtualFile : output) {
                byte[] content;
                InputStream inputStream = virtualFile.getInputStream();
                if (inputStream == null) {
                    content = new byte[0];
                } else {
                    try (inputStream) {
                        content = inputStream.readAllBytes();
                    }
                }
                virtualFile.setInputStream(new ByteArrayInputStream(content));
                files.add(new FileData(virtualFile, virtualFile.getPath(), content));
            }
            return files;
        }

        private void collectRenames(List<FileData> files) {
            Set<String> referencedTextureKeys = collectReferencedTextureKeys(files);

            for (FileData file : files) {
                String path = file.path;

                if (isTexture(path)) {
                    String originalKey = keyFromPackPath(path, "/textures/", ".png");
                    if (isUntrackedVanillaTexture(originalKey, referencedTextureKeys)) continue;
                    String obfuscatedKey = obfuscateKey(originalKey, "t");
                    textureKeys.put(originalKey, obfuscatedKey);
                    filePaths.put(path, texturePath(obfuscatedKey, ".png"));
                    continue;
                }

                if (isTextureMetadata(path)) {
                    String pngPath = path.substring(0, path.length() - ".mcmeta".length());
                    String originalKey = keyFromPackPath(pngPath, "/textures/", ".png");
                    if (isUntrackedVanillaTexture(originalKey, referencedTextureKeys)) continue;
                    String obfuscatedKey = obfuscateKey(originalKey, "t");
                    textureKeys.put(originalKey, obfuscatedKey);
                    filePaths.put(path, texturePath(obfuscatedKey, ".png.mcmeta"));
                    continue;
                }

                if (isModel(path) && !isStableModelRoot(path)) {
                    String originalKey = keyFromPackPath(path, "/models/", ".json");
                    String obfuscatedKey = obfuscateKey(originalKey, "m");
                    modelKeys.put(originalKey, obfuscatedKey);
                    filePaths.put(path, modelPath(obfuscatedKey));
                    continue;
                }

                if (isSound(path)) {
                    String originalKey = keyFromPackPath(path, "/sounds/", ".ogg");
                    String obfuscatedKey = obfuscateKey(originalKey, "s");
                    soundKeys.put(originalKey, obfuscatedKey);
                    filePaths.put(path, soundPath(obfuscatedKey));
                }
            }
        }

        private Set<String> collectReferencedTextureKeys(List<FileData> files) {
            Set<String> keys = new HashSet<>();
            for (FileData file : files) {
                if (!file.isJson()) continue;
                JsonElement parsed;
                try {
                    parsed = JsonParser.parseString(new String(file.content, StandardCharsets.UTF_8));
                } catch (Exception ignored) {
                    continue;
                }
                collectReferencedTextureKeys(parsed, null, namespaceFromPath(file.path), keys);
            }
            return keys;
        }

        private void collectReferencedTextureKeys(JsonElement element, String property, String namespace, Set<String> keys) {
            if (element == null || element.isJsonNull()) return;
            if (element.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                    collectReferencedTextureKeys(entry.getValue(), entry.getKey(), namespace, keys);
                }
                return;
            }
            if (element.isJsonArray()) {
                for (JsonElement child : element.getAsJsonArray()) {
                    collectReferencedTextureKeys(child, property, namespace, keys);
                }
                return;
            }
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return;

            String prop = property == null ? "" : property.toLowerCase(Locale.ROOT);
            if (!isTextureProperty(prop)) return;
            addTextureReference(element.getAsString(), namespace, keys);
        }

        private void rewriteJson(FileData file) {
            JsonElement parsed;
            try {
                parsed = JsonParser.parseString(new String(file.content, StandardCharsets.UTF_8));
            } catch (Exception ignored) {
                return;
            }

            String namespace = namespaceFromPath(file.path);
            JsonElement rewritten = rewriteElement(parsed, null, namespace);
            if (isAtlas(file.path) && rewritten.isJsonObject()) addAtlasSingles(rewritten.getAsJsonObject());
            file.content = GSON.toJson(rewritten).getBytes(StandardCharsets.UTF_8);
        }

        private JsonElement rewriteElement(JsonElement element, String property, String namespace) {
            if (element == null || element.isJsonNull()) return element;
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                JsonObject copy = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    copy.add(entry.getKey(), rewriteElement(entry.getValue(), entry.getKey(), namespace));
                }
                return copy;
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                JsonArray copy = new JsonArray();
                for (JsonElement child : array) {
                    copy.add(rewriteElement(child, property, namespace));
                }
                return copy;
            }
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return element;

            String value = element.getAsString();
            String replacement = rewriteString(property, value, namespace);
            return replacement.equals(value) ? element : new JsonPrimitive(replacement);
        }

        private String rewriteString(String property, String value, String namespace) {
            if (value == null || value.isBlank() || value.startsWith("#")) return value;
            String prop = property == null ? "" : property.toLowerCase(Locale.ROOT);

            if (isModelProperty(prop)) return replaceOrOriginal(modelKeys, value, namespace);
            if (isTextureProperty(prop)) {
                String replacement = lookupTextureProperty(prop, value, namespace);
                return replacement != null ? replacement : value;
            }
            if (isSoundProperty(prop)) return replaceOrOriginal(soundKeys, value, namespace);

            String model = lookup(modelKeys, value, namespace);
            if (model != null) return model;
            String texture = lookup(textureKeys, value, namespace);
            if (texture != null) return texture;
            String sound = lookup(soundKeys, value, namespace);
            return sound != null ? sound : value;
        }

        private String replaceOrOriginal(Map<String, String> map, String value, String namespace) {
            String replacement = lookup(map, value, namespace);
            return replacement != null ? replacement : value;
        }

        private String lookupTextureProperty(String prop, String value, String namespace) {
            return prop.equals("file")
                    ? lookup(textureKeys, value, namespace, extensionFrom(value))
                    : lookup(textureKeys, value, namespace);
        }

        private static boolean isModelProperty(String prop) {
            return prop.equals("parent") || prop.equals("model") || prop.equals("base");
        }

        private static boolean isTextureProperty(String prop) {
            return prop.equals("file") || prop.equals("texture") || prop.equals("resource") || prop.equals("sprite")
                    || prop.equals("particle") || prop.startsWith("layer") || isTextureFace(prop);
        }

        private static boolean isSoundProperty(String prop) {
            return prop.equals("name") || prop.equals("sounds");
        }

        private void addAtlasSingles(JsonObject atlas) {
            if (textureKeys.isEmpty()) return;
            JsonArray sources = atlas.has("sources") && atlas.get("sources").isJsonArray()
                    ? atlas.getAsJsonArray("sources")
                    : new JsonArray();

            for (String obfuscatedKey : textureKeys.values()) {
                JsonObject source = new JsonObject();
                source.addProperty("type", "single");
                source.addProperty("resource", obfuscatedKey);
                source.addProperty("sprite", obfuscatedKey);
                sources.add(source);
            }

            atlas.add("sources", sources);
        }

        private String lookup(Map<String, String> map, String value, String namespace) {
            return lookup(map, value, namespace, "");
        }

        private String lookup(Map<String, String> map, String value, String namespace, String suffix) {
            String direct = stripKnownExtension(value);
            if (direct.contains(":")) return appendSuffix(map.get(direct), suffix);

            String local = namespace + ":" + direct;
            String replacement = map.get(local);
            if (replacement != null) return appendSuffix(replacement, suffix);

            return appendSuffix(map.get("minecraft:" + direct), suffix);
        }

        private String obfuscateKey(String originalKey, String prefix) {
            int split = originalKey.indexOf(':');
            String namespace = split >= 0 ? originalKey.substring(0, split) : "minecraft";
            String path = split >= 0 ? originalKey.substring(split + 1) : originalKey;
            String targetNamespace = type == Type.FULL
                    ? namespaceMap.computeIfAbsent(namespace, ignored -> "o" + hash("ns:" + namespace, 10))
                    : namespace;
            return targetNamespace + ":" + prefix + "/" + hash(namespace + ":" + path, 24);
        }

        private String hash(String value, int chars) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
                StringBuilder builder = new StringBuilder(bytes.length * 2);
                for (byte b : bytes) builder.append(String.format("%02x", b));
                return builder.substring(0, chars);
            } catch (Exception ignored) {
                return Integer.toHexString(value.hashCode());
            }
        }

        private static boolean isTextureFace(String prop) {
            return prop.equals("all") || prop.equals("top") || prop.equals("bottom") || prop.equals("side")
                    || prop.equals("front") || prop.equals("back") || prop.equals("up") || prop.equals("down")
                    || prop.equals("north") || prop.equals("south") || prop.equals("east") || prop.equals("west");
        }

        private static String stripKnownExtension(String value) {
            String stripped = value;
            if (stripped.endsWith(".json")) stripped = stripped.substring(0, stripped.length() - 5);
            if (stripped.endsWith(".png")) stripped = stripped.substring(0, stripped.length() - 4);
            if (stripped.endsWith(".ogg")) stripped = stripped.substring(0, stripped.length() - 4);
            return stripped;
        }

        private static String extensionFrom(String value) {
            if (value == null) return "";
            if (value.endsWith(".json")) return ".json";
            if (value.endsWith(".png")) return ".png";
            if (value.endsWith(".ogg")) return ".ogg";
            return "";
        }

        private static String appendSuffix(String value, String suffix) {
            return value == null ? null : value + suffix;
        }

        private static boolean isUntrackedVanillaTexture(String key, Set<String> referencedTextureKeys) {
            return key.startsWith("minecraft:") && !referencedTextureKeys.contains(key);
        }

        private static void addTextureReference(String value, String namespace, Set<String> keys) {
            if (value == null || value.isBlank() || value.startsWith("#")) return;
            String stripped = stripKnownExtension(value);
            if (stripped.contains(":")) {
                keys.add(stripped);
                return;
            }
            keys.add(namespace + ":" + stripped);
            keys.add("minecraft:" + stripped);
        }

        private static String keyFromPackPath(String path, String marker, String suffix) {
            String normalized = path.replace('\\', '/');
            String namespace = normalized.substring("assets/".length(), normalized.indexOf('/', "assets/".length()));
            String resourcePath = normalized.substring(normalized.indexOf(marker) + marker.length(), normalized.length() - suffix.length());
            return namespace + ":" + resourcePath;
        }

        private static String namespaceFromPath(String path) {
            String normalized = path.replace('\\', '/');
            if (!normalized.startsWith("assets/")) return "minecraft";
            int end = normalized.indexOf('/', "assets/".length());
            return end == -1 ? "minecraft" : normalized.substring("assets/".length(), end);
        }

        private static String modelPath(String key) {
            int split = key.indexOf(':');
            return "assets/" + key.substring(0, split) + "/models/" + key.substring(split + 1) + ".json";
        }

        private static String texturePath(String key, String suffix) {
            int split = key.indexOf(':');
            return "assets/" + key.substring(0, split) + "/textures/" + key.substring(split + 1) + suffix;
        }

        private static String soundPath(String key) {
            int split = key.indexOf(':');
            return "assets/" + key.substring(0, split) + "/sounds/" + key.substring(split + 1) + ".ogg";
        }

        private static boolean isJsonPath(String path) {
            return path.endsWith(".json") || path.endsWith(".mcmeta");
        }

        private static boolean isTexture(String path) {
            return path.startsWith("assets/") && path.contains("/textures/") && path.endsWith(".png");
        }

        private static boolean isTextureMetadata(String path) {
            return path.startsWith("assets/") && path.contains("/textures/") && path.endsWith(".png.mcmeta");
        }

        private static boolean isModel(String path) {
            return path.startsWith("assets/") && path.contains("/models/") && path.endsWith(".json");
        }

        private static boolean isSound(String path) {
            return path.startsWith("assets/") && path.contains("/sounds/") && path.endsWith(".ogg");
        }

        private static boolean isAtlas(String path) {
            return path.startsWith("assets/") && path.contains("/atlases/") && path.endsWith(".json");
        }

        private static boolean isStableModelRoot(String path) {
            if (!path.startsWith("assets/minecraft/models/")) return false;
            String modelPath = path.substring("assets/minecraft/models/".length());
            return modelPath.matches("(item|block|entity|builtin)/[^/]+\\.json");
        }
    }

    private static final class FileData {
        private final VirtualFile virtualFile;
        private String path;
        private byte[] content;

        private FileData(VirtualFile virtualFile, String path, byte[] content) {
            this.virtualFile = virtualFile;
            this.path = path;
            this.content = content;
        }

        private boolean isJson() {
            return Pass.isJsonPath(path);
        }
    }
}
