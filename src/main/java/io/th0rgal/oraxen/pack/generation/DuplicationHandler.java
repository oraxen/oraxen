package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.lumine.mythic.utils.logging.Log;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DuplicationHandler {

    public DuplicationHandler() {
    }

    public static void mergeBaseItemFiles(List<VirtualFile> output) {
        Logs.logSuccess("Attempting to merge imported base-item json files");
        Map<String, List<VirtualFile>> baseItemsToMerge = new HashMap<>();
        List<String> materials = Arrays.stream(Material.values()).map(Enum::toString).toList();

        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().startsWith("assets/minecraft/models/item/") && materials.contains(Utils.getFileNameOnly(v.getPath()).toUpperCase())).toList()) {
            Material itemMaterial = Material.getMaterial(Utils.getFileNameOnly(virtual.getPath()).toUpperCase());
            if (baseItemsToMerge.containsKey(virtual.getPath())) {
                List<VirtualFile> newList = new ArrayList<>(baseItemsToMerge.get(virtual.getPath()).stream().toList());
                newList.add(virtual);
                baseItemsToMerge.put(virtual.getPath(), newList);
            } else {
                baseItemsToMerge.put(virtual.getPath(), List.of(virtual));
            }
        }

        if (!baseItemsToMerge.isEmpty()) for (List<VirtualFile> duplicates : baseItemsToMerge.values()) {
            if (duplicates.isEmpty()) continue;

            JsonObject mainItem = new JsonObject();
            List<JsonObject> duplicateJsonObjects = duplicates.stream().map(VirtualFile::toJsonElement).filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).toList();
            JsonArray overrides = getItemOverrides(duplicateJsonObjects);
            JsonObject baseTextures = getItemTextures(duplicateJsonObjects);
            String parent = getItemParent(duplicateJsonObjects);
            if (parent != null) parent = "item/generated";

            mainItem.addProperty("parent", parent);
            mainItem.add("overrides", overrides);
            mainItem.add("textures", baseTextures);

            // Generate the template new item file
            VirtualFile first = duplicates.stream().findFirst().get();
            InputStream newInput = new ByteArrayInputStream(mainItem.toString().getBytes(StandardCharsets.UTF_8));
            VirtualFile newItem = new VirtualFile(Utils.getParentDirs(first.getPath()), Utils.removeParentDirs(first.getPath()), newInput);
            newItem.setPath(newItem.getPath().replace("//", "/"));
            newItem.setInputStream(newInput);

            // Remove all the old fonts from output
            output.removeAll(duplicates);
            output.add(newItem);
        }
    }

    private static JsonObject getItemTextures(List<JsonObject> duplicates) {
        JsonObject newTextures = new JsonObject();
        for (JsonObject itemJsons : duplicates) {
            // Check if this itemfile has a different parent model than
            if (itemJsons.has("textures")) {
                JsonObject oldObject = itemJsons.getAsJsonObject("textures");
                for (Map.Entry<String, JsonElement> entry : oldObject.entrySet())
                    if (!newTextures.has(entry.getKey()))
                        newTextures.add(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    private static String getItemParent(List<JsonObject> duplicates) {
        for (JsonObject itemJsons : duplicates) {
            // Check if this itemfile has a different parent model than
            if (itemJsons.getAsJsonObject().has("parent"))
                return itemJsons.getAsJsonObject().get("parent").getAsString();
        }
        return null;
    }

    private static JsonArray getItemOverrides(List<JsonObject> duplicates) {
        JsonArray newProviders = new JsonArray();
        for (JsonObject itemJsons : duplicates) {
            JsonArray providers = itemJsons.getAsJsonArray("overrides");
            Map<JsonObject, String> newOverrides = getNewOverrides(newProviders);
            if (providers != null) for (JsonElement providerElement : providers) {
                if (!providerElement.isJsonObject()) continue;
                if (newProviders.contains(providerElement)) continue;
                if (!providerElement.getAsJsonObject().has("predicate")) continue;
                if (!providerElement.getAsJsonObject().has("model")) continue;

                JsonObject predicate = providerElement.getAsJsonObject().getAsJsonObject("predicate");
                if (!newOverrides.containsKey(predicate))
                    newProviders.add(providerElement);
                else
                    Logs.logWarning("Tried adding " + predicate + " but it was already defined in this item");
            }
        }
        return newProviders;
    }

    private static Map<JsonObject, String> getNewOverrides(JsonArray newOverrides) {
        HashMap<JsonObject, String> overrides = new HashMap<>();
        for (JsonElement element : newOverrides) {
            if (!element.isJsonObject()) continue;
            if (!element.getAsJsonObject().has("predicate")) continue;
            if (!element.getAsJsonObject().has("model")) continue;
            JsonObject predicate = element.getAsJsonObject().get("predicate").getAsJsonObject();
            String modelPath = element.getAsJsonObject().get("model").getAsString();
            overrides.put(predicate, modelPath);
        }
        return overrides;
    }

    //Experimental way of combining 2 fonts instead of making glyphconfigs later
    public static void mergeFontFiles(List<VirtualFile> output) {
        Logs.logSuccess("Attempting to merge imported font files");
        //output.stream().filter(v -> v.getPath().split("/").length > 3 && v.getPath().replaceFirst("assets/.*/font/", "").split("/").length == 1 && v.getPath().endsWith(".json")).collect(Collectors.toSet());
        Map<String, List<VirtualFile>> fontsToMerge = new HashMap<>();

        // Generate a map of all duplicate fonts
        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().split("/").length > 3 && v.getPath().replaceFirst("assets/.*/font/", "").split("/").length == 1 && v.getPath().endsWith(".json")).toList()) {
            if (fontsToMerge.containsKey(virtual.getPath())) {
                List<VirtualFile> newList = new ArrayList<>(fontsToMerge.get(virtual.getPath()).stream().toList());
                newList.add(virtual);
                fontsToMerge.put(virtual.getPath(), newList);
            } else {
                fontsToMerge.put(virtual.getPath(), List.of(virtual));
            }
        }


        if (!fontsToMerge.isEmpty()) for (List<VirtualFile> duplicates : fontsToMerge.values()) {
            if (duplicates.isEmpty()) continue;
            JsonObject mainFont = new JsonObject();
            JsonArray mainFontArray = getFontProviders(duplicates);
            mainFont.add("providers", mainFontArray);

            // Generate the template new font file
            VirtualFile first = duplicates.stream().findFirst().get();
            InputStream newInput = new ByteArrayInputStream(mainFont.toString().getBytes(StandardCharsets.UTF_8));
            VirtualFile newFont = new VirtualFile(Utils.getParentDirs(first.getPath()), Utils.removeParentDirs(first.getPath()), newInput);
            newFont.setPath(newFont.getPath().replace("//", "/"));
            newFont.setInputStream(newInput);

            // Remove all the old fonts from output
            output.removeAll(duplicates);
            output.add(newFont);
        }
    }

    private static JsonArray getFontProviders(List<VirtualFile> duplicates) {
        JsonArray newProviders = new JsonArray();
        for (VirtualFile font : duplicates) {
            JsonElement fontelement = font.toJsonElement();

            if (!fontelement.isJsonObject()) {
                Logs.logError("Not a json object");
                continue;
            }

            JsonArray providers = fontelement.getAsJsonObject().getAsJsonArray("providers");
            List<String> newProviderChars = getNewProviderCharSet(newProviders);
            for (JsonElement providerElement : providers) {
                if (!providerElement.isJsonObject()) continue;
                if (newProviders.contains(providerElement)) continue;
                if (!providerElement.getAsJsonObject().has("chars")) continue;
                String chars = providerElement.getAsJsonObject().getAsJsonArray("chars").toString();
                if (!newProviderChars.contains(chars))
                    newProviders.add(providerElement);
                else
                    Logs.logWarning("Tried adding " + chars + " but it was already defined in this font");
            }
        }
        return newProviders;
    }

    private static List<String> getNewProviderCharSet(JsonArray newProvider) {
        List<String> charList = new ArrayList<>();
        for (JsonElement element : newProvider) {
            if (!element.isJsonObject()) continue;
            if (!element.getAsJsonObject().has("chars")) continue;
            JsonArray chars = element.getAsJsonObject().get("chars").getAsJsonArray();
            charList.add(chars.getAsString());
        }
        return charList;
    }

    /**
     * Check if the file already exists in the zip file
     */
    public static void checkForDuplicate(ZipOutputStream out, ZipEntry entry) {
        String name = entry.getName();
        try {
            out.putNextEntry(entry);
        } catch (IOException e) {
            Logs.logWarning("Duplicate file detected: <blue>" + name + "</blue> - Attempting to migrate it");
            if (!Settings.MERGE_DUPLICATES.toBool()) {
                Logs.logError("Not attempting to migrate duplicate file as <#22b14c>" + Settings.MERGE_DUPLICATES.getPath() + "</#22b14c> is disabled in settings.yml");
            } else if (attemptToMigrateDuplicate(name)) {
                Logs.logSuccess("Duplicate file fixed:<blue> " + name);
                try {
                    OraxenPlugin.get().getDataFolder().toPath().resolve("pack").resolve(name).toFile().delete();
                    Logs.logSuccess("Deleted the imported <blue>" + Utils.removeParentDirs(name) + "</blue> and migrated it to its supported Oraxen config(s)");
                } catch (Exception ignored) {
                    Log.error("Failed to delete the imported <blue>" + Utils.removeParentDirs(name) + "</blue> after migrating it");
                }
                Logs.logSuccess("Might need to restart your server ones before the resourcepack works fully");
            }
            Logs.newline();
        }
    }

    private static boolean attemptToMigrateDuplicate(String name) {
        if (name.matches("assets/minecraft/models/item/.*.json")) {
            Logs.logWarning("Found a duplicate <blue>" + Utils.removeParentDirs(name) + "</blue>, attempting to migrate it into Oraxen item configs");
            return migrateItemJson(name);
        } else if (name.matches("assets/.*/font/.*.json")) {
            Logs.logWarning("Found a duplicated font file, trying to migrate it into Oraxens generated copy");
            return mergeDuplicateFontJson(name);
        } else if (name.matches("assets/.*/sounds.json")) {
            Logs.logWarning("Found a sounds.json duplicate, trying to migrate it into Oraxens sound.yml config");
            return migrateSoundJson(name);
        } else if (name.startsWith("assets/minecraft/shaders")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a shader file");
            Logs.logWarning("Merging this is too advanced and should be migrated manually or deleted.");
            return false;
        } else if (name.startsWith("assets/minecraft/textures/models/armor/leather_layer")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a combined custom armor texture");
            Logs.logWarning("You should not import already combined armor layer files, but individual ones for every armor set you want.");
            Logs.logWarning("Please refer to https://docs.oraxen.com/configuration/custom-armors for more information");
            return false;
        } else if (name.startsWith("assets/minecraft/textures")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a texture file");
            Logs.logWarning("Cannot migrate texture files, rename this or the duplicate entry");
            return false;
        } else {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is not a file that Oraxen can migrate right now");
            Logs.logWarning("Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            return false;
        }
    }

    //TODO
    // Fix importing other aspects of parent-model like shields display
    // It should use the imported as a base and merge the overrides only
    private static boolean migrateItemJson(String name) {
        String itemMaterial = Utils.removeParentDirs(name).replace(".json", "").toUpperCase();
        try {
            Material.valueOf(itemMaterial);
        } catch (IllegalArgumentException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not find material");
            return false;
        }

        if (!name.endsWith(".json")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is not a .json file");
            return false;
        }
        YamlConfiguration migratedYaml = loadMigrateYaml("items");
        if (migratedYaml == null) {
            Logs.logWarning("Failed to migrate duplicate file-entry, failed to load items/migrated_duplicates.yml");
            return false;
        }
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "pack", name);
        String fileContent;
        try {
            fileContent = Files.readString(path);
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not read file");
            return false;
        }

        JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();
        if (json.getAsJsonArray("overrides") != null) for (JsonElement element : json.getAsJsonArray("overrides")) {
            JsonObject predicate = element.getAsJsonObject().get("predicate").getAsJsonObject();
            String modelPath = element.getAsJsonObject().get("model").getAsString().replace("\\", "/");
            String id = "migrated_" + Utils.removeParentDirs(modelPath);
            int cmd;
            try {
                cmd = predicate.get("custom_model_data").getAsInt();
            } catch (NullPointerException e) {
                Logs.logWarning("Failed to migrate duplicate file-entry, could not find custom_model_data");
                return false;
            }

            migratedYaml.set(id + ".material", itemMaterial);
            migratedYaml.set(id + ".excludeFromInventory", true);
            migratedYaml.set(id + ".excludeFromCommands", true);
            migratedYaml.set(id + ".Pack.generate_model", false);
            migratedYaml.set(id + ".Pack.model", modelPath);
            if (Settings.RETAIN_CUSTOM_MODEL_DATA.toBool()) migratedYaml.set(id + ".Pack.custom_model_data", cmd);
        }

        try {
            migratedYaml.save(new File(OraxenPlugin.get().getDataFolder(), "/items/migrated_duplicates.yml"));
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not save migrated_duplicates.yml");
            return false;
        }

        return true;
    }

    private static boolean migrateSoundJson(String name) {
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/", name);
        try {
            String fileContent;
            try {
                fileContent = Files.readString(path);
            } catch (IOException e) {
                Logs.logWarning("Failed to migrate duplicate file-entry, could not read file");
                return false;
            }

            JsonObject sounds = JsonParser.parseString(fileContent).getAsJsonObject();
            YamlConfiguration soundYaml = YamlConfiguration.loadConfiguration(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
            for (String id : sounds.keySet()) {
                if (soundYaml.contains("sounds." + id)) {
                    Logs.logWarning("Sound " + id + " is already defined in sound.yml, skipping");
                    continue;
                }
                JsonObject sound = sounds.get(id).getAsJsonObject();
                boolean replace = sound.get("replace") != null && sound.get("replace").getAsBoolean();
                String category = sound.get("category") != null && sound.get("category").getAsString() != null ? sound.get("category").getAsString() : null;
                String subtitle = sound.get("subtitle").getAsString() != null ? sound.get("subtitle").getAsString() : null;
                JsonArray soundArray = sound.getAsJsonArray("sounds");
                List<String> soundList = new ArrayList<>();
                if (soundArray != null) for (JsonElement s : soundArray) soundList.add(s.getAsString());

                soundYaml.set("sounds." + id + ".replace", replace);
                soundYaml.set("sounds." + id + ".category", category != null ? category : "master");
                if (subtitle != null) soundYaml.set("sounds." + id + ".subtitle", subtitle);
                soundYaml.set("sounds." + id + ".sounds", soundList);

                try {
                    soundYaml.save(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
                    Logs.logSuccess("Successfully migrated sound <blue>" + id + "</blue> into sound.yml");
                } catch (IOException e) {
                    Logs.logWarning("Failed to migrate duplicate file-entry, could not save <blue>" + id + "</blue> to sound.yml");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static boolean mergeDuplicateFontJson(String name) {
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/", name);

        return true;
    }

    private static boolean migrateDefaultFontJson(String name) {
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/pack/", name);
        try {
            YamlConfiguration glyphYaml = loadMigrateYaml("glyphs");

            if (glyphYaml == null) {
                Logs.logWarning("Failed to migrate duplicate file-entry, failed to load glyphs/migrated_duplicates.yml");
                return false;
            }

            String fileContent;
            try {
                fileContent = Files.readString(path);
            } catch (IOException e) {
                Logs.logWarning("Failed to migrate duplicate file-entry, could not read file");
                return false;
            }

            JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();
            if (json.getAsJsonArray("providers") == null) {
                Logs.logWarning("Failed to migrate duplicate file-entry, file is not a valid font file");
                return false;
            }

            for (JsonElement element : json.getAsJsonArray("providers")) {
                JsonObject provider = element.getAsJsonObject();
                String file = provider.get("file").getAsString();
                JsonArray charsArray = provider.getAsJsonArray("chars");
                List<String> charList = new ArrayList<>();
                if (charsArray != null) for (JsonElement c : charsArray) charList.add(c.getAsString());
                int ascent, height;

                try {
                    ascent = provider.get("ascent").getAsInt();
                    height = provider.get("height").getAsInt();
                } catch (IllegalStateException e) {
                    ascent = 8;
                    height = 8;
                }

                glyphYaml.set("glyphs." + file + ".file", file != null ? file : "required/exit_icon.png");
                glyphYaml.set("glyphs." + file + ".chars", charList);
                glyphYaml.set("glyphs." + file + ".ascent", ascent);
                glyphYaml.set("glyphs." + file + ".height", height);

                try {
                    glyphYaml.save(new File(OraxenPlugin.get().getDataFolder(), "/glyphs/migrated_duplicates.yml"));
                } catch (IOException e) {
                    Logs.logWarning("Failed to migrate duplicate file-entry, could not save migrated_duplicates.yml");
                    return false;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static YamlConfiguration loadMigrateYaml(String folder) {

        File file = OraxenPlugin.get().getDataFolder().toPath().toAbsolutePath().resolve(folder).resolve("migrated_duplicates.yml").toFile();
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
