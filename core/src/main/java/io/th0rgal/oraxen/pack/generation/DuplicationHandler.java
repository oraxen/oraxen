package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DuplicationHandler {

    public static final String DUPLICATE_FILE_FOLDER = "migrated_duplicates/";
    public static File getDuplicateItemFile(Material material) {
        return OraxenPlugin.get().getDataFolder().toPath().resolve("items").resolve(DUPLICATE_FILE_FOLDER + "duplicate_" + material.name().toLowerCase(Locale.ROOT) + ".yml").toFile();
    }

    public static void mergeBaseItemFiles(List<VirtualFile> output) {
        Logs.logSuccess("Attempting to merge imported base-item json files");
        Map<String, List<VirtualFile>> baseItemsToMerge = new HashMap<>();

        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().startsWith("assets/minecraft/models/item/")).toList()) {
            if (Material.getMaterial(Utils.getFileNameOnly(virtual.getPath()).toUpperCase()) == null) continue;
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
        Map<String, List<VirtualFile>> fontsToMerge = new HashMap<>();

        // Generate a map of all duplicate fonts
        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().matches("assets/.*/font/.*.json")).toList()) {
            if (fontsToMerge.containsKey(virtual.getPath())) {
                List<VirtualFile> newList = new ArrayList<>(fontsToMerge.get(virtual.getPath()).stream().toList());
                newList.add(virtual);
                fontsToMerge.put(virtual.getPath(), newList);
            } else {
                fontsToMerge.put(virtual.getPath(), List.of(virtual));
            }
        }

        Map<String, List<VirtualFile>> finalFontsToMerge =
                fontsToMerge.entrySet().stream().filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!finalFontsToMerge.isEmpty()) {
            Logs.logWarning("Attempting to merge imported font files...");
            for (List<VirtualFile> duplicates : finalFontsToMerge.values()) {
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
                Logs.logSuccess("Merged " + duplicates.size() + " duplicate font files into a final " + newFont.getPath());
            }
            Logs.logWarning("The imported font files have not been deleted.");
            Logs.logWarning("If anything seems wrong, there might be conflicting unicodes assigned.");
        } else Logs.logSuccess("No duplicate font files found!");
        Logs.newline();
    }

    private static JsonArray getFontProviders(List<VirtualFile> duplicates) {
        JsonArray newProviders = new JsonArray();
        for (VirtualFile font : duplicates) {
            JsonElement fontelement = font.toJsonElement();

            if (fontelement == null || !fontelement.isJsonObject()) continue;

            JsonArray providers = fontelement.getAsJsonObject().getAsJsonArray("providers");
            List<String> newProviderChars = getNewProviderCharSet(newProviders);
            if (providers != null) for (JsonElement providerElement : providers) {
                if (!providerElement.isJsonObject()) continue;
                JsonObject provider = providerElement.getAsJsonObject();
                if (newProviders.contains(providerElement)) continue;
                if (provider.has("chars")) {
                    String chars = provider.getAsJsonArray("chars").toString();
                    if (!newProviderChars.contains(chars)) newProviders.add(provider);
                    else Logs.logWarning("Tried adding " + chars + " but it was already defined in this font");
                } else newProviders.add(provider);
            }
        }
        return newProviders;
    }

    private static List<String> getNewProviderCharSet(JsonArray newProvider) {
        List<String> charList = new ArrayList<>();
        for (JsonElement element : newProvider) {
            if (!element.isJsonObject()) continue;
            if (!element.getAsJsonObject().has("chars")) continue;
            String chars = element.getAsJsonObject().get("chars").getAsJsonArray().toString();
            charList.add(chars);
        }
        return charList;
    }

    private static final String DUPLICATE_LINE_STRING = "// This file was recognized as a duplicate and was migrated into its relevant config(s)";
    /**
     * Check if the file already exists in the zip file
     */
    public static void checkForDuplicate(ZipOutputStream out, ZipEntry entry) {
        String name = entry.getName();
        try {
            out.putNextEntry(entry);
        } catch (IOException e) {
            File duplicateFile;
            Path packFolder = OraxenPlugin.get().getDataFolder().toPath().resolve("pack");
            if (packFolder.resolve(name).toFile().exists()) duplicateFile = packFolder.resolve(name).toFile();
            else duplicateFile = packFolder.resolve(name.replace("assets/minecraft/", "")).toFile();
            List<String> lines = null;
            try {
                if (duplicateFile.getName().endsWith(".json")) lines = FileUtils.readLines(duplicateFile, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                if (Settings.DEBUG.toBool()) ex.printStackTrace();
            }
            if (lines != null && lines.get(0).equals(DUPLICATE_LINE_STRING)) return;

            Logs.logWarning("Duplicate file detected: <blue>" + name + "</blue> - Attempting to migrate it");
            if (!Settings.MERGE_DUPLICATES.toBool()) {
                Logs.logError("Not attempting to migrate duplicate file as <#22b14c>" + Settings.MERGE_DUPLICATES.getPath() + "</#22b14c> is disabled in settings.yml", true);
            } else if (attemptToMigrateDuplicate(name)) {
                Logs.logSuccess("Duplicate file fixed:<blue> " + name);
                try {
                    if (lines == null) lines = FileUtils.readLines(duplicateFile, StandardCharsets.UTF_8);
                    lines.add(0, DUPLICATE_LINE_STRING);
                    FileUtils.writeLines(duplicateFile, lines);
                } catch (Exception ignored) {
                    Logs.logError("Failed to delete the imported <blue>" + Utils.removeParentDirs(name) + "</blue> after migrating it");
                }
                Logs.logSuccess("It is advised to restart your server to ensure that any new conflicts are detected.", true);
            }
        }
    }

    private static boolean attemptToMigrateDuplicate(String name) {
        if (name.matches("assets/minecraft/models/item/.*.json")) {
            Logs.logWarning("Found a duplicate <blue>" + Utils.removeParentDirs(name) + "</blue>, attempting to migrate it into Oraxen item configs");
            return migrateItemJson(name);
        } else if (name.matches("assets/minecraft/sounds.json")) {
            Logs.logWarning("Found a sounds.json duplicate, trying to migrate it into Oraxens sound.yml config");
            return migrateSoundJson(name);
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype_text") && Settings.HIDE_SCOREBOARD_NUMBERS.toBool()) {
            Logs.logWarning("You are importing another copy of a shader file used to hide scoreboard numbers");
            Logs.logWarning("Either disable <#22b14c>" + Settings.HIDE_SCOREBOARD_NUMBERS.getPath() + "</#22b14c> in settings.yml or delete this file");
            return false;
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype_armor_cutout_no_cull") && Settings.CUSTOM_ARMOR_SHADER_GENERATE_FILES.toBool()) {
            Logs.logWarning("You are trying to import a shader file used for custom armor.");
            Logs.logWarning("This shader file is already in your pack. Deleting...");
            return true;
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a shader file");
            Logs.logWarning("Merging this is too advanced and should be migrated manually or deleted.");
            return false;
        } else if (name.startsWith("assets/minecraft/textures/models/armor/leather_layer")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a combined custom armor texture");
            Logs.logWarning("You should not import already combined armor layer files.");
            Logs.logWarning("If you want to handle these files manually, disable <#22b14c>" + Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.getPath() + "</#22b14c> in settings.yml");
            Logs.logWarning("Please refer to https://docs.oraxen.com/configuration/custom-armors for more information. Deleting...");
            return true;
        } else if (name.startsWith("assets/minecraft/textures")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a texture file");
            Logs.logWarning("Cannot migrate texture files, rename this or the duplicate entry");
            return false;
        } else if (name.startsWith("assets/minecraft/lang")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a language file");
            Logs.logWarning("Please combine this with the duplicate file found in Oraxen/pack/lang folder");
            return false;
        } else if (name.matches("assets/minecraft/optifine/%s/armors/.*/.*.properties".formatted(VersionUtil.atOrAbove("1.21") ? "cit_single" : "cit"))) {
            Logs.logWarning("You are trying to import an Optifine CustomArmor file.");
            Logs.logWarning("Oraxen already generates all these needed files for you. Deleting...");
            return true;
        } else {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is not a file that Oraxen can migrate right now");
            Logs.logWarning("Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            return false;
        }
    }

    private static boolean migrateItemJson(String name) {
        String materialName = Utils.removeExtensionOnly(Utils.removeParentDirs(name)).toUpperCase();
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not find a matching material for " + materialName);
            return false;
        }

        if (!name.endsWith(".json")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is not a .json file");
            return false;
        }

        YamlConfiguration migratedYaml = loadMigrateItemYaml(material);
        if (migratedYaml == null) {
            Logs.logWarning("Failed to migrate duplicate file-entry, failed to load " + DuplicationHandler.getDuplicateItemFile(material).getPath());
            return false;
        }
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "pack", name);
        if (!path.toFile().exists()) path = Path.of(path.toString().replace("assets\\minecraft\\", ""));
        String fileContent;
        try {
            fileContent = Files.readString(path);
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not read file");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return false;
        }

        JsonObject json;
        List<JsonObject> overrides;

        try {
            json = JsonParser.parseString(fileContent).getAsJsonObject();
            overrides = new ArrayList<>(json.getAsJsonArray("overrides").asList().stream().filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).distinct().toList());
        } catch (JsonParseException | NullPointerException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not parse json");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return false;
        }

        Map<Integer, List<String>> pullingModels = new HashMap<>();
        Map<Integer, String> chargedModels = new HashMap<>();
        Map<Integer, String> blockingModels = new HashMap<>();
        Map<Integer, String> castModels = new HashMap<>();
        Map<Integer, List<String>> damagedModels = new HashMap<>();
        List<JsonElement> overridesToRemove = new ArrayList<>();

        if (!overrides.isEmpty()) {
            handleBowPulling(overrides, overridesToRemove, pullingModels);
            handleCrossbowPulling(overrides, overridesToRemove, chargedModels);
            handleShieldBlocking(overrides, overridesToRemove, blockingModels);
            handleFishingRodCast(overrides, overridesToRemove, castModels);
            handleDamaged(overrides, overridesToRemove, damagedModels);

            overrides.removeIf(overridesToRemove::contains);

            for (JsonElement element : overrides) {
                JsonObject predicate = element.getAsJsonObject().get("predicate").getAsJsonObject();
                String modelPath = element.getAsJsonObject().get("model").getAsString().replace("\\", "/");
                String id = "migrated_" + modelPath.replaceAll("[^a-zA-Z0-9]+","_");
                // Assume if no cmd is in that it is meant to replace the default model
                int cmd = predicate.has("custom_model_data") ? predicate.get("custom_model_data").getAsInt() : 0;


                migratedYaml.set(id + ".material", materialName);
                migratedYaml.set(id + ".excludeFromInventory", true);
                migratedYaml.set(id + ".excludeFromCommands", true);
                migratedYaml.set(id + ".Pack.generate_model", false);
                migratedYaml.set(id + ".Pack.model", modelPath);
                if (pullingModels.containsKey(cmd)) migratedYaml.set(id + ".Pack.pulling_models", pullingModels.get(cmd));
                if (damagedModels.containsKey(cmd)) migratedYaml.set(id + ".Pack.damaged_models", damagedModels.get(cmd));
                if (chargedModels.containsKey(cmd)) migratedYaml.set(id + ".Pack.charged_model", chargedModels.get(cmd));
                if (blockingModels.containsKey(cmd)) migratedYaml.set(id + ".Pack.blocking_model", blockingModels.get(cmd));
                if (castModels.containsKey(cmd)) migratedYaml.set(id + ".Pack.cast_model", castModels.get(cmd));
                if (Settings.RETAIN_CUSTOM_MODEL_DATA.toBool()) migratedYaml.set(id + ".Pack.custom_model_data", cmd);
            }
        }

        try {
            migratedYaml.save(DuplicationHandler.getDuplicateItemFile(material));
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not save migrated_duplicates.yml");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return false;
        }

        return true;
    }

    private static void handleBowPulling(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, List<String>> pullingModels) {
        handleExtraListPredicates(overrides, overridesToRemove, pullingModels, "pulling");
    }
    private static void handleDamaged(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, List<String>> damagedModels) {
        handleExtraListPredicates(overrides, overridesToRemove, damagedModels, "damaged");
    }

    private static void handleExtraListPredicates(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, List<String>> predicateModels, String predicate) {
        for (JsonObject object : overrides) {
            if (object.get("predicate") == null || !object.get("predicate").isJsonObject()) continue;
            JsonObject predicateObject = object.get("predicate").getAsJsonObject();
            if (predicateObject == null || !predicateObject.has(predicate)) continue;
            int cmd = predicateObject.has("custom_model_data") ? predicateObject.get("custom_model_data").getAsInt() : 0;
            String modelPath = object.get("model").getAsString().replace("\\", "/");
            predicateModels.computeIfAbsent(cmd, k -> new ArrayList<>()).add(modelPath);
            overridesToRemove.add(object);
        }
    }

    private static void handleCrossbowPulling(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, String> chargedModels) {
        handleExtraPredicates(overrides, overridesToRemove, chargedModels, "charged");
    }
    private static void handleShieldBlocking(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, String> blockingModels) {
        handleExtraPredicates(overrides, overridesToRemove, blockingModels, "blocking");
    }
    private static void handleFishingRodCast(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, String> castModels) {
        handleExtraPredicates(overrides, overridesToRemove, castModels, "cast");
    }

    private static void handleExtraPredicates(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove, Map<Integer, String> predicateModels, String predicate) {
        for (JsonObject object : overrides) {
            if (object.get("predicate") == null || !object.get("predicate").isJsonObject()) continue;
            JsonObject predicateObject = object.get("predicate").getAsJsonObject();
            if (predicateObject == null || !predicateObject.has(predicate)) continue;
            int cmd = predicateObject.has("custom_model_data") ? predicateObject.get("custom_model_data").getAsInt() : 0;
            String modelPath = object.get("model").getAsString().replace("\\", "/");
            predicateModels.putIfAbsent(cmd, modelPath);
            overridesToRemove.add(object);
        }
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
            YamlConfiguration soundYaml = OraxenYaml.loadConfiguration(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
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

    private static YamlConfiguration loadMigrateItemYaml(Material material) {

        File file = DuplicationHandler.getDuplicateItemFile(material);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                return null;
            }
        }
        try {
            return OraxenYaml.loadConfiguration(file);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            return null;
        }
    }

    @Deprecated(forRemoval = true)
    public static void convertOldMigrateItemConfig() {
        File oldMigrateConfigFile = OraxenPlugin.get().getDataFolder().toPath().resolve("items/migrated_duplicates.yml").toFile();
        if (!oldMigrateConfigFile.exists()) return;
        YamlConfiguration oldMigrateConfig = OraxenYaml.loadConfiguration(oldMigrateConfigFile);
        Logs.logInfo("Attempting to convert migrated_duplicates.yml into new format");

        Map<String, List<String>> oldMigrateConfigsSorted = new HashMap<>();
        for (String key : oldMigrateConfig.getKeys(false)) {
            String material = oldMigrateConfig.getString(key + ".material").toLowerCase(Locale.ROOT);
            oldMigrateConfigsSorted.computeIfAbsent(material, k -> new ArrayList<>()).add(key);
        }

        for (Map.Entry<String, List<String>> entry : oldMigrateConfigsSorted.entrySet()) {
            String material = entry.getKey();
            List<String> itemIds = entry.getValue();

            File newMigrateConfigFile = OraxenPlugin.get().getDataFolder().toPath().resolve("items/migrated_duplicates/duplicate_" + material + ".yml").toFile();
            if (!newMigrateConfigFile.getParentFile().exists()) newMigrateConfigFile.getParentFile().mkdirs();
            if (!newMigrateConfigFile.exists()) {
                try {
                    newMigrateConfigFile.createNewFile();
                } catch (IOException e) {
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                    continue;
                }
            }

            YamlConfiguration newMigrateConfig = OraxenYaml.loadConfiguration(newMigrateConfigFile);
            for (String oldKey : itemIds) {
                newMigrateConfig.set(oldKey, oldMigrateConfig.get(oldKey));
            }
            try {
                newMigrateConfig.save(newMigrateConfigFile);
            } catch (IOException e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        }

        try {
            Files.delete(oldMigrateConfigFile.toPath());
            Logs.logSuccess("Successfully converted migrated_duplicates.yml into new format");
        } catch (IOException e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
    }
}
