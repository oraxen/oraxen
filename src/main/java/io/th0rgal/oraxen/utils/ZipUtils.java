package io.th0rgal.oraxen.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {
    }

    public static void writeZipFile(final File outputFile, final File directoryToZip,
                                    final List<VirtualFile> fileList) {

        try (final FileOutputStream fos = new FileOutputStream(outputFile);
             final ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
            final int compressionLevel = Deflater.class.getDeclaredField(Settings.COMPRESSION.toString()).getInt(null);
            zos.setLevel(compressionLevel);
            zos.setComment(Settings.COMMENT.toString());
            for (final VirtualFile file : fileList)
                addToZip(file.getPath(),
                        file.getInputStream(),
                        zos);

        } catch (final IOException | NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static void addToZip(String zipFilePath, final InputStream fis, ZipOutputStream zos) throws IOException {
        final ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipEntry.setLastModifiedTime(FileTime.fromMillis(0L));
        checkForDuplicate(zos, zipEntry);

        final byte[] bytes = new byte[1024];
        int length;
        try (fis) {
            while ((length = fis.read(bytes)) >= 0)
                zos.write(bytes, 0, length);
        } catch (IOException ignored) {
        } finally {
            zos.closeEntry();
            if (Settings.PROTECTION.toBool()) {
                zipEntry.setCrc(bytes.length);
                zipEntry.setSize(new BigInteger(bytes).mod(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
            }
        }
    }

    /**
     * Check if the file already exists in the zip file
     * In the future this method will handle and merge files aswell
     */
    private static void checkForDuplicate(ZipOutputStream out, ZipEntry entry) {
        String name = entry.getName();
        try {
            out.putNextEntry(entry);
        } catch (IOException e) {
            Logs.logWarning("Duplicate file detected: " + name + " - Attempting to merge it");
            if (!Settings.ATTEMPT_TO_MERGE_DUPLICATES.toBool()) {
                Logs.logError("Not attempting to merge duplicate file as it is disabled in settings.yml");
            }
            if (attemptToMergeDuplicate(name)) {
                Logs.logAsComponent("<prefix><#55ffa4>Duplicate file fixed: " + name);
                Logs.logAsComponent("<prefix><#55ffa4>Deleted the imported " + Utils.getLastStringInSplit(name, "/") + "and migrated it over to config entries in merged.yml");
                Logs.logAsComponent("<prefix><#55ffa4>Might need to restart your server ones before the resourcepack works fully");
                OraxenPlugin.get().getDataFolder().toPath().resolve("pack/" + name).toFile().delete();
            } else {
                Logs.logError("Failed to merge duplicate file: " + name + ", to configs");
                Logs.logError("Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            }
        }
    }

    private static boolean attemptToMergeDuplicate(String name) {
        String itemMaterial = Utils.getLastStringInSplit(name, "/").split(".json")[0].toUpperCase();
        try {
            Material.valueOf(itemMaterial);
        } catch (IllegalArgumentException e) {
            if (!attemptToMergeDuplicateNonItem(name))
                Logs.logWarning("Failed to merge duplicate file-entry, could not find material");
            return false;
        }

        if (!name.endsWith(".json")) {
            Logs.logWarning("Failed to merge duplicate file-entry, file is not a .json file");
            return false;
        }
        YamlConfiguration mergedYaml = loadMergedYaml();
        if (mergedYaml == null) {
            Logs.logWarning("Failed to merge duplicate file-entry, failed to load merged_duplicates.yml");
            return false;
        }
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "\\pack\\", name);
        String fileContent;
        try {
            fileContent = Files.readString(path);
        } catch (IOException e) {
            Logs.logWarning("Failed to merge duplicate file-entry, could not read file");
            return false;
        }

        JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();
        if (json.getAsJsonArray("overrides") != null) for (JsonElement s : json.getAsJsonArray("overrides")) {
            JsonObject predicate = s.getAsJsonObject().get("predicate").getAsJsonObject();
            String modelPath = s.getAsJsonObject().get("model").getAsString().replace("\\", "/");
            String id = "merged_" + Utils.getLastStringInSplit(modelPath.split(":")[1], "/");
            int cmd;
            try {
                cmd = predicate.get("custom_model_data").getAsInt();
            } catch (NullPointerException e) {
                Logs.logWarning("Failed to merge duplicate file-entry, could not find custom_model_data");
                return false;
            }

            mergedYaml.set(id + ".material", itemMaterial);
            mergedYaml.set(id + ".excludeFromInventory", true);
            mergedYaml.set(id + ".excludeFromCommands", true);
            mergedYaml.set(id + ".Pack.custom_model_data", cmd);
            mergedYaml.set(id + ".Pack.model", modelPath);
            mergedYaml.set(id + ".Pack.generate_model", false);
        }

        try {
            mergedYaml.save(new File(OraxenPlugin.get().getDataFolder(), "/items/merged_duplicates.yml"));
        } catch (IOException e) {
            Logs.logWarning("Failed to merge duplicate file-entry, could not save merged_duplicates.yml");
            return false;
        }

        return true;
    }

    private static boolean attemptToMergeDuplicateNonItem(String name) {
        if (name.startsWith("assets/minecraft/shaders")) {
            Logs.logWarning("Failed to merge duplicate file-entry, file is a shader file");
            Logs.logWarning("Merging this is too advanced and should be done manually.");
            return false;
        } else if (name.startsWith("assets/minecraft/textures/models/armor/leather_layer")) {
            Logs.logWarning("Failed to merge duplicate file-entry, file is a file for custom armor");
            Logs.logWarning("You should not import already combined armor layer files, but individual ones for every armor set you want.");
            Logs.logWarning("Please refer to https://docs.oraxen.com/configuration/custom-armors for more information");
            return false;
        } else if (name.startsWith("assets/minecraft/textures")) {
            Logs.logWarning("Failed to merge duplicate file-entry, file is a texture file");
            Logs.logWarning("Cannot merge texture files, rename this or the duplicate entry");
            return false;
        } else if (name.matches("assets/.*/sounds.json")) {
            Logs.logWarning("Found a sounds.json duplicate, trying to merge it into Oraxens sound.yml config");
            return mergeSoundJson(name);
        } else {
            Logs.logWarning("Failed to merge duplicate file-entry, file is not a file that Oraxen can merge right now");
            Logs.logWarning("Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            return false;
        }
    }

    private static boolean mergeSoundJson(String name) {
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/", name);
        try {
            String content = Files.readString(path);
            JsonObject sounds = JsonParser.parseString(content).getAsJsonObject();
            YamlConfiguration soundYaml = YamlConfiguration.loadConfiguration(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
            for (String id : sounds.keySet()) {
                Logs.logInfo("Found sound: " + id);
                if (soundYaml.contains("sounds." + id)) {
                    Logs.logWarning("Sound already exists in sound.yml, skipping");
                    continue;
                }
                JsonObject sound = sounds.get(id).getAsJsonObject();
                boolean replace = sound.get("replace") != null && sound.get("replace").getAsBoolean();
                String category = sound.get("category").getAsString();
                String subtitle = sound.get("subtitle") != null ? sound.get("subtitle").getAsString() : null;
                JsonArray soundArray = sound.getAsJsonArray("sounds") != null ? sound.getAsJsonArray("sounds") : null;
                List<String> soundList = new ArrayList<>();
                if (soundArray != null) for (JsonElement s : soundArray) soundList.add(s.getAsString());

                soundYaml.set("sounds." + id + ".replace", replace);
                soundYaml.set("sounds." + id + ".category", category != null ? category : "master");
                if (subtitle != null) soundYaml.set("sounds." + id + ".subtitle", subtitle);
                soundYaml.set("sounds." + id + ".sounds", soundList);

                try {
                    soundYaml.save(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
                    Logs.logAsComponent("<prefix><#55ffa4>Successfully merged sound " + id + " into sound.yml");
                } catch (IOException e) {
                    Logs.logWarning("Failed to merge duplicate file-entry, could not save " + id + " to sound.yml");
                    return false;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static YamlConfiguration loadMergedYaml() {
        File file = new File(OraxenPlugin.get().getDataFolder(), "\\items\\" + "merged_duplicates.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }
        try {
            return YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            return null;
        }
    }
}
