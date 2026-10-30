package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Composes native additive resources. The first file has the highest priority. */
final class PackPipeline {
    private PackPipeline() {}

    static void mergeNativeFiles(List<VirtualFile> files, boolean mergeFonts) {
        Map<String, JsonObject> merged = new LinkedHashMap<>();
        for (int i = files.size() - 1; i >= 0; i--) {
            VirtualFile file = files.get(i);
            String path = file.getPath();
            if (!PackAssetMerger.isAdditivePath(path) || (!mergeFonts && path.contains("/font/"))
                    || !file.isJsonObject()) continue;
            JsonObject higher = file.toJsonObject();
            if (higher == null) continue;
            if (path.contains("/lang/") && !validLocale(higher)) continue;
            JsonObject lower = merged.get(path);
            merged.put(path, lower == null ? higher : PackAssetMerger.mergeJson(path, lower, higher));
        }
        List<VirtualFile> result = new ArrayList<>();
        for (VirtualFile file : files) {
            JsonObject json = merged.remove(file.getPath());
            if (json != null) {
                int slash = file.getPath().lastIndexOf('/');
                result.add(new VirtualFile(file.getPath().substring(0, slash), file.getPath().substring(slash + 1),
                        new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8))));
            } else if (!PackAssetMerger.isAdditivePath(file.getPath())
                    || (!mergeFonts && file.getPath().contains("/font/")) || !file.isJsonObject()) {
                result.add(file);
            }
        }
        files.clear();
        files.addAll(result);
    }

    private static boolean validLocale(JsonObject locale) {
        return locale.entrySet().stream().allMatch(entry -> entry.getValue().isJsonPrimitive());
    }

    static void processSnapshots(List<VirtualFile> files, Consumer<List<VirtualFile>> processor) {
        Map<String, List<VirtualFile>> snapshots = new LinkedHashMap<>();
        snapshots.put("", new ArrayList<>());
        List<VirtualFile> retained = new ArrayList<>();
        for (VirtualFile file : files) {
            int assets = file.getPath().startsWith("assets/") ? 0 : file.getPath().indexOf("/assets/") + 1;
            if (assets == 0 && !file.getPath().startsWith("assets/")) {
                retained.add(file);
                continue;
            }
            String prefix = file.getPath().substring(0, assets);
            String path = file.getPath().substring(assets);
            int slash = path.lastIndexOf('/');
            snapshots.computeIfAbsent(prefix, ignored -> new ArrayList<>()).add(new VirtualFile(
                    path.substring(0, slash), path.substring(slash + 1), file.getInputStream()));
        }
        snapshots.forEach((prefix, snapshot) -> {
            processor.accept(snapshot);
            for (VirtualFile file : snapshot) {
                if (!prefix.isEmpty()) file.setPath(prefix + file.getPath());
                retained.add(file);
            }
        });
        files.clear();
        files.addAll(retained);
    }

    /** Removes generated overlay resources that already resolve from lower priority packs. */
    static void sparsifyGeneratedOverlays(List<VirtualFile> files, JsonArray generatedEntries) {
        if (files == null || files.isEmpty() || generatedEntries == null || generatedEntries.isEmpty()) return;

        Map<String, long[]> generatedRanges = new LinkedHashMap<>();
        for (JsonElement element : generatedEntries) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("directory") || !entry.get("directory").isJsonPrimitive()) continue;
            String directory = entry.get("directory").getAsString();
            long[] range = PackMerger.range(entry);
            if (range != null && range[0] <= range[1]) generatedRanges.put(directory, range);
        }
        if (generatedRanges.isEmpty()) return;

        VirtualFile rootMetadata = files.stream().filter(file -> file.getPath().equals("pack.mcmeta")).findFirst().orElse(null);
        if (rootMetadata == null) return;

        try {
            JsonObject metadata;
            try {
                JsonElement parsed = JsonParser.parseString(new String(readAndRestore(rootMetadata), StandardCharsets.UTF_8));
                if (!parsed.isJsonObject()) return;
                metadata = parsed.getAsJsonObject();
            } catch (RuntimeException exception) {
                return;
            }
            JsonElement overlaysElement = metadata.get("overlays");
            if (overlaysElement == null || !overlaysElement.isJsonObject()) return;
            JsonElement declarationsElement = overlaysElement.getAsJsonObject().get("entries");
            if (declarationsElement == null || !declarationsElement.isJsonArray()) return;

            List<OverlayDeclaration> declarations = new ArrayList<>();
            for (JsonElement element : declarationsElement.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject declaration = element.getAsJsonObject();
                if (!declaration.has("directory") || !declaration.get("directory").isJsonPrimitive()) continue;
                String directory = declaration.get("directory").getAsString();
                long[] range = PackMerger.range(declaration);
                if (range != null) declarations.add(new OverlayDeclaration(directory, range));
            }

            Map<String, byte[]> baseAssets = new HashMap<>();
            Map<String, Map<String, VirtualFile>> overlayAssets = new HashMap<>();
            Set<String> directories = declarations.stream().map(OverlayDeclaration::directory)
                    .collect(java.util.stream.Collectors.toSet());
            for (VirtualFile file : files) {
                String path = file.getPath();
                if (path.startsWith("assets/")) {
                    baseAssets.put(path, readAndRestore(file));
                    continue;
                }
                int slash = path.indexOf('/');
                if (slash <= 0) continue;
                String directory = path.substring(0, slash);
                if (!directories.contains(directory) || !path.startsWith(directory + "/assets/")) continue;
                overlayAssets.computeIfAbsent(directory, ignored -> new LinkedHashMap<>())
                        .put(path.substring(slash + 1), file);
            }

            List<VirtualFile> redundantFiles = new ArrayList<>();
            for (Map.Entry<String, long[]> generated : generatedRanges.entrySet()) {
                int declarationIndex = -1;
                long targetFormat = generated.getValue()[0];
                for (int i = 0; i < declarations.size(); i++) {
                    OverlayDeclaration declaration = declarations.get(i);
                    if (!declaration.directory().equals(generated.getKey())) continue;
                    declarationIndex = i;
                    targetFormat = declaration.range()[0];
                    break;
                }
                if (declarationIndex < 0) continue;

                Map<String, byte[]> lowerPriorityAssets = new HashMap<>(baseAssets);
                for (int i = 0; i < declarationIndex; i++) {
                    OverlayDeclaration lower = declarations.get(i);
                    if (targetFormat < lower.range()[0] || targetFormat > lower.range()[1]) continue;
                    Map<String, VirtualFile> lowerFiles = overlayAssets.get(lower.directory());
                    if (lowerFiles == null) continue;
                    for (Map.Entry<String, VirtualFile> asset : lowerFiles.entrySet())
                        lowerPriorityAssets.put(asset.getKey(), readAndRestore(asset.getValue()));
                }

                Map<String, VirtualFile> snapshot = overlayAssets.get(generated.getKey());
                if (snapshot == null) continue;
                for (Map.Entry<String, VirtualFile> asset : snapshot.entrySet()) {
                    byte[] lowerContent = lowerPriorityAssets.get(asset.getKey());
                    if (lowerContent != null && Arrays.equals(lowerContent, readAndRestore(asset.getValue())))
                        redundantFiles.add(asset.getValue());
                }
            }
            files.removeAll(redundantFiles);
        } catch (IOException | RuntimeException exception) {
            Logs.logWarning("Failed to sparsify generated resource pack overlays. Keeping the full overlays.");
            if (io.th0rgal.oraxen.configs.Settings.DEBUG.toBool()) exception.printStackTrace();
        }
    }

    private static byte[] readAndRestore(VirtualFile file) throws IOException {
        InputStream input = file.getInputStream();
        if (input == null) {
            file.setInputStream(new ByteArrayInputStream(new byte[0]));
            return new byte[0];
        }
        byte[] content;
        try (input) {
            content = input.readAllBytes();
        }
        file.setInputStream(new ByteArrayInputStream(content));
        return content;
    }

    private record OverlayDeclaration(String directory, long[] range) {}
}
