package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.AppearanceMode;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.AnimatedGlyph;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.ShiftProvider;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.sound.CustomSound;
import io.th0rgal.oraxen.sound.JukeboxDatapack;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.customarmor.ComponentArmorModels;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorType;
import io.th0rgal.oraxen.utils.customarmor.ShaderArmorTextures;
import io.th0rgal.oraxen.utils.customarmor.TrimArmorDatapack;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePack {

    private final Map<String, Collection<Consumer<File>>> packModifiers;
    private static Map<String, VirtualFile> outputFiles;
    private ShaderArmorTextures shaderArmorTextures;
    private TrimArmorDatapack trimArmorDatapack;
    private ComponentArmorModels componentArmorModels;
    private static final File packFolder = new File(OraxenPlugin.get().getDataFolder(), "pack");
    private final File pack = new File(packFolder, packFolder.getName() + ".zip");

    /**
     * Tracks whether animation shaders were generated (for combining with
     * scoreboard shaders)
     */
    private boolean animationShadersGenerated = false;

    public ResourcePack() {
        // we use maps to avoid duplicate
        packModifiers = new HashMap<>();
        outputFiles = new HashMap<>();
    }

    public void generate() {
        outputFiles.clear();
        animationShadersGenerated = false;

        makeDirsIfNotExists(packFolder, new File(packFolder, "assets"));

        componentArmorModels = CustomArmorType.getSetting() == CustomArmorType.COMPONENT ? new ComponentArmorModels()
                : null;
        trimArmorDatapack = CustomArmorType.getSetting() == CustomArmorType.TRIMS ? new TrimArmorDatapack() : null;
        shaderArmorTextures = CustomArmorType.getSetting() == CustomArmorType.SHADER ? new ShaderArmorTextures() : null;

        if (Settings.GENERATE_DEFAULT_ASSETS.toBool())
            extractDefaultFolders();
        extractRequired();

        if (!Settings.GENERATE.toBool())
            return;

        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool() && PluginUtils.isEnabled("HappyHUD")) {
            Logs.logError("HappyHUD detected with hide_scoreboard_numbers enabled!");
            Logs.logWarning(
                    "Recommend following this guide for compatibility: https://docs.oraxen.com/compatibility/happyhud");
        }

        try {
            Files.deleteIfExists(pack.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        extractInPackIfNotExists(new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(new File(packFolder, "pack.png"));
        updatePackMcmeta();

        // Sorting items to keep only one with models (and generate it if needed)
        final Map<Material, Map<String, ItemBuilder>> texturedItems = extractTexturedItems();

        // Appearance systems can be combined on 1.21.4+.
        // Pre-1.21.4 ALWAYS uses legacy predicates only.
        final boolean is1_21_4Plus = VersionUtil.atOrAbove("1.21.4");

        if (is1_21_4Plus) {
            // Validate and log any configuration warnings
            AppearanceMode.validateAndLogWarnings();

            // ITEM_PROPERTIES: Generate assets/oraxen/items/<item_id>.json
            if (AppearanceMode.isItemPropertiesEnabled()) {
                generateModelDefinitions(filterForItemModel(texturedItems));
            }

            // MODEL_DATA_IDS or MODEL_DATA_FLOAT: Generate assets/minecraft/items/<material>.json
            if (AppearanceMode.shouldGenerateVanillaItemDefinitions()) {
                boolean useSelect = AppearanceMode.shouldUseSelectForVanillaItemDefs();
                boolean includeBothModes = AppearanceMode.shouldUseBothDispatchModes();
                generateVanillaItemDefinitions(filterForPredicates(texturedItems), useSelect, includeBothModes);
            }

            // generate_predicates: Generate legacy predicate overrides (not needed on 1.21.4+)
            if (AppearanceMode.shouldGenerateLegacyPredicates()) {
                generatePredicates(filterForPredicates(texturedItems));
            }
        } else {
            // Pre-1.21.4: Always generate legacy predicate overrides (the only option available)
            generatePredicates(filterForPredicates(texturedItems));
        }

        generateFont();
        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool())
            hideScoreboardNumbers();
        hideScoreboardOrTablistBackgrounds();
        if (Settings.TEXTURE_SLICER.toBool())
            PackSlicer.slicePackFiles();
        if (CustomArmorType.getSetting() == CustomArmorType.SHADER
                && Settings.CUSTOM_ARMOR_SHADER_GENERATE_FILES.toBool())
            ShaderArmorTextures.generateArmorShaderFiles();

        for (final Collection<Consumer<File>> packModifiers : packModifiers.values())
            for (Consumer<File> packModifier : packModifiers)
                packModifier.accept(packFolder);
        List<VirtualFile> output = new ArrayList<>(outputFiles.values());

        // zipping resourcepack
        try {
            // Adds all non-directory root files
            getFilesInFolder(packFolder, output, packFolder.getCanonicalPath(), packFolder.getName() + ".zip");

            // needs to be ordered, forEach cannot be used
            File[] files = packFolder.listFiles();
            if (files != null)
                for (final File folder : files) {
                    if (!folder.isDirectory())
                        continue;
                    if (folder.getName().equals("uploads"))
                        continue;
                    getAllFiles(folder, output,
                            folder.getName().matches("models|textures|lang|font|sounds") ? "assets/minecraft" : "");
                }

            // Merge uploaded resource packs (zips)
            mergeUploadedPacks(output);

            // Convert the global.json within the lang-folder to all languages
            convertGlobalLang(output);

            // Handles generation of datapack & other files for custom armor
            handleCustomArmor(output);

            Collections.sort(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<String> malformedTextures = new HashSet<>();
        if (Settings.VERIFY_PACK_FILES.toBool())
            malformedTextures = verifyPackFormatting(output);

        if (Settings.GENERATE_ATLAS_FILE.toBool())
            AtlasGenerator.generateAtlasFile(output, malformedTextures);

        if (Settings.MERGE_DUPLICATE_FONTS.toBool())
            DuplicationHandler.mergeFontFiles(output);
        if (Settings.MERGE_ITEM_MODELS.toBool())
            DuplicationHandler.mergeBaseItemFiles(output);
        // Merge vanilla item definitions for 1.21.4+ (if predicates enabled)
        DuplicationHandler.mergeVanillaItemDefinitions(output);

        List<String> excludedExtensions = Settings.EXCLUDED_FILE_EXTENSIONS.toStringList();
        excludedExtensions.removeIf(f -> f.equals("png") || f.equals("json"));
        if (!excludedExtensions.isEmpty() && !output.isEmpty()) {
            List<VirtualFile> newOutput = new ArrayList<>();
            for (VirtualFile virtual : output)
                for (String extension : excludedExtensions)
                    if (virtual.getPath().endsWith(extension))
                        newOutput.add(virtual);
            output.removeAll(newOutput);
        }

        generateSound(output);

        SchedulerUtil.runTask(() -> {
            OraxenPackGeneratedEvent event = new OraxenPackGeneratedEvent(output);
            EventUtils.callEvent(event);
            ZipUtils.writeZipFile(pack, event.getOutput());

            UploadManager uploadManager = OraxenPlugin.get().getUploadManager();
            if (uploadManager != null) { // If the uploadManager isnt null, this was triggered by a pack-reload
                uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), true, true);
            } else { // Otherwise this is was triggered on server-startup
                uploadManager = new UploadManager(OraxenPlugin.get());
                OraxenPlugin.get().setUploadManager(uploadManager);
                uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), false, false);
            }
        });
    }

    /**
     * Ensures {@code pack/pack.mcmeta} always has the correct {@code pack_format}
     * for the running server version.
     *
     * <p>
     * We can't rely on {@link #extractInPackIfNotExists(File)} because users often
     * keep their pack folder
     * across updates, and the embedded template may be outdated for newer Minecraft
     * versions.
     * </p>
     */
    private void updatePackMcmeta() {
        Path mcmetaPath = packFolder.toPath().resolve("pack.mcmeta");
        if (!mcmetaPath.toFile().exists())
            return;

        try {
            String content = Files.readString(mcmetaPath, StandardCharsets.UTF_8);
            JsonObject root;
            try {
                root = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception ignored) {
                root = new JsonObject();
            }

            JsonObject pack = root.has("pack") && root.get("pack").isJsonObject()
                    ? root.getAsJsonObject("pack")
                    : new JsonObject();

            // Preserve description if present (some users customize it).
            if (!pack.has("description")) {
                pack.addProperty("description", "§9§lOraxen §8| §7Extend the Game §7www§8.§7oraxen§8.§7com");
            }

            int packFormat = ResourcePackFormatUtil.getCurrentResourcePackFormat();
            pack.addProperty("pack_format", packFormat);
            root.add("pack", pack);

            Files.writeString(mcmetaPath, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
            Logs.logWarning("Failed to update pack.mcmeta pack_format. Keeping existing file.");
        }
    }

    private static Set<String> verifyPackFormatting(List<VirtualFile> output) {
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

                    if (image.getHeight() > 256 || image.getWidth() > 256) {
                        Logs.logWarning("Found invalid texture at <blue>" + texture.getPath());
                        Logs.logError("Resolution of textures cannot exceed 256x256");
                        malformedTextures.add(texture);
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

    private static String modelPathToPackPath(String modelPath) {
        String namespace = modelPath.split(":").length == 1 ? "minecraft" : modelPath.split(":")[0];
        String texturePath = modelPath.split(":").length == 1 ? modelPath : modelPath.split(":")[1];
        texturePath = texturePath.endsWith(".png") ? texturePath : texturePath + ".png";
        return "assets/" + namespace + "/textures/" + texturePath;
    }

    private final boolean extractAssets = !new File(packFolder, "assets").exists();
    private final boolean extractModels = !new File(packFolder, "models").exists();
    private final boolean extractFonts = !new File(packFolder, "font").exists();
    private final boolean extractOptifine = !new File(packFolder, "optifine").exists();
    private final boolean extractLang = !new File(packFolder, "lang").exists();
    private final boolean extractTextures = !new File(packFolder, "textures").exists();
    private final boolean extractSounds = !new File(packFolder, "sounds").exists();

    private void extractDefaultFolders() {
        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                extract(entry, OraxenPlugin.get().getResourceManager(), isSuitable(entry.getName()));
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
            zip.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isSuitable(String entryName) {
        String name = StringUtils.substringAfter(entryName, "pack/").split("/")[0];
        if (name.equals("textures") && extractTextures)
            return true;
        if (name.equals("models") && extractModels)
            return true;
        if (name.equals("font") && extractFonts)
            return true;
        if (name.equals("optifine") && extractOptifine)
            return true;
        if (name.equals("lang") && extractLang)
            return true;
        if (name.equals("sounds") && extractSounds)
            return true;
        return name.equals("assets") && extractAssets;
    }

    private void extractRequired() {
        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (entry.getName().startsWith("pack/textures/models/armor/leather_layer_")
                        || entry.getName().startsWith("pack/textures/required")
                        || entry.getName().startsWith("pack/models/required")) {
                    OraxenPlugin.get().getResourceManager().extractFileIfTrue(entry,
                            !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists());
                }
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
            zip.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void extract(ZipEntry entry, ResourcesManager resourcesManager, boolean isSuitable) {
        final String name = entry.getName();
        resourcesManager.extractFileIfTrue(entry, isSuitable);
    }

    private Map<Material, Map<String, ItemBuilder>> extractTexturedItems() {
        final Map<Material, Map<String, ItemBuilder>> texturedItems = new HashMap<>();
        for (final Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            final String itemId = entry.getKey();
            final ItemBuilder item = entry.getValue();
            OraxenMeta oraxenMeta = item.getOraxenMeta();
            if (item.hasOraxenMeta() && oraxenMeta.hasPackInfos()) {
                String modelName = oraxenMeta.getModelName() + ".json";
                String modelPath = oraxenMeta.getModelPath();
                if (oraxenMeta.shouldGenerateModel()) {
                    writeStringToVirtual(modelPath, modelName, new ModelGenerator(oraxenMeta).getJson().toString());
                }
                final Map<String, ItemBuilder> items = texturedItems.computeIfAbsent(item.build().getType(),
                        k -> new LinkedHashMap<>());

                // Insert in order of CustomModelData
                List<Map.Entry<String, ItemBuilder>> sortedItems = new ArrayList<>(items.entrySet());
                int insertIndex = 0;
                Integer newCmd = Optional.ofNullable(oraxenMeta.getCustomModelData()).orElse(0);

                for (Map.Entry<String, ItemBuilder> existingEntry : sortedItems) {
                    Integer existingCmd = Optional
                            .ofNullable(existingEntry.getValue().getOraxenMeta().getCustomModelData()).orElse(0);
                    if (existingCmd > newCmd)
                        break;
                    insertIndex++;
                }

                // Rebuild map in correct order
                Map<String, ItemBuilder> newItems = new LinkedHashMap<>();
                for (int i = 0; i < sortedItems.size(); i++) {
                    if (i == insertIndex) {
                        newItems.put(itemId, item);
                    }
                    Map.Entry<String, ItemBuilder> existingEntry = sortedItems.get(i);
                    newItems.put(existingEntry.getKey(), existingEntry.getValue());
                }
                if (insertIndex >= sortedItems.size()) {
                    newItems.put(itemId, item);
                }

                texturedItems.put(item.build().getType(), newItems);
            }
        }
        return texturedItems;
    }

    @SafeVarargs
    public final void addModifiers(String groupName, final Consumer<File>... modifiers) {
        packModifiers.put(groupName, Arrays.asList(modifiers));
    }

    public static void addOutputFiles(final VirtualFile... files) {
        for (VirtualFile file : files)
            outputFiles.put(file.getPath(), file);
    }

    public File getFile() {
        return pack;
    }

    public File getPackFolder() {
        return packFolder;
    }

    private void extractInPackIfNotExists(final File file) {
        if (!file.exists())
            OraxenPlugin.get().saveResource("pack/" + file.getName(), true);
    }

    private void makeDirsIfNotExists(final File... folders) {
        for (final File folder : folders)
            if (!folder.exists())
                folder.mkdirs();
    }

    private void generatePredicates(final Map<Material, Map<String, ItemBuilder>> texturedItems) {
        for (final Map.Entry<Material, Map<String, ItemBuilder>> texturedItemsEntry : texturedItems.entrySet()) {
            final Material entryMaterial = texturedItemsEntry.getKey();
            final PredicatesGenerator predicatesGenerator = new PredicatesGenerator(entryMaterial,
                    new ArrayList<>(texturedItemsEntry.getValue().values()));
            final String[] vanillaModelPath = (predicatesGenerator.getVanillaModelName(entryMaterial) + ".json")
                    .split("/");
            writeStringToVirtual("assets/minecraft/models/" + vanillaModelPath[0], vanillaModelPath[1],
                    predicatesGenerator.toJSON().toString());
        }
    }

    /**
     * Generates vanilla item model definitions (assets/minecraft/items/*.json) for 1.21.4+.
     *
     * @param texturedItems    the items to generate definitions for
     * @param useSelect        true for {@code minecraft:select} on strings (MODEL_DATA_IDS),
     *                         false for {@code minecraft:range_dispatch} on floats (MODEL_DATA_FLOAT_LEGACY)
     * @param includeBothModes if true, generates both select (strings) AND range_dispatch (floats)
     *                         dispatchers for maximum compatibility with external plugins
     */
    private void generateVanillaItemDefinitions(final Map<Material, Map<String, ItemBuilder>> texturedItems,
            boolean useSelect, boolean includeBothModes) {
        for (final Map.Entry<Material, Map<String, ItemBuilder>> texturedItemsEntry : texturedItems.entrySet()) {
            final Material material = texturedItemsEntry.getKey();
            final List<ItemBuilder> items = new ArrayList<>(texturedItemsEntry.getValue().values());

            final VanillaItemDefinitionGenerator generator = new VanillaItemDefinitionGenerator(
                    material, items, useSelect, includeBothModes);
            writeStringToVirtual("assets/minecraft/items", generator.getFileName(),
                    generator.toJSON().toString());
        }
    }

    private static boolean needsTinting(Material material) {
        return material.name().startsWith("LEATHER_")
                || material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private void generateModelDefinitions(final Map<Material, Map<String, ItemBuilder>> texturedItems) {
        for (final Map.Entry<Material, Map<String, ItemBuilder>> materialEntry : texturedItems.entrySet()) {
            for (final Map.Entry<String, ItemBuilder> entry : materialEntry.getValue().entrySet()) {
                Material key = materialEntry.getKey();

                String itemId = entry.getKey();
                ItemBuilder texturedItem = entry.getValue();
                OraxenMeta oraxenMeta = texturedItem.getOraxenMeta();
                if (oraxenMeta.hasPackInfos()) {
                    // Generate the main item model definition
                    final ModelDefinitionGenerator modelDefinitionGenerator = new ModelDefinitionGenerator(oraxenMeta,
                            key);
                    writeStringToVirtual("assets/oraxen/items/", itemId + ".json",
                            modelDefinitionGenerator.toJSON().toString());

                    // Generate additional model definitions from Pack.models
                    // These are registered as oraxen:<itemId>/<key> -> modelPath
                    if (oraxenMeta.hasAdditionalModels()) {
                        for (Map.Entry<String, String> modelEntry : oraxenMeta.getAdditionalModels().entrySet()) {
                            String modelKey = modelEntry.getKey();
                            String modelPath = modelEntry.getValue();
                            String additionalDefinitionId = itemId + "/" + modelKey;
                            String additionalDefinition = createSimpleModelDefinition(modelPath);
                            writeStringToVirtual("assets/oraxen/items/", additionalDefinitionId + ".json",
                                    additionalDefinition);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a simple model definition JSON for additional models.
     * Used by Pack.models to register model aliases.
     */
    private String createSimpleModelDefinition(String modelPath) {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonObject model = new com.google.gson.JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelPath);
        root.add("model", model);
        return root.toString();
    }

    private Map<Material, Map<String, ItemBuilder>> filterForItemModel(
            Map<Material, Map<String, ItemBuilder>> texturedItems) {
        Map<Material, Map<String, ItemBuilder>> filtered = new HashMap<>();
        for (Map.Entry<Material, Map<String, ItemBuilder>> materialEntry : texturedItems.entrySet()) {
            Map<String, ItemBuilder> filteredItems = new LinkedHashMap<>();
            for (Map.Entry<String, ItemBuilder> entry : materialEntry.getValue().entrySet()) {
                OraxenMeta meta = entry.getValue().getOraxenMeta();
                if (meta != null && !meta.isExcludedFromItemModel()) {
                    filteredItems.put(entry.getKey(), entry.getValue());
                }
            }
            if (!filteredItems.isEmpty()) {
                filtered.put(materialEntry.getKey(), filteredItems);
            }
        }
        return filtered;
    }

    private Map<Material, Map<String, ItemBuilder>> filterForPredicates(
            Map<Material, Map<String, ItemBuilder>> texturedItems) {
        Map<Material, Map<String, ItemBuilder>> filtered = new HashMap<>();
        for (Map.Entry<Material, Map<String, ItemBuilder>> materialEntry : texturedItems.entrySet()) {
            Map<String, ItemBuilder> filteredItems = new LinkedHashMap<>();
            for (Map.Entry<String, ItemBuilder> entry : materialEntry.getValue().entrySet()) {
                OraxenMeta meta = entry.getValue().getOraxenMeta();
                if (meta != null && !meta.isExcludedFromPredicates()) {
                    filteredItems.put(entry.getKey(), entry.getValue());
                }
            }
            if (!filteredItems.isEmpty()) {
                filtered.put(materialEntry.getKey(), filteredItems);
            }
        }
        return filtered;
    }

    private void generateFont() {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        if (!fontManager.autoGenerate)
            return;

        // Generate the main default font with glyphs
        final JsonObject output = new JsonObject();
        final JsonArray providers = new JsonArray();
        for (final Glyph glyph : fontManager.getGlyphs()) {
            if (!glyph.hasBitmap())
                providers.add(glyph.toJson());
        }
        for (FontManager.GlyphBitMap glyphBitMap : FontManager.glyphBitMaps.values()) {
            providers.add(glyphBitMap.toJson(fontManager));
        }
        for (final Font font : fontManager.getFonts()) {
            providers.add(font.toJson());
        }

        // Add shift provider to default font for backward compatibility.
        // This allows getShift() to work in plain strings (e.g., GUI titles)
        // without requiring the oraxen:shift font to be explicitly applied.
        ShiftProvider shiftProvider = fontManager.getShiftProvider();
        providers.add(shiftProvider.toProviderJson());

        output.add("providers", providers);
        writeStringToVirtual("assets/minecraft/font", "default.json", output.toString());
        if (Settings.FIX_FORCE_UNICODE_GLYPHS.toBool())
            writeStringToVirtual("assets/minecraft/font", "uniform.json", output.toString());

        // Generate the dedicated shift font (still useful for explicit font references)
        generateShiftFont(fontManager);

        // Process and generate animated glyph fonts
        processAnimatedGlyphs(fontManager);
    }

    /**
     * Generates the dedicated shift font file (assets/oraxen/font/shift.json).
     * Uses a space font provider for efficient pixel-based text shifting.
     */
    private void generateShiftFont(FontManager fontManager) {
        ShiftProvider shiftProvider = fontManager.getShiftProvider();
        JsonObject shiftFont = shiftProvider.generateFontFile();
        writeStringToVirtual("assets/oraxen/font", "shift.json", shiftFont.toString());
        if (Settings.DEBUG.toBool()) Logs.logInfo("Generated shift font with space provider");
    }

    /**
     * Processes animated glyphs: validates sprite sheets and generates font files.
     */
    private void processAnimatedGlyphs(FontManager fontManager) {
        Collection<AnimatedGlyph> animatedGlyphs = fontManager.getAnimatedGlyphs();
        if (animatedGlyphs.isEmpty()) {
            return;
        }

        // Note: Codepoint counter is reset in ConfigsManager.parseAllGlyphConfigs()
        // BEFORE animated glyphs are created, ensuring clean codepoint allocation on
        // reload.

        Logs.logInfo("Processing " + animatedGlyphs.size() + " animated glyphs...");

        for (AnimatedGlyph animGlyph : animatedGlyphs) {
            processAnimatedGlyph(animGlyph);
        }

        // Generate animation shaders and set flag for scoreboard shader combining
        generateAnimationShaders();
        animationShadersGenerated = true;
    }

    /**
     * Processes a single animated glyph: validates sprite sheet and generates font.
     */
    private void processAnimatedGlyph(AnimatedGlyph animGlyph) {
        File textureFile = animGlyph.getTextureFile(packFolder.toPath());

        if (!textureFile.exists()) {
            Logs.logWarning("Sprite sheet not found for animated glyph '" + animGlyph.getName() + "': "
                    + textureFile.getPath());
            return;
        }

        try {
            BufferedImage image = ImageIO.read(textureFile);
            if (image == null) {
                Logs.logError("Failed to read sprite sheet for: " + animGlyph.getName());
                return;
            }

            BufferedImage sheetImage = prepareAnimationSpriteSheet(animGlyph, image);
            if (sheetImage == null) return;

            boolean generatedStrip = (sheetImage != image);
            String spriteSheetPath = writeSpriteSheetIfNeeded(animGlyph, sheetImage, generatedStrip);

            animGlyph.setProcessed(spriteSheetPath);
            generateAnimationFont(animGlyph);
        } catch (IOException e) {
            Logs.logError("Failed to process sprite sheet for: " + animGlyph.getName());
            Logs.debug(e);
        }
    }

    /**
     * Prepares the sprite sheet for animation, converting vertical to horizontal if needed.
     */
    private BufferedImage prepareAnimationSpriteSheet(AnimatedGlyph animGlyph, BufferedImage image) {
        int frameCount = animGlyph.getFrameCount();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        boolean widthDiv = imageWidth % frameCount == 0;
        boolean heightDiv = imageHeight % frameCount == 0;

        if (!heightDiv && !widthDiv) {
            Logs.logWarning("Sprite sheet dimensions (" + imageWidth + "x" + imageHeight +
                    ") are not divisible by frame count (" + frameCount + ") for: " + animGlyph.getName());
        }

        boolean vertical = heightDiv && (!widthDiv || imageHeight >= imageWidth);
        boolean horizontal = widthDiv && (!heightDiv || imageWidth > imageHeight);

        if (vertical && heightDiv) {
            return convertVerticalToHorizontalStrip(animGlyph, image, frameCount);
        } else if (!horizontal) {
            Logs.logWarning("Unable to determine sprite sheet orientation for: " + animGlyph.getName());
        }
        return image;
    }

    /**
     * Converts a vertical sprite sheet to horizontal strip format.
     */
    private BufferedImage convertVerticalToHorizontalStrip(AnimatedGlyph animGlyph, BufferedImage image, int frameCount) {
        int frameHeight = image.getHeight() / frameCount;
        int frameWidth = image.getWidth();
        if (frameHeight <= 0) {
            Logs.logWarning("Invalid frame height for animated glyph '" + animGlyph.getName() + "'");
            return null;
        }

        BufferedImage horizontalStrip = new BufferedImage(frameWidth * frameCount, frameHeight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = horizontalStrip.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        for (int i = 0; i < frameCount; i++) {
            BufferedImage frame = image.getSubimage(0, i * frameHeight, frameWidth, frameHeight);
            graphics.drawImage(frame, i * frameWidth, 0, null);
        }
        graphics.dispose();
        return horizontalStrip;
    }

    /**
     * Writes the sprite sheet to virtual files if it was generated, returns the resource path.
     */
    private String writeSpriteSheetIfNeeded(AnimatedGlyph animGlyph, BufferedImage sheetImage, boolean generatedStrip) {
        String texturePath = animGlyph.getTexturePath();
        String namespace = "minecraft";
        String relativePath = texturePath;

        if (texturePath.contains(":")) {
            String[] split = texturePath.split(":", 2);
            namespace = split[0];
            relativePath = split[1];
        }
        if (relativePath.endsWith(".png")) {
            relativePath = relativePath.substring(0, relativePath.length() - 4);
        }

        String finalPath = generatedStrip ? relativePath + "_strip" : relativePath;
        String spriteSheetPath = namespace + ":" + finalPath + ".png";

        if (generatedStrip) {
            String filePath = finalPath + ".png";
            int lastSlash = filePath.lastIndexOf('/');
            String folder = "assets/" + namespace + "/textures";
            String name = filePath;
            if (lastSlash >= 0) {
                folder = folder + "/" + filePath.substring(0, lastSlash);
                name = filePath.substring(lastSlash + 1);
            }
            writeImageToVirtual(folder, name, sheetImage);
        }
        return spriteSheetPath;
    }

    /**
     * Generates the font file for an animated glyph.
     */
    private void generateAnimationFont(AnimatedGlyph animGlyph) {
        JsonObject fontJson = animGlyph.toFontJson();
        if (fontJson != null) {
            writeStringToVirtual("assets/oraxen/font/animations", animGlyph.getName() + ".json",
                    fontJson.toString());
            Logs.logSuccess("Generated animation font for: " + animGlyph.getName() +
                    " (" + animGlyph.getFrameCount() + " frames @ " + animGlyph.getFps() + " fps)");
        }
    }

    /**
     * Generates animation shaders based on server version.
     * Different Minecraft versions use different shader formats.
     */
    private void generateAnimationShaders() {
        // Determine shader version based on pack format / server version
        String shaderVersion = getShaderVersion();

        // Generate shaders (see-through uses a different vertex format on 1.21.6+)
        String vshContent = getAnimationVertexShader(shaderVersion, false);
        String fshContent = getAnimationFragmentShader(shaderVersion, false);
        String jsonContent = getAnimationShaderJson(shaderVersion, false);

        String vshSeeThrough = getAnimationVertexShader(shaderVersion, true);
        String fshSeeThrough = getAnimationFragmentShader(shaderVersion, true);
        String jsonSeeThrough = getAnimationShaderJson(shaderVersion, true);

        String vshIntensity = getAnimationVertexShader(shaderVersion, false);
        String fshIntensity = getAnimationFragmentShader(shaderVersion, false, true);
        String jsonIntensity = getAnimationShaderJson(shaderVersion, "rendertype_text_intensity", false);

        String vshIntensitySeeThrough = getAnimationVertexShader(shaderVersion, true);
        String fshIntensitySeeThrough = getAnimationFragmentShader(shaderVersion, true, true);
        String jsonIntensitySeeThrough = getAnimationShaderJson(shaderVersion, "rendertype_text_intensity_see_through", true);

        // Write shaders for both rendertype_text and rendertype_text_see_through
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text.vsh", vshContent);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text.fsh", fshContent);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text.json", jsonContent);

        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_see_through.vsh", vshSeeThrough);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_see_through.fsh", fshSeeThrough);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_see_through.json", jsonSeeThrough);

        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity.vsh", vshIntensity);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity.fsh", fshIntensity);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity.json", jsonIntensity);

        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity_see_through.vsh", vshIntensitySeeThrough);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity_see_through.fsh", fshIntensitySeeThrough);
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_text_intensity_see_through.json", jsonIntensitySeeThrough);

        Logs.logSuccess("Generated animation shaders for version: " + shaderVersion);
    }

    /**
     * Determines the shader version based on server version.
     */
    private String getShaderVersion() {
        if (VersionUtil.atOrAbove("1.21.6")) {
            return "1.21.6";
        } else if (VersionUtil.atOrAbove("1.21.4")) {
            return "1.21.4";
        } else if (VersionUtil.atOrAbove("1.21")) {
            return "1.21";
        } else {
            return "1.20";
        }
    }

    /**
     * Generates animation vertex shader using visibility-based animation.
     * Each frame is a separate character; the shader hides frames that don't match current time.
     * Also handles text effects (R=253) with position-based effects like wave, shake, wobble.
     *
     * Color encoding for animated glyphs:
     * - R = 254: animation marker
     * - G bits 0-6: FPS, bit 7: loop flag
     * - B bits 0-3: frame index, bits 4-7: total frames - 1
     *
     * Color encoding for text effects:
     * - R = 253: text effect marker
     * - G bits 7 and 3: marker bits (must be 1)
     * - G bits 0-2: effect type, bits 4-6: speed
     * - B bits 7 and 3: marker bits (must be 1)
     * - B bits 0-2: char index, bits 4-6: param (amplitude/intensity)
     */
    private String getAnimationVertexShader(String version, boolean seeThrough) {
        boolean is1_21_6Plus = version.compareTo("1.21.6") >= 0;
        boolean is1_21_4Plus = version.compareTo("1.21.4") >= 0;

        if (is1_21_6Plus) {
            if (seeThrough) {
                return """
                    #version 330

                    #moj_import <minecraft:dynamictransforms.glsl>
                    #moj_import <minecraft:projection.glsl>
                    #moj_import <minecraft:globals.glsl>

                    in vec3 Position;
                    in vec4 Color;
                    in vec2 UV0;

                    out vec4 vertexColor;
                    out vec2 texCoord0;
                    out vec4 effectData; // Pass effect info to fragment shader

                    void main() {
                        vec3 pos = Position;
                        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                        texCoord0 = UV0;
                        vertexColor = Color;
                        effectData = vec4(0.0);

                        int rInt = int(Color.r * 255.0 + 0.5);

                        // Check for animation color: R=254 for primary, R≈63 for shadow
                        bool isPrimaryAnim = (rInt == 254);
                        bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                        if (isPrimaryAnim || isShadowAnim) {
                            int gRaw = int(Color.g * 255.0 + 0.5);
                            int bRaw = int(Color.b * 255.0 + 0.5);
                            int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                            int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                            bool loop = (gInt < 128);
                            float fps = max(1.0, float(gInt & 0x7F));
                            int frameIndex = bInt & 0x0F;
                            int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                            int rawFrame = int(floor(timeSeconds * fps));
                            int currentFrame = loop ? (rawFrame % totalFrames) : min(rawFrame, totalFrames - 1);

                            float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                            if (isPrimaryAnim) {
                                vertexColor = vec4(1.0, 1.0, 1.0, visible);
                            } else {
                                vertexColor = vec4(0.0);
                            }
                        }

                        // Text effects: R=253 for primary
                        // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                        // Note: Text effect shadows share the animation shadow range and are hidden above.
                        bool isPrimaryEffect = (rInt == 253);

                        if (isPrimaryEffect) {
                            int gRaw = int(Color.g * 255.0 + 0.5);
                            int bRaw = int(Color.b * 255.0 + 0.5);
                            bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                            if (hasMarker) {
                                int effectType = gRaw & 0x07;
                                float speed = max(1.0, float((gRaw >> 4) & 0x07));
                                float charIndex = float(bRaw & 0x07);
                                float param = float((bRaw >> 4) & 0x07);

                                float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                                // Wave effect (type 1): vertical sine wave
                                if (effectType == 1) {
                                    float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.y += sin(phase) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                }
                                // Shake effect (type 2): random jitter
                                else if (effectType == 2) {
                                    float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                    pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                }
                                // Wobble effect (type 6): circular oscillation
                                else if (effectType == 6) {
                                    float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.x += cos(phase) * amplitude;
                                    pos.y += sin(phase) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                }

                                // Pass effect data to fragment shader
                                // x=effectType, y=speed, z=charIndex, w=param
                                effectData = vec4(float(effectType), speed, charIndex, param);
                                vertexColor = Color; // Keep original color for fragment shader processing
                            }
                        }
                    }
                    """;
            }

            return """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;

                out float sphericalVertexDistance;
                out float cylindricalVertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                out vec4 effectData;
                out vec4 effectData; // Pass effect info to fragment shader

                void main() {
                    vec3 pos = Position;
                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                    sphericalVertexDistance = fog_spherical_distance(pos);
                    cylindricalVertexDistance = fog_cylindrical_distance(pos);
                    texCoord0 = UV0;
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    effectData = vec4(0.0);

                    int rInt = int(Color.r * 255.0 + 0.5);

                    // Check for animation color: R=254 for primary, R≈63 for shadow
                    bool isPrimaryAnim = (rInt == 254);
                    bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                    if (isPrimaryAnim || isShadowAnim) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                        int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                        bool loop = (gInt < 128);
                        float fps = max(1.0, float(gInt & 0x7F));
                        int frameIndex = bInt & 0x0F;
                        int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                        int rawFrame = int(floor(timeSeconds * fps));
                        int currentFrame = loop ? (rawFrame % totalFrames) : min(rawFrame, totalFrames - 1);

                        float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                        if (isPrimaryAnim) {
                            vertexColor = vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0);
                        } else {
                            vertexColor = vec4(0.0);
                        }
                    }

                    // Text effects: R=253 for primary
                    // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                    // Note: Text effect shadows share the animation shadow range and are hidden above.
                    bool isPrimaryEffect = (rInt == 253);

                    if (isPrimaryEffect) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                        if (hasMarker) {
                            int effectType = gRaw & 0x07;
                            float speed = max(1.0, float((gRaw >> 4) & 0x07));
                            float charIndex = float(bRaw & 0x07);
                            float param = float((bRaw >> 4) & 0x07);

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                            // Wave effect (type 1): vertical sine wave
                            if (effectType == 1) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }
                            // Shake effect (type 2): random jitter
                            else if (effectType == 2) {
                                float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }
                            // Wobble effect (type 6): circular oscillation
                            else if (effectType == 6) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += cos(phase) * amplitude;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }
                            // Wobble effect (type 6): circular oscillation
                            else if (effectType == 6) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += cos(phase) * amplitude;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }

                            // Pass effect data to fragment shader
                            effectData = vec4(float(effectType), speed, charIndex, param);
                            vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                        }
                    }
                }
                """;
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = is1_21_4Plus ? "#moj_import <minecraft:fog.glsl>" : "#moj_import <fog.glsl>";

            if (seeThrough) {
                return """
                    #version 150

                    %s

                    in vec3 Position;
                    in vec4 Color;
                    in vec2 UV0;

                    uniform mat4 ModelViewMat;
                    uniform mat4 ProjMat;
                    uniform int FogShape;
                    uniform float GameTime;

                    out float vertexDistance;
                    out vec4 vertexColor;
                    out vec2 texCoord0;
                    out vec4 effectData; // Pass effect info to fragment shader

                    void main() {
                        vec3 pos = Position;
                        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                        vertexDistance = fog_distance(pos, FogShape);
                        texCoord0 = UV0;
                        vertexColor = Color;
                        effectData = vec4(0.0);

                        int rInt = int(Color.r * 255.0 + 0.5);

                        // Check for animation color: R=254 for primary, R≈63 for shadow
                        bool isPrimaryAnim = (rInt == 254);
                        bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                        if (isPrimaryAnim || isShadowAnim) {
                            int gRaw = int(Color.g * 255.0 + 0.5);
                            int bRaw = int(Color.b * 255.0 + 0.5);
                            int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                            int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                            bool loop = (gInt < 128);
                            float fps = max(1.0, float(gInt & 0x7F));
                            int frameIndex = bInt & 0x0F;
                            int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                            int rawFrame = int(floor(timeSeconds * fps));
                            int currentFrame = loop ? int(mod(float(rawFrame), float(totalFrames))) : min(rawFrame, totalFrames - 1);

                            float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                            if (isPrimaryAnim) {
                                vertexColor = vec4(1.0, 1.0, 1.0, visible);
                            } else {
                                vertexColor = vec4(0.0);
                            }
                        }

                        // Text effects: R=253 for primary
                        // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                        bool isPrimaryEffect = (rInt == 253);

                        if (isPrimaryEffect) {
                            int gRaw = int(Color.g * 255.0 + 0.5);
                            int bRaw = int(Color.b * 255.0 + 0.5);
                            bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                            if (hasMarker) {
                                int effectType = gRaw & 0x07;
                                float speed = max(1.0, float((gRaw >> 4) & 0x07));
                                float charIndex = float(bRaw & 0x07);
                                float param = float((bRaw >> 4) & 0x07);

                                float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                                // Wave effect (type 1): vertical sine wave
                                if (effectType == 1) {
                                    float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.y += sin(phase) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                    vertexDistance = fog_distance(pos, FogShape);
                                }
                                // Shake effect (type 2): random jitter
                                else if (effectType == 2) {
                                    float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                    pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                    vertexDistance = fog_distance(pos, FogShape);
                                }
                                // Wobble effect (type 6): circular oscillation
                                else if (effectType == 6) {
                                    float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                    float amplitude = max(1.0, param) * 0.15;
                                    pos.x += cos(phase) * amplitude;
                                    pos.y += sin(phase) * amplitude;
                                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                    vertexDistance = fog_distance(pos, FogShape);
                                }

                                // Pass effect data to fragment shader
                                effectData = vec4(float(effectType), speed, charIndex, param);
                                vertexColor = Color;
                            }
                        }
                    }
                    """.formatted(imports);
            }

            return """
                #version 150

                %s

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform int FogShape;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData; // Pass effect info to fragment shader

                void main() {
                    vec3 pos = Position;
                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                    vertexDistance = fog_distance(pos, FogShape);
                    texCoord0 = UV0;
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    effectData = vec4(0.0);

                    int rInt = int(Color.r * 255.0 + 0.5);

                    // Check for animation color: R=254 for primary, R≈63 for shadow
                    bool isPrimaryAnim = (rInt == 254);
                    bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                    if (isPrimaryAnim || isShadowAnim) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                        int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                        bool loop = (gInt < 128);
                        float fps = max(1.0, float(gInt & 0x7F));
                        int frameIndex = bInt & 0x0F;
                        int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                        int rawFrame = int(floor(timeSeconds * fps));
                        int currentFrame = loop ? int(mod(float(rawFrame), float(totalFrames))) : min(rawFrame, totalFrames - 1);

                        float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                        if (isPrimaryAnim) {
                            vertexColor = vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0);
                        } else {
                            vertexColor = vec4(0.0);
                        }
                    }

                    // Text effects: R=253 for primary
                    // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                    // Note: Text effect shadows share the animation shadow range and are hidden above.
                    bool isPrimaryEffect = (rInt == 253);

                    if (isPrimaryEffect) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                        if (hasMarker) {
                            int effectType = gRaw & 0x07;
                            float speed = max(1.0, float((gRaw >> 4) & 0x07));
                            float charIndex = float(bRaw & 0x07);
                            float param = float((bRaw >> 4) & 0x07);

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                            // Wave effect (type 1): vertical sine wave
                            if (effectType == 1) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }
                            // Shake effect (type 2): random jitter
                            else if (effectType == 2) {
                                float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }
                            // Wobble effect (type 6): circular oscillation
                            else if (effectType == 6) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += cos(phase) * amplitude;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }
                            // Wobble effect (type 6): circular oscillation
                            else if (effectType == 6) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += cos(phase) * amplitude;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }

                            // Pass effect data to fragment shader
                            effectData = vec4(float(effectType), speed, charIndex, param);
                            vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                        }
                    }
                }
                """.formatted(imports);
        }
    }

    /**
     * Generates a simple fragment shader - visibility is handled in vertex shader.
     */
    private String getAnimationFragmentShader(String version, boolean seeThrough) {
        return getAnimationFragmentShader(version, seeThrough, false);
    }

    private String getAnimationFragmentShader(String version, boolean seeThrough, boolean intensity) {
        boolean is1_21_6Plus = version.compareTo("1.21.6") >= 0;
        boolean is1_21_4Plus = version.compareTo("1.21.4") >= 0;
        String sampleExpr = intensity ? "texture(Sampler0, texCoord0).rrrr" : "texture(Sampler0, texCoord0)";

        // HSV to RGB function for rainbow effect
        String hsvToRgb = """
                // HSV to RGB conversion for rainbow effect
                vec3 hsv2rgb(vec3 c) {
                    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
                    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                }
                """;

        if (is1_21_6Plus) {
            if (seeThrough) {
                return """
                    #version 330

                    #moj_import <minecraft:dynamictransforms.glsl>
                    #moj_import <minecraft:globals.glsl>

                    uniform sampler2D Sampler0;

                    in vec4 vertexColor;
                    in vec2 texCoord0;
                    in vec4 effectData;

                    out vec4 fragColor;

                    %s

                    void main() {
                        vec4 color = %s * vertexColor * ColorModulator;

                        // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                        if (effectData.x >= 0.0 && effectData.y > 0.5) {
                            int effectType = int(effectData.x + 0.5);
                            float speed = effectData.y;
                            float charIndex = effectData.z;
                            float param = effectData.w;
                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                            // Rainbow effect (type 0)
                            if (effectType == 0) {
                                float hue = mod(charIndex * 0.08 + timeSeconds * speed * 0.05, 1.0);
                                vec3 rgb = hsv2rgb(vec3(hue, 1.0, 1.0));
                                color.rgb = rgb;  // Replace color (not multiply) for full brightness
                            }
                            // Pulse effect (type 3): opacity fades in/out
                            else if (effectType == 3) {
                                float pulse = (sin(timeSeconds * speed * 0.5 + charIndex * 0.3) + 1.0) * 0.5;
                                color.a *= 0.3 + pulse * 0.7;
                            }
                            // Gradient effect (type 4): static color gradient
                            else if (effectType == 4) {
                                float t = charIndex / 15.0;
                                vec3 startColor = vec3(1.0, 0.3, 0.3);
                                vec3 endColor = vec3(0.3, 0.3, 1.0);
                                color.rgb = mix(startColor, endColor, t);  // Replace color for full brightness
                            }
                            // Typewriter effect (type 5): characters appear sequentially
                            else if (effectType == 5) {
                                float revealTime = charIndex / (speed * 2.0);
                                if (mod(timeSeconds, 16.0 / speed) < revealTime) {
                                    color.a = 0.0;
                                }
                            }
                            // Obfuscate effect (type 7): rapid color cycling
                            else if (effectType == 7) {
                                float seed = charIndex + floor(timeSeconds * speed * 10.0);
                                float r = fract(sin(seed * 12.9898) * 43758.5453);
                                float g = fract(sin(seed * 78.233) * 43758.5453);
                                float b = fract(sin(seed * 37.719) * 43758.5453);
                                color.rgb = vec3(r, g, b);
                            }
                        }

                        if (color.a < 0.1) {
                            discard;
                        }
                        fragColor = color;
                    }
                    """.formatted(hsvToRgb, sampleExpr);
            }

            return """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:globals.glsl>

                uniform sampler2D Sampler0;

                in float sphericalVertexDistance;
                in float cylindricalVertexDistance;
                in vec4 vertexColor;
                in vec2 texCoord0;
                in vec4 effectData;

                out vec4 fragColor;

                %s

                void main() {
                    vec4 color = %s * vertexColor * ColorModulator;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                        // Rainbow effect (type 0)
                        if (effectType == 0) {
                            float hue = mod(charIndex * 0.08 + timeSeconds * speed * 0.05, 1.0);
                            vec3 rgb = hsv2rgb(vec3(hue, 1.0, 1.0));
                            color.rgb = rgb;  // Replace color (not multiply) for full brightness
                        }
                        // Pulse effect (type 3): opacity fades in/out
                        else if (effectType == 3) {
                            float pulse = (sin(timeSeconds * speed * 0.5 + charIndex * 0.3) + 1.0) * 0.5;
                            color.a *= 0.3 + pulse * 0.7;
                        }
                        // Gradient effect (type 4): static color gradient
                        else if (effectType == 4) {
                            float t = charIndex / 15.0;
                            vec3 startColor = vec3(1.0, 0.3, 0.3);
                            vec3 endColor = vec3(0.3, 0.3, 1.0);
                            color.rgb = mix(startColor, endColor, t);  // Replace color for full brightness
                        }
                        // Typewriter effect (type 5): characters appear sequentially
                        else if (effectType == 5) {
                            float revealTime = charIndex / (speed * 2.0);
                            if (mod(timeSeconds, 16.0 / speed) < revealTime) {
                                color.a = 0.0;
                            }
                        }
                        // Obfuscate effect (type 7): rapid color cycling
                        else if (effectType == 7) {
                            float seed = charIndex + floor(timeSeconds * speed * 10.0);
                            float r = fract(sin(seed * 12.9898) * 43758.5453);
                            float g = fract(sin(seed * 78.233) * 43758.5453);
                            float b = fract(sin(seed * 37.719) * 43758.5453);
                            color.rgb = vec3(r, g, b);
                        }
                    }

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
                }
                """.formatted(hsvToRgb, sampleExpr);
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = is1_21_4Plus ? "#moj_import <minecraft:fog.glsl>" : "#moj_import <fog.glsl>";

            return """
                #version 150

                %s

                uniform sampler2D Sampler0;
                uniform vec4 ColorModulator;
                uniform float FogStart;
                uniform float FogEnd;
                uniform vec4 FogColor;
                uniform float GameTime;

                in float vertexDistance;
                in vec4 vertexColor;
                in vec2 texCoord0;
                in vec4 effectData;

                out vec4 fragColor;

                %s

                void main() {
                    vec4 color = %s * vertexColor * ColorModulator;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                        // Rainbow effect (type 0)
                        if (effectType == 0) {
                            float hue = mod(charIndex * 0.08 + timeSeconds * speed * 0.05, 1.0);
                            vec3 rgb = hsv2rgb(vec3(hue, 1.0, 1.0));
                            color.rgb = rgb;  // Replace color (not multiply) for full brightness
                        }
                        // Pulse effect (type 3): opacity fades in/out
                        else if (effectType == 3) {
                            float pulse = (sin(timeSeconds * speed * 0.5 + charIndex * 0.3) + 1.0) * 0.5;
                            color.a *= 0.3 + pulse * 0.7;
                        }
                        // Gradient effect (type 4): static color gradient
                        else if (effectType == 4) {
                            float t = charIndex / 15.0;
                            vec3 startColor = vec3(1.0, 0.3, 0.3);
                            vec3 endColor = vec3(0.3, 0.3, 1.0);
                            color.rgb = mix(startColor, endColor, t);  // Replace color for full brightness
                        }
                        // Typewriter effect (type 5): characters appear sequentially
                        else if (effectType == 5) {
                            float revealTime = charIndex / (speed * 2.0);
                            if (mod(timeSeconds, 16.0 / speed) < revealTime) {
                                color.a = 0.0;
                            }
                        }
                        // Obfuscate effect (type 7): rapid color cycling
                        else if (effectType == 7) {
                            float seed = charIndex + floor(timeSeconds * speed * 10.0);
                            float r = fract(sin(seed * 12.9898) * 43758.5453);
                            float g = fract(sin(seed * 78.233) * 43758.5453);
                            float b = fract(sin(seed * 37.719) * 43758.5453);
                            color.rgb = vec3(r, g, b);
                        }
                    }

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """.formatted(imports, hsvToRgb, sampleExpr);
        }
    }

    /**
     * Generates the shader JSON configuration.
     */
    private String getAnimationShaderJson(String version, boolean seeThrough) {
        String shaderName = seeThrough ? "rendertype_text_see_through" : "rendertype_text";
        return getAnimationShaderJson(version, shaderName, seeThrough);
    }

    private String getAnimationShaderJson(String version, String shaderName, boolean seeThrough) {
        boolean is1_21_6Plus = version.compareTo("1.21.6") >= 0;

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks - most uniforms come from imported glsl files
            // Only samplers need to be declared in the JSON
            if (seeThrough) {
                return """
                    {
                        "vertex": "minecraft:core/%s",
                        "fragment": "minecraft:core/%s",
                        "samplers": [
                            { "name": "Sampler0" }
                        ]
                    }
                    """.formatted(shaderName, shaderName);
            }

            return """
                {
                    "vertex": "minecraft:core/%s",
                    "fragment": "minecraft:core/%s",
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ]
                }
                """.formatted(shaderName, shaderName);
        } else {
            if (seeThrough) {
                return """
                    {
                        "blend": {
                            "func": "add",
                            "srcrgb": "srcalpha",
                            "dstrgb": "1-srcalpha"
                        },
                        "vertex": "rendertype_text",
                        "fragment": "rendertype_text",
                        "attributes": [
                            "Position",
                            "Color",
                            "UV0"
                        ],
                        "samplers": [
                            { "name": "Sampler0" }
                        ],
                        "uniforms": [
                            { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                            { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                            { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                            { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                            { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                            { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                            { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] },
                            { "name": "GameTime", "type": "float", "count": 1, "values": [ 0.0 ] }
                        ]
                    }
                    """.replace("rendertype_text", shaderName);
            }

            return """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_text",
                    "fragment": "rendertype_text",
                    "attributes": [
                        "Position",
                        "Color",
                        "UV0",
                        "UV2"
                    ],
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                        { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] },
                        { "name": "GameTime", "type": "float", "count": 1, "values": [ 0.0 ] }
                    ]
                }
                """.replace("rendertype_text", shaderName);
        }
    }

    /**
     * Generates a combined vertex shader that supports both animation and
     * scoreboard number hiding.
     * Uses visibility-based animation: each frame is a separate character,
     * and the shader hides frames that don't match current time.
     */
    private String getCombinedVertexShader(String version) {
        boolean is1_21_6Plus = version.compareTo("1.21.6") >= 0;
        boolean is1_21_4Plus = version.compareTo("1.21.4") >= 0;

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks from globals.glsl
            return """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform vec2 ScreenSize;

                out float sphericalVertexDistance;
                out float cylindricalVertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;

                void main() {
                    vec3 pos = Position;
                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                    sphericalVertexDistance = fog_spherical_distance(pos);
                    cylindricalVertexDistance = fog_cylindrical_distance(pos);
                    texCoord0 = UV0;
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    effectData = vec4(0.0);

                    // Check for animation color: R=254 for primary, R≈63 for shadow
                    int rInt = int(Color.r * 255.0 + 0.5);
                    bool isPrimaryAnim = (rInt == 254);
                    bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                    if (isPrimaryAnim || isShadowAnim) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                        int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                        bool loop = (gInt < 128);
                        float fps = max(1.0, float(gInt & 0x7F));
                        int frameIndex = bInt & 0x0F;
                        int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                        int rawFrame = int(floor(timeSeconds * fps));
                        int currentFrame = loop ? (rawFrame % totalFrames) : min(rawFrame, totalFrames - 1);

                        // Hide this frame if it's not the current one
                        // Shadows always hidden - color precision loss makes all shadows decode to same frameIndex
                        float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                        if (isPrimaryAnim) {
                            vertexColor = vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0);
                        } else {
                            vertexColor = vec4(0.0);
                        }
                    }

                    // Text effects: R=253 for primary
                    // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                    bool isPrimaryEffect = (rInt == 253);

                    if (isPrimaryEffect) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                        if (hasMarker) {
                            int effectType = gRaw & 0x07;
                            float speed = max(1.0, float((gRaw >> 4) & 0x07));
                            float charIndex = float(bRaw & 0x07);
                            float param = float((bRaw >> 4) & 0x07);

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                            // Wave effect (type 1): vertical sine wave
                            if (effectType == 1) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }
                            // Shake effect (type 2): random jitter
                            else if (effectType == 2) {
                                float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                sphericalVertexDistance = fog_spherical_distance(pos);
                                cylindricalVertexDistance = fog_cylindrical_distance(pos);
                            }

                            // Pass effect data to fragment shader
                            effectData = vec4(float(effectType), speed, charIndex, param);
                            vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                        }
                    }

                    // Scoreboard number hiding
                    if (Position.z == 0.0 &&
                            gl_Position.x >= 0.95 && gl_Position.y >= -0.35 &&
                            vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 &&
                            gl_VertexID <= 4) {
                        gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0);
                    }
                }
                """;
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = is1_21_4Plus ? "#moj_import <minecraft:fog.glsl>" : "#moj_import <fog.glsl>";

            return """
                #version 150

                %s

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform int FogShape;
                uniform vec2 ScreenSize;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;

                void main() {
                    vec3 pos = Position;
                    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                    vertexDistance = fog_distance(pos, FogShape);
                    texCoord0 = UV0;
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    effectData = vec4(0.0);

                    // Check for animation color: R=254 for primary, R≈63 for shadow
                    int rInt = int(Color.r * 255.0 + 0.5);
                    bool isPrimaryAnim = (rInt == 254);
                    bool isShadowAnim = (rInt >= 62 && rInt <= 64);

                    if (isPrimaryAnim || isShadowAnim) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                        int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                        bool loop = (gInt < 128);
                        float fps = max(1.0, float(gInt & 0x7F));
                        int frameIndex = bInt & 0x0F;
                        int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                        int rawFrame = int(floor(timeSeconds * fps));
                        int currentFrame = loop ? int(mod(float(rawFrame), float(totalFrames))) : min(rawFrame, totalFrames - 1);

                        // Hide this frame if it's not the current one
                        // Shadows always hidden - color precision loss makes all shadows decode to same frameIndex
                        float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                        if (isPrimaryAnim) {
                            vertexColor = vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0);
                        } else {
                            vertexColor = vec4(0.0);
                        }
                    }

                    // Text effects: R=253 for primary
                    // Marker bits (7 and 3) must be set in G and B to avoid color collisions.
                    bool isPrimaryEffect = (rInt == 253);

                    if (isPrimaryEffect) {
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);
                        bool hasMarker = ((gRaw & 0x88) == 0x88) && ((bRaw & 0x88) == 0x88);

                        if (hasMarker) {
                            int effectType = gRaw & 0x07;
                            float speed = max(1.0, float((gRaw >> 4) & 0x07));
                            float charIndex = float(bRaw & 0x07);
                            float param = float((bRaw >> 4) & 0x07);

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

                            // Wave effect (type 1): vertical sine wave
                            if (effectType == 1) {
                                float phase = charIndex * 0.6 + timeSeconds * speed * 2.0;
                                float amplitude = max(1.0, param) * 0.15;
                                pos.y += sin(phase) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }
                            // Shake effect (type 2): random jitter
                            else if (effectType == 2) {
                                float seed = charIndex + floor(timeSeconds * speed * 8.0);
                                float amplitude = max(1.0, param) * 0.15;
                                pos.x += (fract(sin(seed * 12.9898) * 43758.5453) - 0.5) * amplitude;
                                pos.y += (fract(sin(seed * 78.233) * 43758.5453) - 0.5) * amplitude;
                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                                vertexDistance = fog_distance(pos, FogShape);
                            }

                            // Pass effect data to fragment shader
                            effectData = vec4(float(effectType), speed, charIndex, param);
                            vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                        }
                    }

                    // Scoreboard number hiding
                    if (Position.z == 0.0 &&
                            gl_Position.x >= 0.95 && gl_Position.y >= -0.35 &&
                            vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 &&
                            gl_VertexID <= 4) {
                        gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0);
                    }
                }
                """.formatted(imports);
        }
    }

    /**
     * Generates combined shader JSON that includes uniforms for both animation and
     * scoreboard hiding.
     */
    private String getCombinedShaderJson(String version) {
        boolean is1_21_6Plus = version.compareTo("1.21.6") >= 0;

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks - most uniforms come from imported glsl files
            return """
                {
                    "vertex": "minecraft:core/rendertype_text",
                    "fragment": "minecraft:core/rendertype_text",
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] }
                    ]
                }
                """;
        } else {
            return """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_text",
                    "fragment": "rendertype_text",
                    "attributes": [
                        "Position",
                        "Color",
                        "UV0",
                        "UV2"
                    ],
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                        { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] },
                        { "name": "GameTime", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] }
                    ]
                }
                """;
        }
    }

    private void generateSound(List<VirtualFile> output) {
        SoundManager soundManager = OraxenPlugin.get().getSoundManager();
        if (!soundManager.isAutoGenerate())
            return;

        List<VirtualFile> soundFiles = output.stream()
                .filter(file -> file.getPath().equals("assets/minecraft/sounds.json")).toList();
        JsonObject outputJson = new JsonObject();

        // If file was imported by other means, we attempt to merge in sound.yml entries
        for (VirtualFile soundFile : soundFiles) {
            if (soundFile != null) {
                try {
                    JsonElement soundElement = JsonParser
                            .parseString(IOUtils.toString(soundFile.getInputStream(), StandardCharsets.UTF_8));
                    if (soundElement != null && soundElement.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> entry : soundElement.getAsJsonObject().entrySet())
                            outputJson.add(entry.getKey(), entry.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            output.remove(soundFile);
        }

        Collection<CustomSound> customSounds = handleCustomSoundEntries(soundManager.getCustomSounds());

        // Add all sounds to the sounds.json
        for (CustomSound sound : customSounds) {
            outputJson.add(sound.getName(), sound.toJson());
        }

        InputStream soundInput = new ByteArrayInputStream(outputJson.toString().getBytes(StandardCharsets.UTF_8));
        output.add(new VirtualFile("assets/minecraft", "sounds.json", soundInput));
        try {
            soundInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize JukeboxDatapack with jukebox sounds after processing all sounds
        Collection<CustomSound> jukeboxSounds = customSounds.stream()
                .filter(CustomSound::isJukeboxSound)
                .toList();
        if (!jukeboxSounds.isEmpty()) {
            JukeboxDatapack jukeboxDatapack = new JukeboxDatapack(jukeboxSounds);
            jukeboxDatapack.clearOldDataPack();
            jukeboxDatapack.generateAssets(output);
        }
    }

    private Collection<CustomSound> handleCustomSoundEntries(Collection<CustomSound> sounds) {
        ConfigurationSection mechanic = OraxenPlugin.get().getConfigsManager().getMechanics();
        ConfigurationSection customSounds = mechanic.getConfigurationSection("custom_block_sounds");
        ConfigurationSection noteblock = mechanic.getConfigurationSection("noteblock");
        ConfigurationSection stringblock = mechanic.getConfigurationSection("stringblock");
        ConfigurationSection furniture = mechanic.getConfigurationSection("furniture");
        ConfigurationSection block = mechanic.getConfigurationSection("block");

        handleWoodSoundEntries(sounds, customSounds, noteblock, block);
        handleStoneSoundEntries(sounds, customSounds, stringblock, furniture);

        // Clear the sounds.json file of yaml configuration entries that should not be
        // there
        removeUnwantedSoundEntries(sounds);

        return sounds;
    }

    private void handleWoodSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection noteblock,
            ConfigurationSection block) {
        if (customSounds == null) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
            return;
        }

        if (!customSounds.getBoolean("noteblock_and_block", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
        }

        if (noteblock != null && !noteblock.getBoolean("enabled", true) &&
                block != null && !block.getBoolean("enabled", false)) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
        }
    }

    private void handleStoneSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection stringblock,
            ConfigurationSection furniture) {
        if (customSounds == null) {
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
            return;
        }

        if (!customSounds.getBoolean("stringblock_and_furniture", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
        }

        if (stringblock != null && !stringblock.getBoolean("enabled", true) &&
                furniture != null && !furniture.getBoolean("enabled", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
        }
    }

    private void removeUnwantedSoundEntries(Collection<CustomSound> sounds) {
        sounds.removeIf(s -> s.getName().equals("required") ||
                s.getName().equals("block") ||
                s.getName().equals("block.wood") ||
                s.getName().equals("block.stone") ||
                s.getName().equals("required.wood") ||
                s.getName().equals("required.stone"));
    }

    public static void writeStringToVirtual(String folder, String name, String content) {
        folder = !folder.endsWith("/") ? folder : folder.substring(0, folder.length() - 1);
        addOutputFiles(
                new VirtualFile(folder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    }

    private static void writeImageToVirtual(String folder, String name, BufferedImage image) {
        folder = !folder.endsWith("/") ? folder : folder.substring(0, folder.length() - 1);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            addOutputFiles(new VirtualFile(folder, name, new ByteArrayInputStream(outputStream.toByteArray())));
        } catch (IOException e) {
            Logs.logError("Failed to write generated texture: " + folder + "/" + name);
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
        }
    }

    private void getAllFiles(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null)
            for (final File file : files) {
                if (file.isDirectory())
                    getAllFiles(file, fileList, newFolder, excluded);
                else if (!file.isDirectory() && !blacklist.contains(file.getName()))
                    readFileToVirtuals(fileList, file, newFolder);
            }
    }

    private void getFilesInFolder(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        if (files != null)
            for (final File file : files)
                if (!file.isDirectory() && !Arrays.asList(excluded).contains(file.getName()))
                    readFileToVirtuals(fileList, file, newFolder);
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

    private InputStream processJson(String content) {
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

    private void handleCustomArmor(List<VirtualFile> output) {
        CustomArmorType customArmorType = CustomArmorType.getSetting();

        switch (customArmorType) {
            case COMPONENT -> componentArmorModels.generatePackFiles(output);
            case TRIMS -> {
                if (trimArmorDatapack == null)
                    trimArmorDatapack = new TrimArmorDatapack();
                trimArmorDatapack.clearOldDataPack();
                trimArmorDatapack.generateAssets(output);
            }
            case SHADER -> {
                if (Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.toBool()
                        && shaderArmorTextures.hasCustomArmors()) {
                    try {
                        String armorPath = "assets/minecraft/textures/models/armor";
                        output.add(
                                new VirtualFile(armorPath, "leather_layer_1.png", shaderArmorTextures.getLayerOne()));
                        output.add(
                                new VirtualFile(armorPath, "leather_layer_2.png", shaderArmorTextures.getLayerTwo()));
                        if (Settings.CUSTOM_ARMOR_SHADER_GENERATE_SHADER_COMPATIBLE_ARMOR.toBool()) {
                            output.addAll(shaderArmorTextures.getOptifineFiles());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            default -> {
            } // Handle NONE
        }
    }

    private void mergeUploadedPacks(List<VirtualFile> output) {
        PackMerger packMerger = new PackMerger(packFolder);
        List<VirtualFile> mergedFiles = packMerger.mergeUploadedPacks();

        if (!mergedFiles.isEmpty()) {
            output.addAll(mergedFiles);
        }
    }

    private void convertGlobalLang(List<VirtualFile> output) {
        if (Settings.DEBUG.toBool()) Logs.logInfo("Converting global lang file to individual language files...");
        Set<VirtualFile> virtualLangFiles = new HashSet<>();
        File globalLangFile = new File(packFolder, "lang/global.json");
        JsonObject globalLang = new JsonObject();
        String content = "";
        if (!globalLangFile.exists())
            OraxenPlugin.get().saveResource("pack/lang/global.json", false);

        try {
            content = Files.readString(globalLangFile.toPath(), StandardCharsets.UTF_8);
            globalLang = JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException | IllegalStateException | IllegalArgumentException ignored) {
        }

        if (content.isEmpty() || globalLang.isJsonNull())
            return;

        for (String lang : availableLanguageCodes) {
            File langFile = new File(packFolder, "lang/" + lang + ".json");
            JsonObject langJson = new JsonObject();

            // If the file is in the pack, we want to keep the existing entries over global
            // ones
            if (langFile.exists()) {
                try {
                    langJson = JsonParser.parseString(Files.readString(langFile.toPath(), StandardCharsets.UTF_8))
                            .getAsJsonObject();
                } catch (IOException | IllegalStateException ignored) {
                }
            }

            for (Map.Entry<String, JsonElement> entry : globalLang.entrySet()) {
                if (entry.getKey().equals("DO_NOT_ALTER_THIS_LINE"))
                    continue;
                // If the entry already exists in the lang file, we don't want to overwrite it
                if (langJson.has(entry.getKey()))
                    continue;
                langJson.add(entry.getKey(), entry.getValue());
            }

            InputStream langStream = processJson(langJson.toString());
            virtualLangFiles.add(new VirtualFile("assets/minecraft/lang", lang + ".json", langStream));
        }
        // Remove previous langfiles as these have been migrated in above
        output.removeIf(
                virtualFile -> virtualLangFiles.stream().anyMatch(v -> v.getPath().equals(virtualFile.getPath())));
        output.addAll(virtualLangFiles);
    }

    private static final Set<String> availableLanguageCodes = new HashSet<>(Arrays.asList(
            "af_za", "ar_sa", "ast_es", "az_az", "ba_ru",
            "bar", "be_by", "bg_bg", "br_fr", "brb", "bs_ba", "ca_es", "cs_cz",
            "cy_gb", "da_dk", "de_at", "de_ch", "de_de", "el_gr", "en_au", "en_ca",
            "en_gb", "en_nz", "en_pt", "en_ud", "en_us", "enp", "enws", "eo_uy",
            "es_ar", "es_cl", "es_ec", "es_es", "es_mx", "es_uy", "es_ve", "esan",
            "et_ee", "eu_es", "fa_ir", "fi_fi", "fil_ph", "fo_fo", "fr_ca", "fr_fr",
            "fra_de", "fur_it", "fy_nl", "ga_ie", "gd_gb", "gl_es", "haw_us", "he_il",
            "hi_in", "hr_hr", "hu_hu", "hy_am", "id_id", "ig_ng", "io_en", "is_is",
            "isv", "it_it", "ja_jp", "jbo_en", "ka_ge", "kk_kz", "kn_in", "ko_kr",
            "ksh", "kw_gb", "la_la", "lb_lu", "li_li", "lmo", "lol_us", "lt_lt",
            "lv_lv", "lzh", "mk_mk", "mn_mn", "ms_my", "mt_mt", "nah", "nds_de",
            "nl_be", "nl_nl", "nn_no", "no_no", "oc_fr", "ovd", "pl_pl", "pt_br",
            "pt_pt", "qya_aa", "ro_ro", "rpr", "ru_ru", "ry_ua", "se_no", "sk_sk",
            "sl_si", "so_so", "sq_al", "sr_sp", "sv_se", "sxu", "szl", "ta_in",
            "th_th", "tl_ph", "tlh_aa", "tok", "tr_tr", "tt_ru", "uk_ua", "val_es",
            "vec_it", "vi_vn", "yi_de", "yo_ng", "zh_cn", "zh_hk", "zh_tw", "zlm_arab"));

    private void hideScoreboardNumbers() {
        if (OraxenPlugin.get().getPacketAdapter().isEnabled() && VersionUtil.isPaperServer()
                && VersionUtil.atOrAbove("1.20.3")) {
            OraxenPlugin.get().getPacketAdapter().registerScoreboardListener();
        } else { // Pre 1.20.3 rely on shaders
            // Check if animation shaders were already generated - need to combine them
            if (animationShadersGenerated) {
                // Use combined shaders that support both animation and scoreboard hiding
                String shaderVersion = getShaderVersion();
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh",
                        getCombinedVertexShader(shaderVersion));
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json",
                        getCombinedShaderJson(shaderVersion));
                // Fragment shader stays the same (animation-only, scoreboard uses vertex
                // shader)
                Logs.logInfo("Using combined animation + scoreboard hiding shaders");
            } else {
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json", getScoreboardJson());
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh", getScoreboardVsh());
            }
        }
    }

    private void hideScoreboardOrTablistBackgrounds() {
        String fileName = VersionUtil.atOrAbove("1.20.1") ? "rendertype_gui.vsh" : "position_color.fsh";
        String scoreTabBackground = "";
        if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool() || Settings.HIDE_TABLIST_BACKGROUND.toBool())
            scoreTabBackground = getScoreboardBackground();
        if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool())
            scoreTabBackground = scoreTabBackground.replaceFirst("//SCOREBOARD.a", "vertexColor.a");
        if (Settings.HIDE_TABLIST_BACKGROUND.toBool() && VersionUtil.atOrAbove("1.21"))
            scoreTabBackground = scoreTabBackground.replace("//TABLIST.a", "vertexColor.a");

        if (!scoreTabBackground.isEmpty())
            writeStringToVirtual("assets/minecraft/shaders/core/", fileName, scoreTabBackground);
    }

    private String getScoreboardVsh() {
        return """
                #version 150

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;

                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;

                uniform vec2 ScreenSize;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;

                void main() {
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    texCoord0 = UV0;

                	// delete sidebar numbers
                	if(	Position.z == 0.0 && // check if the depth is correct (0 for gui texts)
                			gl_Position.x >= 0.95 && gl_Position.y >= -0.35 && // check if the position matches the sidebar
                			vertexColor.g == 84.0/255.0 && vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 && // check if the color is the sidebar red color
                			gl_VertexID <= 4 // check if it's the first character of a string
                		) gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0); // move the vertices offscreen, idk if this is a good solution for that but vec4(0.0) doesnt do the trick for everyone
                }
                """;
    }

    private String getScoreboardJson() {
        return """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_text",
                    "fragment": "rendertype_text",
                    "attributes": [
                        "Position",
                        "Color",
                        "UV0",
                        "UV2"
                    ],
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                		{ "name": "ScreenSize", "type": "float", "count": 2,  "values": [ 1.0, 1.0 ] }
                    ]
                }
                """;
    }

    private String getScoreboardBackground() {
        if (VersionUtil.atOrAbove("1.21"))
            return """
                    #version 150

                     in vec3 Position;
                     in vec4 Color;

                     uniform mat4 ModelViewMat;
                     uniform mat4 ProjMat;

                     out vec4 vertexColor;

                     void main() {
                     	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                     	vertexColor = Color;

                     	//Isolating Scoreboard Display
                     	// Mojang Changed the Z position in 1.21, idk exact value but its huge
                     	if(gl_Position.y > -0.5 && gl_Position.y < 0.85 && gl_Position.x > 0.0 && gl_Position.x <= 1.0 && Position.z > 1000.0 && Position.z < 2750.0) {
                     		//vertexColor = vec4(vec3(0.0,0.0,1.0),1.0); // Debugger
                     		//SCOREBOARD.a = 0.0;
                     	}
                     	else {
                         	//vertexColor = vec4(vec3(1.0,0.0,0.0),1.0);
                     	}

                     	// Uncomment this if you want to make LIST invisible
                     	if(Position.z > 2750.0 && Position.z < 3000.0) {
                     		//TABLIST.a = 0.0;
                     	}
                     }

                    """;
        else if (VersionUtil.atOrAbove("1.21"))
            return """
                    #version 150

                    in vec3 Position;
                    in vec4 Color;

                    uniform mat4 ModelViewMat;
                    uniform mat4 ProjMat;

                    out vec4 vertexColor;

                    void main() {
                    	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                    	vertexColor = Color;

                    	//Isolating Scoreboard Display
                    	if(gl_Position.y > -0.5 && gl_Position.y < 0.85 && gl_Position.x > 0.0 && gl_Position.x <= 1.0 && Position.z == 0.0) {
                    		//vertexColor = vec4(vec3(0.0,0.0,1.0),1.0); // Debugger
                    		vertexColor.a = 0.0;
                    	}
                    	else {
                        	//vertexColor = vec4(vec3(1.0,0.0,0.0),1.0);
                    	}
                    }
                    """;
        else
            return """
                    #version 150

                    in vec4 vertexColor;

                    uniform vec4 ColorModulator;

                    out vec4 fragColor;

                    bool isgray(vec4 a) {
                        return a.r == 0 && a.g == 0 && a.b == 0 && a.a < 0.3 && a.a > 0.29;
                    }

                    bool isdarkgray(vec4 a) {
                    	return a.r == 0 && a.g == 0 && a.b == 0 && a.a == 0.4;
                    }

                    void main() {

                        vec4 color = vertexColor;

                        if (color.a == 0.0) {
                            discard;
                        }

                        fragColor = color * ColorModulator;

                    	if(isgray(fragColor)){
                    		discard;
                    	}
                    	if(isdarkgray(fragColor)){
                    		discard;
                    	}
                    }
                    // Made by Reytz#9806 for minecraft 1.18.2""";
    }
}
