package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.pack.generation.AtlasDependencyResolver.AtlasResolutionResult;
import io.th0rgal.oraxen.pack.generation.AtlasDependencyResolver.TextureRef;
import io.th0rgal.oraxen.utils.PlatformCapabilities;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generates {@code assets/minecraft/atlases/blocks.json} (and {@code items.json} on 1.21.11+ / 26.x)
 * using {@link AtlasDependencyResolver} to classify 2D item vs 3D block/model textures, exclude
 * font-only sprite sheets, and bridge cross-atlas references on 3D models.
 */
public class AtlasGenerator {

    private static final AtlasDependencyResolver RESOLVER = new AtlasDependencyResolver();

    public AtlasGenerator() {
    }

    public static void generateAtlasFile(List<VirtualFile> output, Set<String> malformedTextures) {
        boolean splitItemAtlas = false;
        try {
            splitItemAtlas = PlatformCapabilities.supportsSplitItemAtlas(PlatformCapabilities.currentVersion());
        } catch (Exception ignored) {
            // Bukkit may not be initialized in standalone unit tests
        }
        generateAtlasFiles(output, malformedTextures, splitItemAtlas);
    }

    public static void generateAtlasFiles(
            @NotNull List<VirtualFile> output,
            @NotNull Set<String> malformedTextures,
            boolean includeSplitItemAtlas
    ) {
        boolean debug = isDebugEnabled();
        boolean excludeMalformed = isExcludeMalformedEnabled();
        String generationType = getAtlasGenerationType();

        if (debug) {
            Logs.logInfo("Generating atlas-file for Resource Pack");
        }
        if (excludeMalformed && !malformedTextures.isEmpty()) {
            Logs.logWarning("Attempting to exclude malformed textures from atlas-file");
        }

        Set<TextureRef> explicitItemTextures = collectExplicitItemLayerTextures();
        Set<String> effectiveMalformed = excludeMalformed ? malformedTextures : Collections.emptySet();

        if (excludeMalformed && !malformedTextures.isEmpty()) {
            for (VirtualFile vf : output) {
                String p = vf.getPath();
                if (p.endsWith(".png") && malformedTextures.stream().anyMatch(p::startsWith)) {
                    Logs.logWarning("Excluding malformed texture from atlas-file: <gold>" + p);
                }
            }
        }

        AtlasResolutionResult resolution = RESOLVER.resolve(
                output,
                explicitItemTextures,
                effectiveMalformed,
                includeSplitItemAtlas
        );

        JsonObject blocksAtlas = buildAtlasJson(
                resolution.blockAtlasTextures(),
                resolution.bridgedVanillaItemTextures(),
                generationType
        );
        upsertVirtualFile(output, "assets/minecraft/atlases", "blocks.json", blocksAtlas.toString());

        if (includeSplitItemAtlas && !resolution.itemAtlasTextures().isEmpty()) {
            JsonObject itemsAtlas = buildAtlasJson(
                    resolution.itemAtlasTextures(),
                    Collections.emptySet(),
                    generationType
            );
            upsertVirtualFile(output, "assets/minecraft/atlases", "items.json", itemsAtlas.toString());
        }
    }

    /**
     * Merges a split {@code items.json} atlas back into {@code blocks.json} for multi-version targets
     * older than 1.21.11 that only read {@code assets/minecraft/atlases/blocks.json}.
     */
    public static @NotNull byte[] mergeSplitAtlasesForLegacyTarget(
            @NotNull byte[] blocksAtlasBytes,
            @Nullable byte[] itemsAtlasBytes
    ) {
        if (itemsAtlasBytes == null || itemsAtlasBytes.length == 0) {
            return blocksAtlasBytes;
        }
        try {
            JsonObject blocksJson = JsonParser.parseString(new String(blocksAtlasBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject itemsJson = JsonParser.parseString(new String(itemsAtlasBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray blocksSources = blocksJson.has("sources") ? blocksJson.getAsJsonArray("sources") : new JsonArray();
            JsonArray itemsSources = itemsJson.has("sources") ? itemsJson.getAsJsonArray("sources") : new JsonArray();
            for (JsonElement entry : itemsSources) {
                if (!blocksSources.contains(entry)) {
                    blocksSources.add(entry);
                }
            }
            if ("DIRECTORY".equalsIgnoreCase(getAtlasGenerationType())) {
                removeChildEntriesInDirectoryAtlas(blocksSources);
            }
            blocksJson.add("sources", blocksSources);
            return blocksJson.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return blocksAtlasBytes;
        }
    }

    private static @NotNull JsonObject buildAtlasJson(
            @NotNull Set<TextureRef> textures,
            @NotNull Set<TextureRef> forceSingleEntryTextures,
            @NotNull String generationType
    ) {
        JsonObject atlas = new JsonObject();
        JsonArray sources = new JsonArray();
        boolean directoryMode = "DIRECTORY".equalsIgnoreCase(generationType);

        List<TextureRef> sorted = new ArrayList<>(textures);
        Collections.sort(sorted);

        for (TextureRef ref : sorted) {
            JsonObject entry = new JsonObject();
            if (directoryMode && !forceSingleEntryTextures.contains(ref)) {
                String dir = StringUtils.substringBeforeLast(ref.path(), "/");
                entry.addProperty("type", "directory");
                entry.addProperty("source", dir);
                entry.addProperty("prefix", dir + "/");
            } else {
                entry.addProperty("type", "single");
                entry.addProperty("resource", ref.toResourceLocation());
                entry.addProperty("sprite", ref.toResourceLocation());
            }
            if (!sources.contains(entry)) {
                sources.add(entry);
            }
        }

        if (directoryMode) {
            removeChildEntriesInDirectoryAtlas(sources);
        }

        atlas.add("sources", sources);
        return atlas;
    }

    private static void removeChildEntriesInDirectoryAtlas(JsonArray atlasContent) {
        Set<JsonElement> remove = new HashSet<>();
        for (JsonElement element : atlasContent) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("source")) continue;
            String source = obj.get("source").getAsString();
            if (source.contains("/")) {
                String parentDir = StringUtils.substringBefore(source, "/");
                JsonObject parentObject = new JsonObject();
                parentObject.addProperty("type", "directory");
                parentObject.addProperty("source", parentDir);
                parentObject.addProperty("prefix", parentDir + "/");
                if (atlasContent.contains(parentObject)) {
                    remove.add(element);
                }
            }
        }
        remove.forEach(atlasContent::remove);
    }

    private static void upsertVirtualFile(List<VirtualFile> output, String folder, String name, String content) {
        VirtualFile file = new VirtualFile(
                folder,
                name,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
        output.removeIf(v -> v.getPath().equals(file.getPath()));
        output.add(file);
    }

    private static Set<TextureRef> collectExplicitItemLayerTextures() {
        Set<TextureRef> itemTextures = new LinkedHashSet<>();
        if (io.th0rgal.oraxen.OraxenPlugin.get() == null) {
            return itemTextures;
        }
        try {
            Collection<ItemBuilder> items = OraxenItems.getItems();
            if (items != null) {
                for (ItemBuilder builder : items) {
                    if (builder != null && builder.hasOraxenMeta() && builder.getOraxenMeta().hasLayers()) {
                        for (String layer : builder.getOraxenMeta().getLayers()) {
                            if (layer != null && !layer.isBlank()) {
                                itemTextures.add(TextureRef.parse(layer));
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return itemTextures;
    }

    private static boolean isDebugEnabled() {
        if (io.th0rgal.oraxen.OraxenPlugin.get() == null) return false;
        try {
            return Settings.DEBUG.toBool();
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean isExcludeMalformedEnabled() {
        if (io.th0rgal.oraxen.OraxenPlugin.get() == null) return true;
        try {
            return Settings.EXCLUDE_MALFORMED_ATLAS.toBool();
        } catch (Throwable e) {
            return true;
        }
    }

    private static String getAtlasGenerationType() {
        if (io.th0rgal.oraxen.OraxenPlugin.get() == null) return "SINGLE";
        try {
            String val = Settings.ATLAS_GENERATION_TYPE.toString();
            return val != null ? val : "SINGLE";
        } catch (Throwable e) {
            return "SINGLE";
        }
    }
}
