package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackConvertor {

    public PackConvertor() {
    }

    public static void handlePackConversionFor_1_19_3(List<VirtualFile> output) {
        Logs.logSuccess("Generating atlas-file for 1.19.3 Resource Pack format");
        generateAtlasFile(output);
    }

    private static void generateAtlasFile(List<VirtualFile> output) {
        JsonObject atlas = new JsonObject();
        JsonArray atlasContent = new JsonArray();
        Map<VirtualFile, String> textureSubFolders = new HashMap<>();

        for (VirtualFile v : output) textureSubFolders.put(v, v.getPath().replaceFirst("assets/.*/textures/", "").split("/")[0]);
        textureSubFolders.forEach((virtual, path) -> {
            JsonObject atlasEntry = new JsonObject();
            if (path.contains(".png")) {
                String namespace = virtual.getPath().replaceFirst("assets/", "").split("/")[0];
                String sprite = Utils.getLastStringInSplit(virtual.getPath(), "/").split("\\.")[0];
                atlasEntry.addProperty("type", "single");
                atlasEntry.addProperty("resource", namespace + ":" + path.split("\\.")[0]);
                atlasEntry.addProperty("sprite", sprite);
            } else {
                atlasEntry.addProperty("type", "directory");
                atlasEntry.addProperty("source", path);
                atlasEntry.addProperty("prefix", path + "/");
            }

            if (!atlasContent.contains(atlasEntry))
                atlasContent.add(atlasEntry);
        });

        atlas.add("sources", atlasContent);
        VirtualFile atlasFile = new VirtualFile("assets/minecraft/atlases", "blocks.json", new ByteArrayInputStream(atlas.toString().getBytes(StandardCharsets.UTF_8)));
        output.removeIf(v -> v.getPath().equals(atlasFile.getPath()));
        output.add(atlasFile);
    }
}
