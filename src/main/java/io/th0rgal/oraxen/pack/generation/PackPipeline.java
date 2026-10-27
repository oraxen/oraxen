package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.VirtualFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    static JsonObject languageWithDefaults(JsonObject defaults, JsonObject locale) {
        return PackAssetMerger.mergeJson("assets/minecraft/lang/locale.json", defaults, locale);
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
}
