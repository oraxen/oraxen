package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.AppearanceMode;
import io.th0rgal.oraxen.configs.MigrationBackups;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.sounds.SoundConfigMigration;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FilenameUtils;
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
            if (Material.getMaterial(FilenameUtils.getBaseName(virtual.getPath()).toUpperCase()) == null)
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
                if (duplicates.size() < 2) continue;
                List<JsonObject> duplicateJsonObjects = parseJsonObjects(duplicates);
                if (duplicateJsonObjects.size() < 2) continue;
                JsonObject mainItem = duplicateJsonObjects.getLast().deepCopy();
                for (int i = duplicateJsonObjects.size() - 2; i >= 0; i--)
                    mainItem = ItemModelMerger.mergeLegacy(duplicateJsonObjects.get(i), mainItem);
                // Generate the template new item file
                VirtualFile first = duplicates.stream().findFirst().get();
                InputStream newInput = new ByteArrayInputStream(mainItem.toString().getBytes(StandardCharsets.UTF_8));
                VirtualFile newItem = new VirtualFile(FilenameUtils.getFullPath(first.getPath()),
                        FilenameUtils.getName(first.getPath()), newInput);
                newItem.setPath(newItem.getPath().replace("//", "/"));
                newItem.setInputStream(newInput);

                // Replace the original duplicate files.
                output.removeAll(duplicates);
                output.add(newItem);
            }
    }

    /**
     * Merges vanilla item definition files (assets/minecraft/items/*.json) for
     * 1.21.4+.
     * This handles external packs that may include their own range_dispatch entries
     * for custom_model_data, merging them with Oraxen's generated definitions.
     * <p>
     * Note: 1.21.4+ supports multiple CustomModelData "channels":
     * - {@code strings} are used by {@code minecraft:select}
     * - {@code floats} are used by {@code minecraft:range_dispatch}
     * Oraxen primarily generates {@code minecraft:select} (strings) on 1.21.4+.
     */
    public static void mergeVanillaItemDefinitions(List<VirtualFile> output, boolean multiVersionMode) {
        mergeVanillaItemDefinitions(output, multiVersionMode, true);
    }

    public static void mergeVanillaItemDefinitions(List<VirtualFile> output, boolean multiVersionMode, boolean convertLegacy) {
        boolean serverIs1214Plus = VersionUtil.atOrAbove("1.21.4");

        // In multi-version mode, convert legacy predicates to modern item definitions
        // even on 1.21.3 servers so that 1.21.4+ pack versions have assets/minecraft/items/*.json.
        if (!serverIs1214Plus && !multiVersionMode)
            return;

        // Only merge item definitions when we're generating CMD-based definitions,
        // or when in multi-version mode where 1.21.4+ clients need them as a fallback.
        if (!AppearanceMode.shouldGenerateVanillaItemDefinitions() && !multiVersionMode)
            return;

        // First, convert legacy predicate overrides from external packs to modern item
        // definitions
        if (convertLegacy) convertLegacyPredicatesToItemDefinitions(output);

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
                    FilenameUtils.getFullPath(first.getPath()),
                    FilenameUtils.getName(first.getPath()),
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
     * Adds converted rules alongside existing modern definitions.
     */
    private static void convertLegacyPredicatesToItemDefinitions(List<VirtualFile> output) {
        Set<String> modernPaths = output.stream().map(VirtualFile::getPath)
                .filter(path -> path.startsWith("assets/minecraft/items/")).collect(Collectors.toSet());
        // Find legacy model files that have custom_model_data predicates
        List<VirtualFile> legacyModels = output.stream()
                .filter(v -> v.getPath().startsWith("assets/minecraft/models/item/") && v.getPath().endsWith(".json"))
                .toList();

        List<VirtualFile> newDefinitions = new ArrayList<>();

        for (VirtualFile legacyModel : legacyModels) {
            String itemName = FilenameUtils.getBaseName(legacyModel.getPath()).toLowerCase(Locale.ROOT);
            if (modernPaths.contains("assets/minecraft/items/" + itemName + ".json")) continue;


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

            for (JsonElement override : overrides) {
                if (!override.isJsonObject() || !override.getAsJsonObject().has("predicate")) continue;
                JsonObject predicate = override.getAsJsonObject().getAsJsonObject("predicate");
                if (predicate.has("custom_model_data") && predicate.keySet().stream().anyMatch(key ->
                        !Set.of("custom_model_data", "damage", "pull", "pulling", "blocking", "cast").contains(key)))
                    Logs.logWarning("Cannot convert legacy item rule for " + itemName + " with predicates " + predicate.keySet());
            }
            JsonObject itemDef = ItemModelMerger.convertLegacy(legacyJson, itemName);
            if (itemDef == null)
                continue;

            // Add to output
            VirtualFile newDef = new VirtualFile(
                    "assets/minecraft/items",
                    itemName + ".json",
                    new ByteArrayInputStream(itemDef.toString().getBytes(StandardCharsets.UTF_8)));
            newDefinitions.add(newDef);


            Logs.logSuccess("Converted legacy predicates for " + itemName + " to modern item definition");
        }

        output.addAll(newDefinitions);
    }

    private static JsonObject mergeItemDefinitionEntries(List<VirtualFile> duplicates) {
        List<JsonObject> jsonObjects = parseJsonObjects(duplicates);
        if (jsonObjects.isEmpty())
            return null;

        JsonObject result = jsonObjects.getLast().deepCopy();
        for (int i = jsonObjects.size() - 2; i >= 0; i--)
            result = ItemModelMerger.mergeDefinition(jsonObjects.get(i), result);
        return result;
    }
    private static List<JsonObject> parseJsonObjects(List<VirtualFile> files) {
        return files.stream()
                .map(VirtualFile::toJsonElement)
                .filter(e -> e != null && e.isJsonObject())
                .map(JsonElement::getAsJsonObject)
                .toList();
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
                VirtualFile newFont = new VirtualFile(FilenameUtils.getFullPath(first.getPath()),
                        FilenameUtils.getName(first.getPath()), newInput);
                newFont.setPath(newFont.getPath().replace("//", "/"));
                newFont.setInputStream(newInput);

                // Replace the original duplicate files.
                output.removeAll(duplicates);
                output.add(newFont);
                Logs.logSuccess(
                        "Merged " + duplicates.size() + " duplicate font files into a final " + newFont.getPath());
            }
            Logs.logWarning("The imported font files have not been deleted.");
            Logs.logWarning("If anything seems wrong, there might be conflicting unicodes assigned.");
        } else if (Settings.DEBUG.toBool())
            Logs.logInfo("No duplicate font files found!");
    }

    private static JsonArray getFontProviders(List<VirtualFile> duplicates) {
        JsonArray newProviders = new JsonArray();
        for (VirtualFile font : duplicates) {
            JsonElement fontelement = font.toJsonElement();

            if (fontelement == null || !fontelement.isJsonObject())
                continue;

            JsonArray providers = fontelement.getAsJsonObject().getAsJsonArray("providers");

            if (providers != null)
                for (JsonElement providerElement : providers) {
                    if (!providerElement.isJsonObject())
                        continue;
                    JsonObject provider = providerElement.getAsJsonObject();
                    if (newProviders.contains(providerElement))
                        continue;
                    newProviders.add(provider.deepCopy());
                }
        }
        return newProviders;
    }

    /**
     * Check if the file already exists in the zip file
     */
    public static void checkForDuplicate(ZipOutputStream out, ZipEntry entry) {
        try {
            tryOpenEntry(out, entry);
        } catch (IOException exception) {
            Logs.logError("Failed to open zip entry " + entry.getName());
            Logs.debug(exception);
        }
    }

    public static boolean tryOpenEntry(ZipOutputStream out, ZipEntry entry) throws IOException {
        try {
            out.putNextEntry(entry);
            return true;
        } catch (java.util.zip.ZipException exception) {
            if (exception.getMessage() != null && exception.getMessage().startsWith("duplicate entry:")) {
                Logs.logWarning("Skipping duplicate zip entry " + entry.getName());
                migrateRejectedLocalDuplicate(entry.getName());
                return false;
            }
            throw exception;
        }
    }

    private static void migrateRejectedLocalDuplicate(String name) {
        if (OraxenPlugin.get() == null || !Settings.MERGE_DUPLICATES.toBool() || !name.endsWith(".json")) return;
        Path pack = OraxenPlugin.get().getDataFolder().toPath().resolve("pack").toAbsolutePath().normalize();
        Path source = pack.resolve(name).normalize();
        if (!source.startsWith(pack)) return;
        if (!Files.isRegularFile(source)) source = pack.resolve(name.replace("assets/minecraft/", "")).normalize();
        if (!source.startsWith(pack) || !Files.isRegularFile(source)) return;
        try {
            migrateLocalDuplicate(source, () -> attemptToMigrateDuplicate(name));
        } catch (IOException | RuntimeException exception) {
            Logs.logWarning("Failed to migrate duplicate local resource " + name);
            Logs.debug(exception);
        }
    }

    static boolean migrateLocalDuplicate(Path source, java.util.function.BooleanSupplier migration) throws IOException {
        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        String marker = "// This file was recognized as a duplicate and was migrated into its relevant config(s)";
        if (!lines.isEmpty() && lines.getFirst().equals(marker)) return false;
        JsonElement json = JsonParser.parseString(String.join("\n", lines));
        if (!json.isJsonObject() || !migration.getAsBoolean()) return false;
        lines.addFirst(marker);
        Files.write(source, lines, StandardCharsets.UTF_8);
        return true;
    }
    private static boolean attemptToMigrateDuplicate(String name) {
        if (name.matches("assets/minecraft/models/item/.*.json")) {
            Logs.logWarning("Found a duplicate <blue>" + FilenameUtils.getName(name)
                    + "</blue>, attempting to migrate it into Oraxen item configs");
            return migrateItemJson(name);
        } else if (name.matches("assets/[^/]+/sounds\\.json")) {
            Logs.logWarning("Found a sounds.json duplicate, trying to migrate it into Oraxen's sounds.yml config");
            return migrateSoundJson(name);
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype_text")
                && Settings.HIDE_SCOREBOARD_NUMBERS.toBool()) {
            Logs.logWarning("You are importing another copy of a shader file used to hide scoreboard numbers");
            Logs.logWarning("Either disable <#22b14c>" + Settings.HIDE_SCOREBOARD_NUMBERS.getPath()
                    + "</#22b14c> in settings.yml or delete this file");
            return false;
        } else if (name.startsWith("assets/minecraft/shaders/core/rendertype")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a shader file");
            Logs.logWarning("Merging this is too advanced and should be migrated manually or deleted.");
            return false;
        } else if (name.startsWith("assets/minecraft/textures")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a texture file");
            Logs.logWarning("Cannot migrate texture files, rename this or the duplicate entry");
            return false;
        } else if (name.startsWith("assets/minecraft/lang")) {
            Logs.logWarning("Failed to migrate duplicate file-entry, file is a language file");
            Logs.logWarning("Please combine this with the duplicate file found in Oraxen/pack/lang folder");
            return false;
        } else {
            Logs.logWarning(
                    "Failed to migrate duplicate file-entry, file is not a file that Oraxen can migrate right now");
            Logs.logWarning(
                    "Please refer to https://docs.oraxen.com/ on how to solve this, or ask in the support Discord");
            return false;
        }
    }

    private static boolean migrateItemJson(String name) {
        String materialName = FilenameUtils.getBaseName(name).toUpperCase();
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

        if (overrides.isEmpty()) return false;
        processOverrides(overrides, migratedYaml, materialName);
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
        yaml.set(id + ".pack.generate_model", false);
        yaml.set(id + ".pack.model", modelPath);

        if (pullingModels.containsKey(cmd))
            yaml.set(id + ".pack.pulling_models", pullingModels.get(cmd));
        if (damagedModels.containsKey(cmd))
            yaml.set(id + ".pack.damaged_models", damagedModels.get(cmd));
        if (chargedModels.containsKey(cmd))
            yaml.set(id + ".pack.charged_model", chargedModels.get(cmd));
        if (blockingModels.containsKey(cmd))
            yaml.set(id + ".pack.blocking_model", blockingModels.get(cmd));
        if (castModels.containsKey(cmd))
            yaml.set(id + ".pack.cast_model", castModels.get(cmd));
        if (Settings.RETAIN_CUSTOM_MODEL_DATA.toBool())
            yaml.set(id + ".pack.custom_model_data", cmd);
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
            File soundsFile = getSoundsFile();
            YamlConfiguration soundYaml = OraxenYaml.loadConfiguration(soundsFile);
            boolean migratedConfig = SoundConfigMigration.migrateToNewFormat(soundYaml);

            List<Map<String, Object>> configuredSounds = getConfiguredSounds(soundYaml);
            Set<String> configuredIds = getConfiguredSoundIds(configuredSounds);
            String namespace = namespaceFromSoundsJsonPath(name);
            boolean changed = false;

            for (String soundKey : sounds.keySet()) {
                String id = namespace.equals("minecraft") ? soundKey : namespace + ":" + soundKey;
                if (containsSoundId(configuredIds, id)) {
                    Logs.logWarning("Sound " + id + " is already defined in sounds.yml, skipping");
                    continue;
                }

                JsonObject sound = sounds.get(soundKey).getAsJsonObject();
                Map<String, Object> migratedSound = soundJsonToConfigMap(id, sound, namespace);
                configuredSounds.add(migratedSound);
                addSoundId(configuredIds, id);
                changed = true;
                Logs.logSuccess("Successfully migrated sound <blue>" + id + "</blue> into sounds.yml");
            }

            if (changed)
                soundYaml.set("sounds", configuredSounds);
            if (changed || migratedConfig)
                soundYaml.save(soundsFile);
            return changed || migratedConfig;
        } catch (Exception e) {
            Logs.logError("Failed to migrate sounds.json");
            Logs.debug(e);
            return false;
        }

    }

    private static File getSoundsFile() throws IOException {
        File dataFolder = OraxenPlugin.get().getDataFolder();
        File soundsFile = new File(dataFolder, "sounds.yml");
        File legacySoundFile = new File(dataFolder, "sound.yml");
        if (legacySoundFile.exists()) {
            YamlConfiguration legacySoundYaml = OraxenYaml.loadConfiguration(legacySoundFile);
            SoundConfigMigration.migrateToNewFormat(legacySoundYaml);

            boolean soundsFileAlreadyExists = soundsFile.exists();
            YamlConfiguration soundsYaml = soundsFileAlreadyExists
                    ? OraxenYaml.loadConfiguration(soundsFile)
                    : legacySoundYaml;
            if (!soundsFileAlreadyExists || SoundConfigMigration.mergeSounds(soundsYaml, legacySoundYaml))
                soundsYaml.save(soundsFile);
            MigrationBackups.moveToMigrated(dataFolder, legacySoundFile);
            return soundsFile;
        }

        if (soundsFile.exists())
            return soundsFile;

        soundsFile.createNewFile();
        return soundsFile;
    }

    private static Map<String, Object> soundJsonToConfigMap(String id, JsonObject sound, String namespace) {
        Map<String, Object> soundMap = new LinkedHashMap<>();
        soundMap.put("id", id);

        if (sound.has("replace") && sound.get("replace").getAsBoolean())
            soundMap.put("replace", true);
        if (sound.has("category") && !sound.get("category").isJsonNull())
            soundMap.put("category", sound.get("category").getAsString());
        if (sound.has("subtitle") && !sound.get("subtitle").isJsonNull())
            soundMap.put("subtitle", sound.get("subtitle").getAsString());
        if (sound.has("stream") && sound.get("stream").getAsBoolean())
            soundMap.put("stream", true);
        if (sound.has("volume") && !sound.get("volume").isJsonNull())
            soundMap.put("volume", sound.get("volume").getAsFloat());
        if (sound.has("pitch") && !sound.get("pitch").isJsonNull())
            soundMap.put("pitch", sound.get("pitch").getAsFloat());
        if (sound.has("weight") && !sound.get("weight").isJsonNull())
            soundMap.put("weight", sound.get("weight").getAsInt());

        List<String> soundList = new ArrayList<>();
        JsonArray soundArray = sound.getAsJsonArray("sounds");
        if (soundArray != null) {
            for (JsonElement soundElement : soundArray) {
                if (soundElement.isJsonPrimitive()) {
                    soundList.add(namespaceSoundReference(soundElement.getAsString(), namespace));
                    continue;
                }

                if (!soundElement.isJsonObject())
                    continue;

                JsonObject soundObject = soundElement.getAsJsonObject();
                if (soundObject.has("name") && !soundObject.get("name").isJsonNull())
                    soundList.add(namespaceSoundReference(soundObject.get("name").getAsString(), namespace));
                copyFirstSoundProperty(soundMap, soundObject, "stream");
                copyFirstSoundProperty(soundMap, soundObject, "volume");
                copyFirstSoundProperty(soundMap, soundObject, "pitch");
                copyFirstSoundProperty(soundMap, soundObject, "weight");
            }
        }
        soundMap.put("sounds", soundList);

        return soundMap;
    }

    private static String namespaceSoundReference(String sound, String namespace) {
        if (namespace.equals("minecraft") || sound.contains(":"))
            return sound;
        return namespace + ":" + sound;
    }

    private static void copyFirstSoundProperty(Map<String, Object> soundMap, JsonObject soundObject, String property) {
        if (soundMap.containsKey(property) || !soundObject.has(property) || soundObject.get(property).isJsonNull())
            return;

        JsonElement value = soundObject.get(property);
        switch (property) {
            case "stream" -> soundMap.put(property, value.getAsBoolean());
            case "weight" -> soundMap.put(property, value.getAsInt());
            default -> soundMap.put(property, value.getAsFloat());
        }
    }

    private static List<Map<String, Object>> getConfiguredSounds(YamlConfiguration soundYaml) {
        List<Map<String, Object>> configuredSounds = new ArrayList<>();
        for (Map<?, ?> sound : soundYaml.getMapList("sounds")) {
            Map<String, Object> normalizedSound = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : sound.entrySet())
                if (entry.getKey() != null)
                    normalizedSound.put(String.valueOf(entry.getKey()), entry.getValue());
            configuredSounds.add(normalizedSound);
        }
        return configuredSounds;
    }

    private static Set<String> getConfiguredSoundIds(List<Map<String, Object>> configuredSounds) {
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> sound : configuredSounds) {
            Object id = sound.get("id");
            if (id != null)
                addSoundId(ids, String.valueOf(id));
        }
        return ids;
    }

    private static boolean containsSoundId(Set<String> ids, String id) {
        if (ids.contains(id))
            return true;
        if (id.startsWith("minecraft:"))
            return ids.contains(id.substring("minecraft:".length()));
        return ids.contains("minecraft:" + id);
    }

    private static void addSoundId(Set<String> ids, String id) {
        ids.add(id);
        if (id.startsWith("minecraft:"))
            ids.add(id.substring("minecraft:".length()));
        else if (!id.contains(":"))
            ids.add("minecraft:" + id);
    }

    private static String namespaceFromSoundsJsonPath(String path) {
        String normalized = path.replace('\\', '/');
        int namespaceStart = "assets/".length();
        int namespaceEnd = normalized.indexOf('/', namespaceStart);
        return namespaceEnd == -1 ? "minecraft" : normalized.substring(namespaceStart, namespaceEnd);
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
