package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorType;
import io.th0rgal.oraxen.utils.customarmor.ShaderArmorTextures;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects files from the pack folder into VirtualFile objects and provides
 * static pack verification utilities.
 * Extracted from ResourcePack to reduce class size.
 */
class PackFileCollector {

    private final File packFolder;
    private final ShaderArmorTextures shaderArmorTextures;

    PackFileCollector(File packFolder, ShaderArmorTextures shaderArmorTextures) {
        this.packFolder = packFolder;
        this.shaderArmorTextures = shaderArmorTextures;
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

    static Set<String> verifyPackFormatting(List<VirtualFile> output) {
        if (Settings.DEBUG.toBool()) Logs.logInfo("Verifying formatting for textures and models...");
        Set<VirtualFile> textures = new HashSet<>();
        Set<String> texturePaths = new HashSet<>();
        Set<String> mcmeta = new HashSet<>();
        Set<VirtualFile> models = new HashSet<>();
        Set<VirtualFile> malformedTextures = new HashSet<>();
        Set<VirtualFile> malformedModels = new HashSet<>();
        for (VirtualFile virtualFile : output) {
            String path = virtualFile.getPath();
            if (path.matches("assets/.*/models/.*.json"))
                models.add(virtualFile);
            else if (path.matches("assets/.*/textures/.*.png.mcmeta"))
                mcmeta.add(path);
            else if (path.matches("assets/.*/textures/.*.png")) {
                textures.add(virtualFile);
                texturePaths.add(path);
            }
        }

        if (models.isEmpty() && !textures.isEmpty())
            return Collections.emptySet();

        for (VirtualFile model : models) {
            if (!model.getPath().matches("[a-z0-9/._-]+")) {
                Logs.logWarning("Found invalid model at <blue>" + model.getPath());
                Logs.logError("Model-paths must only contain characters [a-z0-9/._-]");
                malformedModels.add(model);
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
                    e.printStackTrace();
                    continue;
                }
                if (jsonModel.has("textures")) {
                    for (JsonElement element : jsonModel.getAsJsonObject("textures").entrySet().stream()
                            .map(Map.Entry::getValue).toList()) {
                        String jsonTexture = element.getAsString();
                        if (!texturePaths.contains(modelPathToPackPath(jsonTexture))) {
                            if (!jsonTexture.startsWith("#") && !jsonTexture.startsWith("item/")
                                    && !jsonTexture.startsWith("block/") && !jsonTexture.startsWith("entity/")) {
                                if (Material.matchMaterial(Utils.getFileNameOnly(jsonTexture).toUpperCase()) == null) {
                                    Logs.logWarning("Found invalid texture-path inside model-file <blue>"
                                            + model.getPath() + "</blue>: " + jsonTexture);
                                    Logs.logWarning("Verify that you have a texture in said path.", true);
                                    malformedModels.add(model);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (VirtualFile texture : textures) {
            if (!texture.getPath().matches("[a-z0-9/._-]+")) {
                Logs.logWarning("Found invalid texture at <blue>" + texture.getPath());
                Logs.logError("Texture-paths must only contain characters [a-z0-9/._-]");
                malformedTextures.add(texture);
            }
            if (!texture.getPath().matches(".*_layer_.*.png")) {
                if (mcmeta.contains(texture.getPath() + ".mcmeta"))
                    continue;
                try {
                    InputStream inputStream = texture.getInputStream();
                    if (inputStream == null) {
                        Logs.logWarning("Found unreadable texture at <blue>" + texture.getPath() + "</blue>");
                        malformedTextures.add(texture);
                        continue;
                    }

                    byte[] data;
                    try (inputStream) {
                        data = inputStream.readAllBytes();
                    }
                    // Important: restore stream for later zip writing
                    texture.setInputStream(new ByteArrayInputStream(data));

                    // ImageIO.read returns null if there is no suitable reader
                    // (corrupt/unsupported)
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
                    if (image == null) {
                        Logs.logWarning("Found unreadable texture at <blue>" + texture.getPath() + "</blue>");
                        Logs.logWarning("Image format may be corrupt or unsupported by ImageIO.", true);
                        malformedTextures.add(texture);
                        continue;
                    }

                } catch (Exception e) {
                    // Be resilient when validating packs: bad files should not crash pack
                    // generation
                    Logs.logWarning("Failed to validate texture <blue>" + texture.getPath() + "</blue>");
                    if (Settings.DEBUG.toBool())
                        e.printStackTrace();
                    malformedTextures.add(texture);
                }
            }
        }

        if (!malformedTextures.isEmpty() || !malformedModels.isEmpty()) {
            Logs.logError("Pack contains malformed texture(s) and/or model(s)");
            Logs.logError("These need to be fixed, otherwise the resourcepack will be broken");
        } else
            Logs.logSuccess("No broken models or textures were found in the resourcepack");

        Set<String> malformedFiles = malformedTextures.stream().map(VirtualFile::getPath).collect(Collectors.toSet());
        malformedFiles.addAll(malformedModels.stream().map(VirtualFile::getPath).collect(Collectors.toSet()));
        return malformedFiles;
    }

    static String modelPathToPackPath(String modelPath) {
        String namespace = modelPath.split(":").length == 1 ? "minecraft" : modelPath.split(":")[0];
        String texturePath = modelPath.split(":").length == 1 ? modelPath : modelPath.split(":")[1];
        texturePath = texturePath.endsWith(".png") ? texturePath : texturePath + ".png";
        return "assets/" + namespace + "/textures/" + texturePath;
    }

    private void readFileToVirtuals(final Collection<VirtualFile> output, File file, String newFolder) {
        try {
            final InputStream fis;
            if (file.getName().endsWith(".json"))
                fis = processJsonFile(file);
            else if (CustomArmorType.getSetting() == CustomArmorType.SHADER && shaderArmorTextures.registerImage(file))
                return;
            else
                fis = new FileInputStream(file);

            output.add(new VirtualFile(getZipFilePath(file.getParentFile().getCanonicalPath(), newFolder),
                    file.getName(), fis));
        } catch (IOException e) {
            e.printStackTrace();
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

        // If the json file is a font file, do not format it through MiniMessage
        if (file.getPath().replace("\\", "/").split("assets/.*/font/").length > 1) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        return processJson(content);
    }

    static InputStream processJson(String content) {
        String parsedContent = AdventureUtils.parseLegacyThroughMiniMessage(content).replace("\\<", "<");
        try (InputStream newStream = new ByteArrayInputStream(parsedContent.getBytes(StandardCharsets.UTF_8))) {
            return newStream;
        } catch (IOException e) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
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
