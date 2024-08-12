package io.th0rgal.oraxen.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.FileUtil;
import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DefaultResourcePackExtractor {

    private static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static ResourcePack vanillaResourcePack = ResourcePack.resourcePack();
    private static final File assetPath;
    private static final String version = StringUtils.removeEnd(MinecraftVersion.getCurrentVersion().getVersion(), ".0");

    static {
        assetPath = OraxenPlugin.get().getDataFolder().toPath().resolve("pack/.assetCache/" + version).toFile();
        assetPath.mkdirs();
        FileUtil.setHidden(assetPath.getParentFile().toPath());
    }

    public static void extractLatest(MinecraftResourcePackReader reader) {
        if (assetPath.exists() && !FileUtil.listFiles(assetPath).isEmpty()) {
            readVanillaRP(reader);
            return;
        }

        Logs.logInfo("Extracting latest vanilla-resourcepack...");

        JsonObject versionInfo;
        try {
            versionInfo = downloadJson(findVersionInfoUrl());
        } catch (Exception e) {
            Logs.logWarning("Failed to fetch version-info for vanilla-resourcepack...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage());
            return;
        }
        if (versionInfo == null) return;

        byte[] clientJar = downloadClientJar(versionInfo);
        extractJarAssets(clientJar);
        readVanillaRP(reader);
        Logs.logSuccess("Finished extracting latest vanilla-resourcepack!");
    }

    private static void readVanillaRP(MinecraftResourcePackReader reader) {
        try {
            vanillaResourcePack = reader.readFromDirectory(assetPath);
        } catch (Exception e) {
            Logs.logWarning("Failed to read Vanilla ResourcePack-cache...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
    }

    private static void extractJarAssets(byte[] clientJar) {

        try(ByteArrayInputStream stream = new ByteArrayInputStream(clientJar)) {
            try(ZipInputStream zis = new ZipInputStream(stream)) {
                ZipEntry entry = zis.getNextEntry();
                while(entry != null) {
                    String name = entry.getName();
                    if (name.startsWith("assets/") && !name.endsWith("scaffolding_unstable.json") && !name.startsWith("assets/minecraft/shaders") && !name.startsWith("assets/minecraft/particles")) {
                        File file = checkAndCreateFile(entry);
                        if (entry.isDirectory() && !file.isDirectory() && !file.mkdirs()) {
                            Logs.logWarning("Failed to create directory: " + name);
                        } else {
                            File parent = file.getParentFile();
                            if (!parent.isDirectory() && !parent.mkdirs()) {
                                Logs.logWarning("Failed to create directory: " + parent.getName());
                            }

                            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(zis.readAllBytes())) {
                                FileUtils.copyToFile(inputStream, file);
                            } catch (Exception e) {
                                Logs.logWarning("Failed to extract file: " + file.getName());
                                if (Settings.DEBUG.toBool()) e.printStackTrace();
                            }
                        }
                    }
                    entry = zis.getNextEntry();
                }
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to extract vanilla-resourcepack from client-jar...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
    }

    @Nullable
    private static File checkAndCreateFile(ZipEntry entry) {
        File destFile = assetPath.toPath().resolve(entry.getName()).toFile();
        Path dirPath = assetPath.getAbsoluteFile().toPath();
        Path filePath = destFile.getAbsoluteFile().toPath();

        if (!filePath.startsWith(dirPath + File.separator)) Logs.logWarning("Entry outside target: " + entry.getName());
        return destFile;
    }

    private static byte[] downloadClientJar(JsonObject versionInfo) {
        String url = versionInfo.getAsJsonObject("downloads").getAsJsonObject("client").get("url").getAsString();
        try(InputStream stream = new URI(url).toURL().openStream()) {
            return stream.readAllBytes();
        } catch (Exception e) {
            Logs.logWarning("Failed to download vanilla-resourcepack from: " + url);
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String findVersionInfoUrl() {
        JsonObject manifest = downloadJson(VERSION_MANIFEST_URL);
        if (manifest == null) return null;
        JsonArray versions = manifest.getAsJsonArray("versions");

        for (JsonElement element : versions) {
            if (element instanceof JsonObject obj) {
                String id = obj.get("id").getAsString();
                if (id.equals(version)) return obj.get("url").getAsString();
            }
        }
        return null;
    }

    @Nullable
    private static JsonObject downloadJson(String url) {
        if (url == null) return null;
        JsonObject jsonObject = null;
        try {
            try (InputStream stream = URI.create(url).toURL().openStream()) {
                try(InputStreamReader reader = new InputStreamReader(stream)) {
                    jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to fetch manifest for vanilla-resourcepack...");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage());
        }

        return jsonObject;
    }
}
