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
        return OraxenPlugin.get().getDataFolder().toPath().resolve("items")
                .resolve(DUPLICATE_FILE_FOLDER + "duplicate_" + material.name().toLowerCase(Locale.ROOT) + ".yml")
                .toFile();
    }

    public static void mergeBaseItemFiles(List<VirtualFile> output) {
        Logs.logSuccess("Attempting to merge imported base-item json files");
        Map<String, List<VirtualFile>> baseItemsToMerge = new HashMap<>();

        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().startsWith("assets/minecraft/models/item/"))
                .toList()) {
            if (Material.getMaterial(Utils.getFileNameOnly(virtual.getPath()).toUpperCase()) == null)
                continue;
            if (baseItemsToMerge.containsKey(virtual.getPath())) {
                List<VirtualFile> newList = new ArrayList<>(baseItemsToMerge.get(virtual.getPath()).stream().toList());
                newList.add(virtual);
                baseItemsToMerge.put(virtual.getPath(), newList);
            } else {
                baseItemsToMerge.put(virtual.getPath(), List.of(virtual));
            }
        }

        if (!baseItemsToMerge.isEmpty())
            for (List<VirtualFile> duplicates : baseItemsToMerge.values()) {
                if (duplicates.isEmpty())
                    continue;

                JsonObject mainItem = new JsonObject();
                List<JsonObject> duplicateJsonObjects = duplicates.stream().map(VirtualFile::toJsonElement)
                        .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).toList();
                JsonArray overrides = getItemOverrides(duplicateJsonObjects);
                JsonObject baseTextures = getItemTextures(duplicateJsonObjects);
                String parent = getItemParent(duplicateJsonObjects);
                if (parent != null)
                    parent = "item/generated";

                mainItem.addProperty("parent", parent);
                mainItem.add("overrides", overrides);
                mainItem.add("textures", baseTextures);

                // Generate the template new item file
                VirtualFile first = duplicates.stream().findFirst().get();
                InputStream newInput = new ByteArrayInputStream(mainItem.toString().getBytes(StandardCharsets.UTF_8));
                VirtualFile newItem = new VirtualFile(Utils.getParentDirs(first.getPath()),
                        Utils.removeParentDirs(first.getPath()), newInput);
                newItem.setPath(newItem.getPath().replace("//", "/"));
                newItem.setInputStream(newInput);

                // Remove all the old fonts from output
                output.removeAll(duplicates);
                output.add(newItem);
            }
    }

    /**
     * Merges vanilla item definition files (assets/minecraft/items/*.json) for
     * 1.21.4+.
     * This handles external packs that may include their own range_dispatch entries
     * for custom_model_data, merging them with Oraxen's generated definitions.
     */
    public static void mergeVanillaItemDefinitions(List<VirtualFile> output) {
        if (!VersionUtil.atOrAbove("1.21.4"))
            return;
        if (!Settings.APPEARANCE_PREDICATES.toBool())
            return;

        // First, convert legacy predicate overrides from external packs to modern item
        // definitions
        convertLegacyPredicatesToItemDefinitions(output);

        Map<String, List<VirtualFile>> itemDefsToMerge = new HashMap<>();

        // Find all vanilla item definition files
        for (VirtualFile virtual : output.stream()
                .filter(v -> v.getPath().startsWith("assets/minecraft/items/") && v.getPath().endsWith(".json"))
                .toList()) {
            String path = virtual.getPath();
            itemDefsToMerge.computeIfAbsent(path, k -> new ArrayList<>()).add(virtual);
        }

        // Only process paths with duplicates
        Map<String, List<VirtualFile>> duplicatesOnly = itemDefsToMerge.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (duplicatesOnly.isEmpty())
            return;

        Logs.logSuccess("Attempting to merge " + duplicatesOnly.size() + " duplicate vanilla item definition files");

        for (Map.Entry<String, List<VirtualFile>> entry : duplicatesOnly.entrySet()) {
            List<VirtualFile> duplicates = entry.getValue();
            if (duplicates.size() < 2)
                continue;

            // Parse all duplicates and merge their range_dispatch entries
            JsonObject mergedDefinition = mergeItemDefinitionEntries(duplicates);
            if (mergedDefinition == null)
                continue;

            // Create merged file
            VirtualFile first = duplicates.get(0);
            InputStream newInput = new ByteArrayInputStream(
                    mergedDefinition.toString().getBytes(StandardCharsets.UTF_8));
            VirtualFile merged = new VirtualFile(
                    Utils.getParentDirs(first.getPath()),
                    Utils.removeParentDirs(first.getPath()),
                    newInput);
            merged.setPath(merged.getPath().replace("//", "/"));

            // Replace duplicates with merged file
            output.removeAll(duplicates);
            output.add(merged);
            Logs.logSuccess("Merged " + duplicates.size() + " item definitions into " + merged.getPath());
        }
    }

    /**
     * Converts legacy predicate overrides from external packs
     * (assets/minecraft/models/item/*.json)
     * to modern 1.21.4+ vanilla item definitions (assets/minecraft/items/*.json).
     * Only creates definitions for materials that don't already have one generated
     * by Oraxen.
     */
    private static void convertLegacyPredicatesToItemDefinitions(List<VirtualFile> output) {
        // Find all existing item definitions (to avoid duplicating)
        Set<String> existingItemDefs = output.stream()
                .filter(v -> v.getPath().startsWith("assets/minecraft/items/") && v.getPath().endsWith(".json"))
                .map(v -> Utils.getFileNameOnly(v.getPath()).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // Find legacy model files that have custom_model_data predicates
        List<VirtualFile> legacyModels = output.stream()
                .filter(v -> v.getPath().startsWith("assets/minecraft/models/item/") && v.getPath().endsWith(".json"))
                .toList();

        List<VirtualFile> newDefinitions = new ArrayList<>();

        for (VirtualFile legacyModel : legacyModels) {
            String itemName = Utils.getFileNameOnly(legacyModel.getPath()).toLowerCase(Locale.ROOT);

            // Skip if we already have a modern item definition for this material
            if (existingItemDefs.contains(itemName))
                continue;

            // Check if it's a vanilla base item (by trying to get Material)
            Material material = Material.getMaterial(itemName.toUpperCase(Locale.ROOT));
            if (material == null)
                continue;

            // Parse the legacy model
            JsonElement jsonElement = legacyModel.toJsonElement();
            if (jsonElement == null || !jsonElement.isJsonObject())
                continue;
            JsonObject legacyJson = jsonElement.getAsJsonObject();

            // Check if it has custom_model_data overrides
            if (!legacyJson.has("overrides"))
                continue;
            JsonArray overrides = legacyJson.getAsJsonArray("overrides");
            if (overrides == null || overrides.isEmpty())
                continue;

            // Extract custom_model_data entries
            List<CmdOverride> cmdOverrides = extractCustomModelDataOverrides(overrides);
            if (cmdOverrides.isEmpty())
                continue;

            // Create modern item definition
            JsonObject itemDef = createItemDefinitionFromOverrides(material, cmdOverrides, legacyJson);
            if (itemDef == null)
                continue;

            // Add to output
            VirtualFile newDef = new VirtualFile(
                    "assets/minecraft/items",
                    itemName + ".json",
                    new ByteArrayInputStream(itemDef.toString().getBytes(StandardCharsets.UTF_8)));
            newDefinitions.add(newDef);
            existingItemDefs.add(itemName);

            Logs.logSuccess("Converted legacy predicates for " + itemName + " to modern item definition");
        }

        output.addAll(newDefinitions);
    }

    /**
     * Extracts custom_model_data overrides from a legacy overrides array.
     */
    private static List<CmdOverride> extractCustomModelDataOverrides(JsonArray overrides) {
        List<CmdOverride> result = new ArrayList<>();
        for (JsonElement element : overrides) {
            if (!element.isJsonObject())
                continue;
            JsonObject override = element.getAsJsonObject();
            if (!override.has("predicate") || !override.has("model"))
                continue;

            JsonObject predicate = override.getAsJsonObject("predicate");
            if (!predicate.has("custom_model_data"))
                continue;

            int cmd = predicate.get("custom_model_data").getAsInt();
            String model = override.get("model").getAsString();
            result.add(new CmdOverride(cmd, model));
        }
        // Sort by CMD value
        result.sort(Comparator.comparingInt(CmdOverride::cmd));
        return result;
    }

    /**
     * Creates a modern item definition from legacy CMD overrides.
     */
    private static JsonObject createItemDefinitionFromOverrides(Material material, List<CmdOverride> overrides,
            JsonObject legacyJson) {
        JsonObject root = new JsonObject();

        // Build range_dispatch
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:custom_model_data");

        JsonArray entries = new JsonArray();
        for (CmdOverride override : overrides) {
            JsonObject entry = new JsonObject();
            entry.addProperty("threshold", override.cmd);

            JsonObject model = new JsonObject();
            model.addProperty("type", "minecraft:model");
            model.addProperty("model", override.model);
            entry.add("model", model);

            entries.add(entry);
        }
        rangeDispatch.add("entries", entries);

        // Fallback to vanilla model
        JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "minecraft:model");
        fallback.addProperty("model", "minecraft:item/" + material.name().toLowerCase(Locale.ROOT));
        rangeDispatch.add("fallback", fallback);

        root.add("model", rangeDispatch);
        return root;
    }

    /**
     * Record for CMD override data.
     */
    private record CmdOverride(int cmd, String model) {
    }

    /**
     * Holds collected range_dispatch data during merging.
     */
    private static class RangeDispatchData {
        final Set<Integer> seenThresholds = new HashSet<>();
        final List<JsonObject> entries = new ArrayList<>();
        JsonObject fallback = null;
    }

    /**
     * Merges range_dispatch entries from multiple item definition files.
     */
    private static JsonObject mergeItemDefinitionEntries(List<VirtualFile> duplicates) {
        List<JsonObject> jsonObjects = parseJsonObjects(duplicates);
        if (jsonObjects.isEmpty())
            return null;

        JsonObject base = jsonObjects.get(0).deepCopy();
        RangeDispatchData data = collectRangeDispatchData(jsonObjects);

        if (!data.entries.isEmpty()) {
            base.add("model", buildRangeDispatchModel(data));
        }

        return base;
    }

    private static List<JsonObject> parseJsonObjects(List<VirtualFile> files) {
        return files.stream()
                .map(VirtualFile::toJsonElement)
                .filter(e -> e != null && e.isJsonObject())
                .map(JsonElement::getAsJsonObject)
                .toList();
    }

    private static RangeDispatchData collectRangeDispatchData(List<JsonObject> definitions) {
        RangeDispatchData data = new RangeDispatchData();

        for (JsonObject def : definitions) {
            JsonObject model = getNestedModel(def);
            if (model == null || !isCustomModelDataRangeDispatch(model))
                continue;

            collectEntriesFromModel(model, data);
            collectFallbackFromModel(model, data);
        }

        return data;
    }

    private static void collectEntriesFromModel(JsonObject model, RangeDispatchData data) {
        if (!model.has("entries"))
            return;

        for (JsonElement entryEl : model.getAsJsonArray("entries")) {
            if (!entryEl.isJsonObject())
                continue;

            JsonObject entry = entryEl.getAsJsonObject();
            if (!entry.has("threshold"))
                continue;

            int threshold = entry.get("threshold").getAsInt();
            if (!data.seenThresholds.contains(threshold)) {
                data.seenThresholds.add(threshold);
                data.entries.add(entry.deepCopy());
            }
        }
    }

    private static void collectFallbackFromModel(JsonObject model, RangeDispatchData data) {
        if (data.fallback == null && model.has("fallback")) {
            data.fallback = model.getAsJsonObject("fallback").deepCopy();
        }
    }

    private static JsonObject buildRangeDispatchModel(RangeDispatchData data) {
        JsonObject rangeDispatch = new JsonObject();
        rangeDispatch.addProperty("type", "minecraft:range_dispatch");
        rangeDispatch.addProperty("property", "minecraft:custom_model_data");

        data.entries.sort(Comparator.comparingInt(e -> e.get("threshold").getAsInt()));

        JsonArray sortedArray = new JsonArray();
        data.entries.forEach(sortedArray::add);
        rangeDispatch.add("entries", sortedArray);

        if (data.fallback != null) {
            rangeDispatch.add("fallback", data.fallback);
        }

        return rangeDispatch;
    }

    /**
     * Gets the model object from an item definition, traversing nested structures.
     */
    private static JsonObject getNestedModel(JsonObject definition) {
        if (!definition.has("model"))
            return null;
        JsonElement model = definition.get("model");
        if (!model.isJsonObject())
            return null;
        return model.getAsJsonObject();
    }

    /**
     * Checks if a model object is a range_dispatch using custom_model_data.
     */
    private static boolean isCustomModelDataRangeDispatch(JsonObject model) {
        if (!model.has("type"))
            return false;
        String type = model.get("type").getAsString();
        if (!"minecraft:range_dispatch".equals(type))
            return false;
        if (!model.has("property"))
            return false;
        String property = model.get("property").getAsString();
        return "minecraft:custom_model_data".equals(property);
    }

    private static JsonObject getItemTextures(List<JsonObject> duplicates) {
        JsonObject newTextures = new JsonObject();
        for (JsonObject itemJsons : duplicates) {
            if (itemJsons.has("textures")) {
                JsonObject oldObject = itemJsons.getAsJsonObject("textures");
                for (Map.Entry<String, JsonElement> entry : oldObject.entrySet()) {
                    if (!newTextures.has(entry.getKey())) {
                        newTextures.add(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return newTextures;
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
            if (providers != null)
                for (JsonElement providerElement : providers) {
                    if (!providerElement.isJsonObject())
                        continue;
                    if (newProviders.contains(providerElement))
                        continue;
                    if (!providerElement.getAsJsonObject().has("predicate"))
                        continue;
                    if (!providerElement.getAsJsonObject().has("model"))
                        continue;

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
            if (!element.isJsonObject())
                continue;
            if (!element.getAsJsonObject().has("predicate"))
                continue;
            if (!element.getAsJsonObject().has("model"))
                continue;
            JsonObject predicate = element.getAsJsonObject().get("predicate").getAsJsonObject();
            String modelPath = element.getAsJsonObject().get("model").getAsString();
            overrides.put(predicate, modelPath);
        }
        return overrides;
    }

    // Experimental way of combining 2 fonts instead of making glyphconfigs later
    public static void mergeFontFiles(List<VirtualFile> output) {
        Map<String, List<VirtualFile>> fontsToMerge = new HashMap<>();

        // Generate a map of all duplicate fonts
        for (VirtualFile virtual : output.stream().filter(v -> v.getPath().matches("assets/.*/font/.*.json"))
                .toList()) {
            if (fontsToMerge.containsKey(virtual.getPath())) {
                List<VirtualFile> newList = new ArrayList<>(fontsToMerge.get(virtual.getPath()).stream().toList());
                newList.add(virtual);
                fontsToMerge.put(virtual.getPath(), newList);
            } else {
                fontsToMerge.put(virtual.getPath(), List.of(virtual));
            }
        }

        Map<String, List<VirtualFile>> finalFontsToMerge = fontsToMerge.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
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
                VirtualFile newFont = new VirtualFile(Utils.getParentDirs(first.getPath()),
                        Utils.removeParentDirs(first.getPath()), newInput);
                newFont.setPath(newFont.getPath().replace("//", "/"));
                newFont.setInputStream(newInput);

                // Remove all the old fonts from output
                output.removeAll(duplicates);
                output.add(newFont);
                Logs.logSuccess(
                        "Merged " + duplicates.size() + " duplicate font files into a final " + newFont.getPath());
            }
            Logs.logWarning("The imported font files have not been deleted.");
            Logs.logWarning("If anything seems wrong, there might be conflicting unicodes assigned.");
        } else
            Logs.logSuccess("No duplicate font files found!");
    }

    private static JsonArray getFontProviders(List<VirtualFile> duplicates) {
        JsonArray newProviders = new JsonArray();
        for (VirtualFile font : duplicates) {
            JsonElement fontelement = font.toJsonElement();

            if (fontelement == null || !fontelement.isJsonObject())
                continue;

            JsonArray providers = fontelement.getAsJsonObject().getAsJsonArray("providers");
            List<String> newProviderChars = getNewProviderCharSet(newProviders);
            if (providers != null)
                for (JsonElement providerElement : providers) {
                    if (!providerElement.isJsonObject())
                        continue;
                    JsonObject provider = providerElement.getAsJsonObject();
                    if (newProviders.contains(providerElement))
                        continue;
                    if (provider.has("chars")) {
                        String chars = provider.getAsJsonArray("chars").toString();
                        if (!newProviderChars.contains(chars))
                            newProviders.add(provider);
                        else
                            Logs.logWarning("Tried adding " + chars + " but it was already defined in this font");
                    } else
                        newProviders.add(provider);
                }
        }
        return newProviders;
    }

    private static List<String> getNewProviderCharSet(JsonArray newProvider) {
        List<String> charList = new ArrayList<>();
        for (JsonElement element : newProvider) {
            if (!element.isJsonObject())
                continue;
            if (!element.getAsJsonObject().has("chars"))
                continue;
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
            if (packFolder.resolve(name).toFile().exists())
                duplicateFile = packFolder.resolve(name).toFile();
            else
                duplicateFile = packFolder.resolve(name.replace("assets/minecraft/", "")).toFile();
            List<String> lines = null;
            try {
                if (duplicateFile.getName().endsWith(".json"))
                    lines = FileUtils.readLines(duplicateFile, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                if (Settings.DEBUG.toBool())
                    ex.printStackTrace();
            }
            if (lines != null && lines.get(0).equals(DUPLICATE_LINE_STRING))
                return;

            Logs.logWarning("Duplicate file detected: <blue>" + name + "</blue> - Attempting to migrate it");
            if (!Settings.MERGE_DUPLICATES.toBool()) {
                Logs.logError("Not attempting to migrate duplicate file as <#22b14c>"
                        + Settings.MERGE_DUPLICATES.getPath() + "</#22b14c> is disabled in settings.yml", true);
            } else if (attemptToMigrateDuplicate(name)) {
                Logs.logSuccess("Duplicate file fixed:<blue> " + name);
                try {
                    if (lines == null)
                        lines = FileUtils.readLines(duplicateFile, StandardCharsets.UTF_8);
                    lines.add(0, DUPLICATE_LINE_STRING);
                    FileUtils.writeLines(duplicateFile, lines);
                } catch (Exception ignored) {
                    Logs.logError("Failed to delete the imported <blue>" + Utils.removeParentDirs(name)
                            + "</blue> after migrating it");
                }
                Logs.logSuccess("It is advised to restart your server to ensure that any new conflicts are detected.",
                        true);
            }
        }
    }

    private static boolean attemptToMigrateDuplicate(String name) {
        if (name.matches("assets/minecraft/models/item/.*.json")) {
            Logs.logWarning("Found a duplicate <blue>" + Utils.removeParentDirs(name)
                    + "</blue>, attempting to migrate it into Oraxen item configs");
            return migrateItemJson(name);
        } else if (name.matches("assets/minecraft/sounds.json")) {
            Logs.logWarning("Found a sounds.json duplicate, trying to migrate it into Oraxens sound.yml config");
            return migrateSoundJson(name);
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype_text")
                && Settings.HIDE_SCOREBOARD_NUMBERS.toBool()) {
            Logs.logWarning("You are importing another copy of a shader file used to hide scoreboard numbers");
            Logs.logWarning("Either disable <#22b14c>" + Settings.HIDE_SCOREBOARD_NUMBERS.getPath()
                    + "</#22b14c> in settings.yml or delete this file");
            return false;
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype_armor_cutout_no_cull")
                && Settings.CUSTOM_ARMOR_SHADER_GENERATE_FILES.toBool()) {
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
            Logs.logWarning("If you want to handle these files manually, disable <#22b14c>"
                    + Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.getPath() + "</#22b14c> in settings.yml");
            Logs.logWarning(
                    "Please refer to https://docs.oraxen.com/configuration/custom-armors for more information. Deleting...");
            return true;
        } else if (name.startsWith("assets/minecraft/textures")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a texture file");
            Logs.logWarning("Cannot migrate texture files, rename this or the duplicate entry");
            return false;
        } else if (name.startsWith("assets/minecraft/lang")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a language file");
            Logs.logWarning("Please combine this with the duplicate file found in Oraxen/pack/lang folder");
            return false;
        } else if (name.matches("assets/minecraft/optifine/%s/armors/.*/.*.properties"
                .formatted(VersionUtil.atOrAbove("1.21") ? "cit_single" : "cit"))) {
            Logs.logWarning("You are trying to import an Optifine CustomArmor file.");
            Logs.logWarning("Oraxen already generates all these needed files for you. Deleting...");
            return true;
        } else {
            Logs.logWarning(
                    "Failed to migrate duplicate file-entry, file is not a file that Oraxen can migrate right now");
            Logs.logWarning(
                    "Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            return false;
        }
    }

    private static boolean migrateItemJson(String name) {
        String materialName = Utils.removeExtensionOnly(Utils.removeParentDirs(name)).toUpperCase();
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            Logs.logWarning(
                    "Failed to migrate duplicate file-entry, could not find a matching material for " + materialName);
            return false;
        }

        if (!name.endsWith(".json")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is not a .json file");
            return false;
        }

        YamlConfiguration migratedYaml = loadMigrateItemYaml(material);
        if (migratedYaml == null) {
            Logs.logWarning("Failed to migrate duplicate file-entry, failed to load "
                    + getDuplicateItemFile(material).getPath());
            return false;
        }

        List<JsonObject> overrides = readOverridesFromFile(name);
        if (overrides == null) {
            return false;
        }

        if (!overrides.isEmpty()) {
            processOverrides(overrides, migratedYaml, materialName);
        }

        return saveMigratedYaml(migratedYaml, material);
    }

    private static List<JsonObject> readOverridesFromFile(String name) {
        Path path = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "pack", name);
        if (!path.toFile().exists()) {
            path = Path.of(path.toString().replace("assets\\minecraft\\", ""));
        }

        String fileContent;
        try {
            fileContent = Files.readString(path);
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not read file");
            Logs.debug(e);
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();
            return new ArrayList<>(json.getAsJsonArray("overrides").asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .distinct()
                    .toList());
        } catch (JsonParseException | NullPointerException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not parse json");
            Logs.debug(e);
            return null;
        }
    }

    private static void processOverrides(List<JsonObject> overrides, YamlConfiguration migratedYaml,
            String materialName) {
        Map<Integer, List<String>> pullingModels = new HashMap<>();
        Map<Integer, String> chargedModels = new HashMap<>();
        Map<Integer, String> blockingModels = new HashMap<>();
        Map<Integer, String> castModels = new HashMap<>();
        Map<Integer, List<String>> damagedModels = new HashMap<>();
        List<JsonElement> overridesToRemove = new ArrayList<>();

        handleBowPulling(overrides, overridesToRemove, pullingModels);
        handleCrossbowPulling(overrides, overridesToRemove, chargedModels);
        handleShieldBlocking(overrides, overridesToRemove, blockingModels);
        handleFishingRodCast(overrides, overridesToRemove, castModels);
        handleDamaged(overrides, overridesToRemove, damagedModels);

        overrides.removeIf(overridesToRemove::contains);

        for (JsonElement element : overrides) {
            JsonObject predicate = element.getAsJsonObject().get("predicate").getAsJsonObject();
            String modelPath = element.getAsJsonObject().get("model").getAsString().replace("\\", "/");
            String id = "migrated_" + modelPath.replaceAll("[^a-zA-Z0-9]+", "_");
            int cmd = predicate.has("custom_model_data") ? predicate.get("custom_model_data").getAsInt() : 0;

            setMigratedItemProperties(migratedYaml, id, materialName, modelPath, cmd,
                    pullingModels, damagedModels, chargedModels, blockingModels, castModels);
        }
    }

    private static void setMigratedItemProperties(YamlConfiguration yaml, String id, String materialName,
            String modelPath, int cmd, Map<Integer, List<String>> pullingModels,
            Map<Integer, List<String>> damagedModels, Map<Integer, String> chargedModels,
            Map<Integer, String> blockingModels, Map<Integer, String> castModels) {
        yaml.set(id + ".material", materialName);
        yaml.set(id + ".excludeFromInventory", true);
        yaml.set(id + ".excludeFromCommands", true);
        yaml.set(id + ".Pack.generate_model", false);
        yaml.set(id + ".Pack.model", modelPath);

        if (pullingModels.containsKey(cmd))
            yaml.set(id + ".Pack.pulling_models", pullingModels.get(cmd));
        if (damagedModels.containsKey(cmd))
            yaml.set(id + ".Pack.damaged_models", damagedModels.get(cmd));
        if (chargedModels.containsKey(cmd))
            yaml.set(id + ".Pack.charged_model", chargedModels.get(cmd));
        if (blockingModels.containsKey(cmd))
            yaml.set(id + ".Pack.blocking_model", blockingModels.get(cmd));
        if (castModels.containsKey(cmd))
            yaml.set(id + ".Pack.cast_model", castModels.get(cmd));
        if (Settings.RETAIN_CUSTOM_MODEL_DATA.toBool())
            yaml.set(id + ".Pack.custom_model_data", cmd);
    }

    private static boolean saveMigratedYaml(YamlConfiguration migratedYaml, Material material) {
        try {
            migratedYaml.save(getDuplicateItemFile(material));
            return true;
        } catch (IOException e) {
            Logs.logWarning("Failed to migrate duplicate file-entry, could not save migrated_duplicates.yml");
            Logs.debug(e);
            return false;
        }
    }

    private static void handleBowPulling(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, List<String>> pullingModels) {
        handleExtraListPredicates(overrides, overridesToRemove, pullingModels, "pulling");
    }

    private static void handleDamaged(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, List<String>> damagedModels) {
        handleExtraListPredicates(overrides, overridesToRemove, damagedModels, "damaged");
    }

    private static void handleExtraListPredicates(@NotNull List<JsonObject> overrides,
            List<JsonElement> overridesToRemove, Map<Integer, List<String>> predicateModels, String predicate) {
        for (JsonObject object : overrides) {
            if (object.get("predicate") == null || !object.get("predicate").isJsonObject())
                continue;
            JsonObject predicateObject = object.get("predicate").getAsJsonObject();
            if (predicateObject == null || !predicateObject.has(predicate))
                continue;
            int cmd = predicateObject.has("custom_model_data") ? predicateObject.get("custom_model_data").getAsInt()
                    : 0;
            String modelPath = object.get("model").getAsString().replace("\\", "/");
            predicateModels.computeIfAbsent(cmd, k -> new ArrayList<>()).add(modelPath);
            overridesToRemove.add(object);
        }
    }

    private static void handleCrossbowPulling(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, String> chargedModels) {
        handleExtraPredicates(overrides, overridesToRemove, chargedModels, "charged");
    }

    private static void handleShieldBlocking(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, String> blockingModels) {
        handleExtraPredicates(overrides, overridesToRemove, blockingModels, "blocking");
    }

    private static void handleFishingRodCast(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, String> castModels) {
        handleExtraPredicates(overrides, overridesToRemove, castModels, "cast");
    }

    private static void handleExtraPredicates(@NotNull List<JsonObject> overrides, List<JsonElement> overridesToRemove,
            Map<Integer, String> predicateModels, String predicate) {
        for (JsonObject object : overrides) {
            if (object.get("predicate") == null || !object.get("predicate").isJsonObject())
                continue;
            JsonObject predicateObject = object.get("predicate").getAsJsonObject();
            if (predicateObject == null || !predicateObject.has(predicate))
                continue;
            int cmd = predicateObject.has("custom_model_data") ? predicateObject.get("custom_model_data").getAsInt()
                    : 0;
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
            YamlConfiguration soundYaml = OraxenYaml
                    .loadConfiguration(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
            for (String id : sounds.keySet()) {
                if (soundYaml.contains("sounds." + id)) {
                    Logs.logWarning("Sound " + id + " is already defined in sound.yml, skipping");
                    continue;
                }
                JsonObject sound = sounds.get(id).getAsJsonObject();
                boolean replace = sound.get("replace") != null && sound.get("replace").getAsBoolean();
                String category = sound.get("category") != null && sound.get("category").getAsString() != null
                        ? sound.get("category").getAsString()
                        : null;
                String subtitle = sound.get("subtitle").getAsString() != null ? sound.get("subtitle").getAsString()
                        : null;
                JsonArray soundArray = sound.getAsJsonArray("sounds");
                List<String> soundList = new ArrayList<>();
                if (soundArray != null)
                    for (JsonElement s : soundArray)
                        soundList.add(s.getAsString());

                soundYaml.set("sounds." + id + ".replace", replace);
                soundYaml.set("sounds." + id + ".category", category != null ? category : "master");
                if (subtitle != null)
                    soundYaml.set("sounds." + id + ".subtitle", subtitle);
                soundYaml.set("sounds." + id + ".sounds", soundList);

                try {
                    soundYaml.save(new File(OraxenPlugin.get().getDataFolder().getAbsolutePath(), "/sound.yml"));
                    Logs.logSuccess("Successfully migrated sound <blue>" + id + "</blue> into sound.yml");
                } catch (IOException e) {
                    Logs.logWarning("Failed to migrate duplicate file-entry, could not save <blue>" + id
                            + "</blue> to sound.yml");
                }
            }
        } catch (Exception e) {
            Logs.logError("Failed to migrate sounds.json");
            Logs.debug(e);
            return false;
        }

        return true;
    }

    private static YamlConfiguration loadMigrateItemYaml(Material material) {

        File file = DuplicationHandler.getDuplicateItemFile(material);
        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Logs.debug(e);
                return null;
            }
        }
        try {
            return OraxenYaml.loadConfiguration(file);
        } catch (Exception e) {
            Logs.debug(e);
            return null;
        }
    }

    @Deprecated(forRemoval = true)
    public static void convertOldMigrateItemConfig() {
        File oldMigrateConfigFile = OraxenPlugin.get().getDataFolder().toPath().resolve("items/migrated_duplicates.yml")
                .toFile();
        if (!oldMigrateConfigFile.exists())
            return;
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

            File newMigrateConfigFile = OraxenPlugin.get().getDataFolder().toPath()
                    .resolve("items/migrated_duplicates/duplicate_" + material + ".yml").toFile();
            if (!newMigrateConfigFile.getParentFile().exists())
                newMigrateConfigFile.getParentFile().mkdirs();
            if (!newMigrateConfigFile.exists()) {
                try {
                    newMigrateConfigFile.createNewFile();
                } catch (IOException e) {
                    Logs.debug(e);
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
                Logs.debug(e);
            }
        }

        try {
            Files.delete(oldMigrateConfigFile.toPath());
            Logs.logSuccess("Successfully converted migrated_duplicates.yml into new format");
        } catch (IOException e) {
            Logs.debug(e);
        }
    }
}
