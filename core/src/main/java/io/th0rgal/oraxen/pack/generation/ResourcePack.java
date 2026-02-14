package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.AppearanceMode;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.AnimatedGlyph;
import io.th0rgal.oraxen.font.EffectFontEncoding;
import io.th0rgal.oraxen.font.EffectFontProvider;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.ShiftProvider;
import io.th0rgal.oraxen.font.TextEffect;
import io.th0rgal.oraxen.font.TextEffectEncoding;
import net.kyori.adventure.key.Key;
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
import org.jetbrains.annotations.Nullable;

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

public class ResourcePack {

    private final Map<String, Collection<Consumer<File>>> packModifiers;
    private static Map<String, VirtualFile> outputFiles;
    private ShaderArmorTextures shaderArmorTextures;
    private TrimArmorDatapack trimArmorDatapack;
    private ComponentArmorModels componentArmorModels;
    private static final File packFolder = new File(OraxenPlugin.get().getDataFolder(), "pack");
    private final File pack = new File(packFolder, packFolder.getName() + ".zip");

    /**
     * Tracks whether text shaders were generated (for combining with scoreboard shaders).
     */
    private boolean textShadersGenerated = false;
    private TextShaderFeatures textShaderFeatures = null;
    private TextEffectSnippets textEffectSnippets = null;
    private TextShaderTarget textEffectSnippetsTarget = null;

    public ResourcePack() {
        // we use maps to avoid duplicate
        packModifiers = new HashMap<>();
        outputFiles = new HashMap<>();
    }

    public void generate() {
        // Check if multi-version pack generation is enabled
        boolean multiVersionEnabled = Settings.MULTI_VERSION_PACKS.toBool();
        boolean isSelfHost = Settings.UPLOAD_TYPE.toString().equalsIgnoreCase("self-host");

        // SelfHost is incompatible with multi-version (can only serve one file)
        if (multiVersionEnabled && isSelfHost) {
            Logs.logError("Multi-version packs are incompatible with self-host upload!");
            Logs.logError("SelfHost can only serve one file at /pack.zip");
            Logs.logError("Falling back to single-pack mode for this generation");
            multiVersionEnabled = false; // Force fallback to single-pack
        }

        if (multiVersionEnabled) {
            // Unregister single-pack listener if switching from single-pack mode
            UploadManager oldUploadManager = OraxenPlugin.get().getUploadManager();
            if (oldUploadManager != null) {
                oldUploadManager.unregister();
            }

            generateMultiVersion();
            return;
        }

        // Unregister multi-version listener if switching from multi-version mode
        io.th0rgal.oraxen.pack.upload.MultiVersionUploadManager oldMultiVersionManager = OraxenPlugin.get().getMultiVersionUploadManager();
        if (oldMultiVersionManager != null) {
            oldMultiVersionManager.unregister();
        }

        // Legacy single-pack generation - prepare and generate base assets
        List<VirtualFile> output = prepareAndGenerateBaseAssets();

        // Early exit if generation is disabled (empty output)
        if (output.isEmpty()) {
            return;
        }

        // Zip and upload the single pack
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
     * Prepares the environment and generates all base pack assets.
     * This is shared logic between single-pack and multi-version generation.
     *
     * @return List of generated VirtualFiles ready for zipping
     */
    private List<VirtualFile> prepareAndGenerateBaseAssets() {
        // Reset state
        outputFiles.clear();
        textShadersGenerated = false;
        textShaderFeatures = null;
        textEffectSnippets = null;
        textEffectSnippetsTarget = null;
        shaderOverlaysGenerated = false;

        makeDirsIfNotExists(packFolder, new File(packFolder, "assets"));

        componentArmorModels = CustomArmorType.getSetting() == CustomArmorType.COMPONENT ? new ComponentArmorModels()
                : null;
        trimArmorDatapack = CustomArmorType.getSetting() == CustomArmorType.TRIMS ? new TrimArmorDatapack() : null;
        shaderArmorTextures = CustomArmorType.getSetting() == CustomArmorType.SHADER ? new ShaderArmorTextures() : null;

        if (Settings.GENERATE_DEFAULT_ASSETS.toBool())
            extractDefaultFolders();
        extractRequired();

        if (!Settings.GENERATE.toBool())
            return new ArrayList<>();

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

        // Generate all base assets
        return generateBaseAssets();
    }

    /**
     * Generates all base pack assets (items, fonts, shaders, blocks, etc.).
     * This is the core generation logic shared between single-pack and multi-version.
     *
     * @return List of generated VirtualFiles
     */
    private List<VirtualFile> generateBaseAssets() {

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
        // Update pack.mcmeta with shader overlays after shaders are generated
        updatePackMcmetaOverlays();
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
                    // Skip macOS metadata directories
                    if (folder.getName().equals("__MACOSX"))
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

        return output;
    }

    /**
     * Generates multiple resource pack versions for different Minecraft client versions.
     * This method delegates to MultiVersionPackGenerator when multi_version_packs is enabled.
     */
    private void generateMultiVersion() {
        // Use shared generation logic
        List<VirtualFile> output = prepareAndGenerateBaseAssets();

        // Early exit if generation is disabled (empty output)
        if (output.isEmpty()) {
            return;
        }

        // Use MultiVersionPackGenerator for multi-version zip and upload
        MultiVersionPackGenerator multiVersionGenerator = new MultiVersionPackGenerator(packFolder);
        multiVersionGenerator.generateMultipleVersions(output);
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
     *
     * <p>
     * Also adds overlay entries for cross-version shader compatibility when the server
     * is running 1.21.4+ and text effects or animated glyphs may be used.
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

            // Add supported_formats for broader client compatibility
            // This tells clients the range of pack formats this pack supports
            MinecraftVersion currentVersion = MinecraftVersion.getCurrentVersion();
            if (currentVersion.isAtLeast(new MinecraftVersion("1.21"))) {
                JsonObject supportedFormats = new JsonObject();
                // Support wide range of 1.21.x pack formats (34 for 1.21, up to 999 for future versions)
                // This ensures compatibility with 1.21, 1.21.2, 1.21.4, 1.21.11, etc.
                supportedFormats.addProperty("min_inclusive", 34);
                // Use a high max to support future versions (Minecraft ignores unknown higher formats)
                supportedFormats.addProperty("max_inclusive", 999);
                pack.add("supported_formats", supportedFormats);
            } else if (currentVersion.isAtLeast(new MinecraftVersion("1.20.2"))) {
                // For 1.20.2+ versions, support formats 18-33
                // supported_formats was introduced in MC 1.20.2 (pack format 18)
                JsonObject supportedFormats = new JsonObject();
                supportedFormats.addProperty("min_inclusive", 18);
                supportedFormats.addProperty("max_inclusive", 33);
                pack.add("supported_formats", supportedFormats);
            }

            root.add("pack", pack);

            // Use Gson with pretty printing for readable output
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(mcmetaPath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
            Logs.logWarning("Failed to update pack.mcmeta pack_format. Keeping existing file.");
        }
    }

    /**
     * Updates pack.mcmeta to add shader overlay entries after shaders have been generated.
     * This must be called after {@link #generateFont()} to ensure overlay directories exist.
     */
    private void updatePackMcmetaOverlays() {
        if (!shaderOverlaysGenerated) {
            return;
        }

        Path mcmetaPath = packFolder.toPath().resolve("pack.mcmeta");
        if (!mcmetaPath.toFile().exists()) {
            return;
        }

        try {
            String content = Files.readString(mcmetaPath, StandardCharsets.UTF_8);
            JsonObject root;
            try {
                root = JsonParser.parseString(content).getAsJsonObject();
            } catch (Exception ignored) {
                Logs.logWarning("Failed to parse pack.mcmeta for overlay update");
                return;
            }

            MinecraftVersion currentVersion = MinecraftVersion.getCurrentVersion();
            if (currentVersion.isAtLeast(new MinecraftVersion("1.21.4"))) {
                addShaderOverlayEntries(root, currentVersion);
            }

            // Use Gson with pretty printing for readable output
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(mcmetaPath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (Settings.DEBUG.toBool())
                e.printStackTrace();
            Logs.logWarning("Failed to update pack.mcmeta with shader overlays. Keeping existing file.");
        }
    }

    /**
     * Adds overlay entries to pack.mcmeta for cross-version shader compatibility.
     *
     * <p>Overlays allow the pack to include different shader formats for different
     * Minecraft client versions. The client automatically selects the appropriate
     * overlay based on its pack_format.</p>
     *
     * @param root The root pack.mcmeta JSON object
     * @param serverVersion The server's Minecraft version
     */
    private void addShaderOverlayEntries(JsonObject root, MinecraftVersion serverVersion) {
        // Get existing overlays or create new one
        JsonObject overlays;
        if (root.has("overlays")) {
            overlays = root.getAsJsonObject("overlays");
        } else {
            overlays = new JsonObject();
        }
        
        // Get existing entries array or create new one
        JsonArray entries;
        if (overlays.has("entries")) {
            entries = overlays.getAsJsonArray("entries");
        } else {
            entries = new JsonArray();
        }

        int initialSize = entries.size();

        if (serverVersion.isAtLeast(new MinecraftVersion("1.21.6"))) {
            // Server is 1.21.6+, base shaders are 1.21.6 format
            // Add overlay for 1.21.4/1.21.5 clients (pack_format 46-54)
            JsonObject entry = new JsonObject();
            JsonObject formats = new JsonObject();
            formats.addProperty("min_inclusive", TextShaderTarget.PACK_FORMAT_1_21_4);
            formats.addProperty("max_inclusive", TextShaderTarget.PACK_FORMAT_1_21_6 - 1);
            entry.add("formats", formats);
            entry.addProperty("directory", OVERLAY_1_21_4_5);
            entries.add(entry);
        } else if (serverVersion.isAtLeast(new MinecraftVersion("1.21.4"))) {
            // Server is 1.21.4/1.21.5, base shaders are 1.21.4 format
            // Add overlay for 1.21.6+ clients (pack_format 55+)
            JsonObject entry = new JsonObject();
            JsonObject formats = new JsonObject();
            formats.addProperty("min_inclusive", TextShaderTarget.PACK_FORMAT_1_21_6);
            formats.addProperty("max_inclusive", 999); // Support future versions
            entry.add("formats", formats);
            entry.addProperty("directory", OVERLAY_1_21_6_PLUS);
            entries.add(entry);
        }

        int newEntriesAdded = entries.size() - initialSize;
        if (newEntriesAdded > 0) {
            overlays.add("entries", entries);
            root.add("overlays", overlays);
            if (Settings.DEBUG.toBool()) {
                Logs.logInfo("Added " + newEntriesAdded + " shader overlay entries to pack.mcmeta");
            }
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
        ResourcesManager.browseJar(entry ->
            extract(entry, OraxenPlugin.get().getResourceManager(), isSuitable(entry.getName()))
        );
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
        ResourcesManager.browseJar(entry -> {
            if (entry.getName().startsWith("pack/textures/models/armor/leather_layer_")
                    || entry.getName().startsWith("pack/textures/required")
                    || entry.getName().startsWith("pack/models/required")) {
                OraxenPlugin.get().getResourceManager().extractFileIfTrue(entry,
                        !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists());
            }
        });
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

        // Generate effect fonts for text effects
        generateEffectFonts();

        // Process animated glyph fonts
        boolean hasAnimatedGlyphs = processAnimatedGlyphs(fontManager);

        // Generate text shaders when needed (animated glyphs and/or text effects).
        maybeGenerateTextShaders(hasAnimatedGlyphs);
    }

    /**
     * Generates effect font files for text effects.
     * Each enabled effect gets a dedicated font that references vanilla glyphs.
     * <p>
     * This method now supports unlimited effects (up to 256) by iterating over
     * all enabled effects rather than a fixed array.
     */
    private void generateEffectFonts() {
        if (!TextEffect.isEnabled() || !TextEffect.hasAnyEffectEnabled()) {
            return;
        }

        EffectFontProvider provider = new EffectFontProvider();

        // Generate fonts for all enabled effects (supports unlimited effects)
        for (TextEffect.Definition def : TextEffect.getEnabledEffects()) {
            Key fontKey = EffectFontProvider.getFontKey(def.getId());
            JsonObject fontJson = provider.generateFontJson(def.getId());

            String path = "assets/" + fontKey.namespace() + "/font";
            String filename = fontKey.value() + ".json";
            writeStringToVirtual(path, filename, fontJson.toString());
        }
    }

    private record TextShaderFeatures(boolean animatedGlyphs, boolean textEffects) {
        boolean anyEnabled() {
            return animatedGlyphs || textEffects;
        }
    }

    private record TextEffectSnippets(String vertexPrelude, String fragmentPrelude,
                                      String vertexEffects, String fragmentEffects) {
    }

    private void maybeGenerateTextShaders(boolean hasAnimatedGlyphs) {
        if (textShadersGenerated) return;

        TextShaderFeatures features = resolveTextShaderFeatures(hasAnimatedGlyphs);
        if (!features.anyEnabled()) return;

        TextShaderTarget target = TextShaderTarget.current();
        generateTextShaders(target, features);
        textShaderFeatures = features;
        textShadersGenerated = true;
    }

    private TextShaderFeatures resolveTextShaderFeatures(boolean hasAnimatedGlyphs) {
        boolean textEffectsEnabled = TextEffect.isEnabled() && TextEffect.hasAnyEffectEnabled();
        TextEffect.ShaderTemplate template = TextEffect.getShaderTemplate();

        boolean includeAnimated;
        boolean includeEffects;

        switch (template) {
            case ANIMATED_GLYPHS -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = false;
            }
            case TEXT_EFFECTS -> {
                includeAnimated = false;
                includeEffects = textEffectsEnabled;
            }
            case AUTO -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = textEffectsEnabled;
            }
            default -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = textEffectsEnabled;
            }
        }

        if (hasAnimatedGlyphs && !includeAnimated) {
            Logs.logWarning("Animated glyphs detected but TextEffects.shader.template disables them.");
        }
        if (textEffectsEnabled && !includeEffects) {
            Logs.logWarning("Text effects are enabled but TextEffects.shader.template disables them.");
        }

        return new TextShaderFeatures(includeAnimated, includeEffects);
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
    private boolean processAnimatedGlyphs(FontManager fontManager) {
        Collection<AnimatedGlyph> animatedGlyphs = fontManager.getAnimatedGlyphs();
        if (animatedGlyphs.isEmpty()) {
            return false;
        }

        // Note: Codepoint counter is reset in ConfigsManager.parseAllGlyphConfigs()
        // BEFORE animated glyphs are created, ensuring clean codepoint allocation on
        // reload.

        Logs.logInfo("Processing " + animatedGlyphs.size() + " animated glyphs...");

        for (AnimatedGlyph animGlyph : animatedGlyphs) {
            processAnimatedGlyph(animGlyph);
        }
        return true;
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

            int frameCount = Math.max(1, animGlyph.getFrameCount());
            int sheetWidth = sheetImage.getWidth();
            int sheetHeight = sheetImage.getHeight();
            int frameWidthPx;
            int frameHeightPx;

            if (sheetWidth % frameCount == 0) {
                frameWidthPx = sheetWidth / frameCount;
                frameHeightPx = sheetHeight;
            } else if (sheetHeight % frameCount == 0) {
                frameWidthPx = sheetWidth;
                frameHeightPx = sheetHeight / frameCount;
            } else {
                frameWidthPx = Math.max(1, sheetWidth / frameCount);
                frameHeightPx = sheetHeight;
                Logs.logWarning("Sprite sheet '" + animGlyph.getName() + "' has non-divisible dimensions; " +
                        "reset advance may be approximate.");
            }

            animGlyph.setProcessed(spriteSheetPath, frameWidthPx, frameHeightPx);
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

    /** Directory name for 1.21.6+ shader overlay */
    private static final String OVERLAY_1_21_6_PLUS = "overlay_1_21_6_plus";

    /** Tracks whether overlay shaders were generated (for pack.mcmeta update) */
    private boolean shaderOverlaysGenerated = false;

    /**
     * Generates text shaders based on target and enabled features.
     * Different Minecraft versions use different shader formats.
     * Also generates overlay shaders for cross-version compatibility.
     */
    private void generateTextShaders(TextShaderTarget target, TextShaderFeatures features) {
        // Generate base shaders for the current server version
        generateTextShadersForTarget(target, features, "");

        // Generate overlay shaders for cross-version compatibility
        // If server is 1.21.4/1.21.5, also generate 1.21.6+ shaders in overlay directory
        if (target.isAtLeast("1.21.4") && !target.isAtLeast("1.21.6")) {
            TextShaderTarget overlayTarget = TextShaderTarget.forVersion("1.21.6");
            generateTextShadersForTarget(overlayTarget, features, OVERLAY_1_21_6_PLUS + "/");
            shaderOverlaysGenerated = true;
            Logs.logSuccess("Generated shader overlay for 1.21.6+ clients");
        }
        // If server is 1.21.6+, also generate 1.21.4/1.21.5 shaders in overlay directory
        else if (target.isAtLeast("1.21.6")) {
            TextShaderTarget overlayTarget = TextShaderTarget.forVersion("1.21.4");
            generateTextShadersForTarget(overlayTarget, features, OVERLAY_1_21_4_5 + "/");
            shaderOverlaysGenerated = true;
            Logs.logSuccess("Generated shader overlay for 1.21.4/1.21.5 clients");
        }
    }

    /** Directory name for 1.21.4/1.21.5 shader overlay */
    private static final String OVERLAY_1_21_4_5 = "overlay_1_21_4_5";

    /**
     * Generates text shaders for a specific target version into the given path prefix.
     *
     * @param target The target version for shader generation
     * @param features The shader features to include
     * @param pathPrefix Empty for base pack, or "overlay_xxx/" for overlay directories
     */
    private void generateTextShadersForTarget(TextShaderTarget target, TextShaderFeatures features, String pathPrefix) {
        // Generate shaders (see-through uses a different vertex format on 1.21.6+)
        String vshContent = getAnimationVertexShader(target, features, false);
        String fshContent = getAnimationFragmentShader(target, false);
        String jsonContent = getAnimationShaderJson(target, false);

        String vshSeeThrough = getAnimationVertexShader(target, features, true);
        String fshSeeThrough = getAnimationFragmentShader(target, true);
        String jsonSeeThrough = getAnimationShaderJson(target, true);

        String vshIntensity = getAnimationVertexShader(target, features, false);
        String fshIntensity = getAnimationFragmentShader(target, false, true);
        String jsonIntensity = getAnimationShaderJson(target, "rendertype_text_intensity", false);

        String vshIntensitySeeThrough = getAnimationVertexShader(target, features, true);
        String fshIntensitySeeThrough = getAnimationFragmentShader(target, true, true);
        String jsonIntensitySeeThrough = getAnimationShaderJson(target, "rendertype_text_intensity_see_through", true);

        String shaderPath = pathPrefix + "assets/minecraft/shaders/core";

        // Write shaders for both rendertype_text and rendertype_text_see_through
        writeStringToVirtual(shaderPath, "rendertype_text.vsh", vshContent);
        writeStringToVirtual(shaderPath, "rendertype_text.fsh", fshContent);
        writeStringToVirtual(shaderPath, "rendertype_text.json", jsonContent);

        writeStringToVirtual(shaderPath, "rendertype_text_see_through.vsh", vshSeeThrough);
        writeStringToVirtual(shaderPath, "rendertype_text_see_through.fsh", fshSeeThrough);
        writeStringToVirtual(shaderPath, "rendertype_text_see_through.json", jsonSeeThrough);

        writeStringToVirtual(shaderPath, "rendertype_text_intensity.vsh", vshIntensity);
        writeStringToVirtual(shaderPath, "rendertype_text_intensity.fsh", fshIntensity);
        writeStringToVirtual(shaderPath, "rendertype_text_intensity.json", jsonIntensity);

        writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.vsh", vshIntensitySeeThrough);
        writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.fsh", fshIntensitySeeThrough);
        writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.json", jsonIntensitySeeThrough);

        Logs.logSuccess("Generated text shaders for " + target.displayName()
                + " (shader " + getShaderVersion(target) + ")" + (pathPrefix.isEmpty() ? "" : " [overlay]"));
    }

    /**
     * Determines the shader version based on server version.
     */
    private String getShaderVersion(TextShaderTarget target) {
        if (target.isAtLeast("1.21.6")) {
            return "1.21.6";
        } else if (target.isAtLeast("1.21.4")) {
            return "1.21.4";
        } else if (target.isAtLeast("1.21")) {
            return "1.21";
        } else {
            return "1.20";
        }
    }

    private String getTextShaderConstants(TextShaderTarget target, TextShaderFeatures features) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, """
                const bool ORAXEN_ANIMATED_GLYPHS = %s;
                const bool ORAXEN_TEXT_EFFECTS = %s;
                """,
                features.animatedGlyphs() ? "true" : "false",
                features.textEffects() ? "true" : "false"));

        // Generate exact trigger colors only for effects that have valid snippets for this target
        // This ensures we don't recognize trigger colors for effects without shader code
        List<TextEffect.Definition> enabledEffects = TextEffect.getEnabledEffects().stream()
                .filter(def -> def.resolveSnippet(target.packFormat(), target.minecraftVersion()) != null)
                .toList();
        int effectCount = enabledEffects.size();
        sb.append(String.format(Locale.ROOT, "const int ORAXEN_EFFECT_COUNT = %d;\n", effectCount));

        // Always define arrays (GLSL requires all identifiers to exist even in unreachable branches)
        // Use at least size 1 to avoid zero-size array issues
        int arraySize = Math.max(1, effectCount);
        sb.append("const ivec3 ORAXEN_EFFECT_TRIGGERS[").append(arraySize).append("] = ivec3[](\n");
        if (enabledEffects.isEmpty()) {
            // Placeholder entry that will never match (ORAXEN_EFFECT_COUNT is 0)
            sb.append("    ivec3(0, 0, 0)\n");
        } else {
            for (int i = 0; i < enabledEffects.size(); i++) {
                TextEffect.Definition def = enabledEffects.get(i);
                int rgb = def.getTriggerColor().value();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                sb.append(String.format(Locale.ROOT, "    ivec3(%d, %d, %d)", r, g, b));
                if (i < enabledEffects.size() - 1) {
                    sb.append(",");
                }
                sb.append(" // ").append(def.getName()).append(" (id=").append(def.getId()).append(")\n");
            }
        }
        sb.append(");\n");

        // Also generate effect IDs array to map trigger index to effect type
        sb.append("const int ORAXEN_EFFECT_IDS[").append(arraySize).append("] = int[](\n");
        if (enabledEffects.isEmpty()) {
            sb.append("    0\n");
        } else {
            for (int i = 0; i < enabledEffects.size(); i++) {
                TextEffect.Definition def = enabledEffects.get(i);
                sb.append(String.format(Locale.ROOT, "    %d", def.getId()));
                if (i < enabledEffects.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        sb.append(");\n");

        return sb.toString();
    }

    private TextEffectSnippets getTextEffectSnippets(TextShaderTarget target) {
        if (textEffectSnippets != null && target.equals(textEffectSnippetsTarget)) {
            return textEffectSnippets;
        }
        textEffectSnippetsTarget = target;
        textEffectSnippets = buildTextEffectSnippets(target);
        return textEffectSnippets;
    }

    private TextEffectSnippets buildTextEffectSnippets(TextShaderTarget target) {
        if (!TextEffect.isEnabled() || !TextEffect.hasAnyEffectEnabled()) {
            return new TextEffectSnippets("", "", "", "");
        }

        StringBuilder vertexPrelude = new StringBuilder();
        StringBuilder fragmentPrelude = new StringBuilder();
        StringBuilder vertexEffects = new StringBuilder();
        StringBuilder fragmentEffects = new StringBuilder();

        appendPrelude(vertexPrelude, TextEffect.getSharedVertexPrelude());
        appendPrelude(fragmentPrelude, TextEffect.getSharedFragmentPrelude());

        boolean firstVertex = true;
        boolean firstFragment = true;

        for (TextEffect.Definition definition : TextEffect.getEnabledEffects()) {
            TextEffect.Snippet snippet = definition.resolveSnippet(target.packFormat(), target.minecraftVersion());
            if (snippet == null) {
                Logs.logWarning("No shader snippet for text effect '" + definition.getName()
                        + "' on target " + target.displayName());
                continue;
            }

            if (snippet.hasVertexPrelude()) {
                appendPrelude(vertexPrelude, snippet.vertexPrelude());
            }
            if (snippet.hasFragmentPrelude()) {
                appendPrelude(fragmentPrelude, snippet.fragmentPrelude());
            }

            if (snippet.hasVertex()) {
                appendEffectBlock(vertexEffects, definition, snippet.vertex(), firstVertex);
                firstVertex = false;
            }
            if (snippet.hasFragment()) {
                appendEffectBlock(fragmentEffects, definition, snippet.fragment(), firstFragment);
                firstFragment = false;
            }
        }

        return new TextEffectSnippets(vertexPrelude.toString(), fragmentPrelude.toString(),
                vertexEffects.toString(), fragmentEffects.toString());
    }

    private void appendPrelude(StringBuilder builder, @Nullable String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(snippet.stripTrailing());
    }

    /**
     * Appends an effect block that matches on effectType.
     * Both vertex and fragment shaders define effectType from the effect encoding.
     * No variables or placeholders - all values are hardcoded in the snippet.
     */
    private void appendEffectBlock(StringBuilder builder, TextEffect.Definition definition,
                                   String snippet, boolean first) {
        String effectIndent = "                            ";
        String codeIndent = effectIndent + "    ";

        int effectId = definition.getId();

        builder.append(effectIndent)
                .append("// ")
                .append(definition.getName())
                .append(" (id=")
                .append(effectId)
                .append(")\n");
        builder.append(effectIndent)
                .append(first ? "if" : "else if")
                .append(" (effectType == ")
                .append(effectId)
                .append(") {\n");
        builder.append(indentSnippet(snippet, codeIndent));
        builder.append("\n")
                .append(effectIndent)
                .append("}\n");
    }

    private String indentSnippet(String snippet, String indent) {
        String trimmed = snippet.stripTrailing();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] lines = trimmed.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                out.append(indent).append(line);
            }
            if (i < lines.length - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }

    /**
     * Configuration for version-specific shader code generation.
     * Holds the variable parts that differ between MC versions and seeThrough modes.
     */
    private record VertexShaderConfig(
            String fogDistanceInit,        // Initial fog distance calculation (empty for seeThrough)
            String fogDistanceRecalc,      // Fog recalculation after effects (empty for seeThrough)
            String vertexColorInit,        // Initial vertexColor assignment
            String vertexColorAnimated,    // vertexColor for animated glyphs
            String moduloExpr              // Modulo expression: "(rawFrame %% totalFrames)" or "int(mod(float(rawFrame), float(totalFrames)))"
    ) {}

    /**
     * Generates the vertex shader main() body logic. This is shared between all shader variants,
     * with version-specific parts injected via VertexShaderConfig.
     *
     * @param config Version-specific shader configuration
     * @param vertexEffects The vertex effect code snippet
     * @param includeScoreboardHiding Whether to include scoreboard number hiding code
     */
    private String getVertexShaderMainBody(VertexShaderConfig config, String vertexEffects, boolean includeScoreboardHiding) {
        String scoreboardHiding = includeScoreboardHiding ? """

                        // Scoreboard number hiding
                        if (Position.z == 0.0 &&
                                gl_Position.x >= 0.95 && gl_Position.y >= -0.35 &&
                                vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 &&
                                gl_VertexID <= 4) {
                            gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0);
                        }""" : "";

        return """
                    void main() {
                        vec3 pos = Position;
                        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
%s                        texCoord0 = UV0;
                        vertexColor = %s;
                        effectData = vec4(-1.0, 0.0, 0.0, 0.0); // -1 means no effect

                        int rInt = int(Color.r * 255.0 + 0.5);
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);

                        // Check for animation color: R=254 for primary, R≈63 for shadow
                        bool isPrimaryAnim = (rInt == 254);
                        bool isShadowAnim = (rInt >= 62 && rInt <= 64) && (gRaw >= 1) && (bRaw <= 64);

                        if (ORAXEN_ANIMATED_GLYPHS && (isPrimaryAnim || isShadowAnim)) {
                            int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                            int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                            bool loop = (gInt < 128);
                            float fps = max(1.0, float(gInt & 0x7F));
                            int frameIndex = bInt & 0x0F;
                            int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                            int rawFrame = int(floor(timeSeconds * fps));
                            int currentFrame = loop ? %s : min(rawFrame, totalFrames - 1);

                            float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                            if (isPrimaryAnim) {
                                vertexColor = %s;
                            } else {
                                vertexColor = vec4(0.0);
                            }
                        }

                        // Text effects: exact trigger color matching
                        if (ORAXEN_TEXT_EFFECTS && ORAXEN_EFFECT_COUNT > 0 && (!ORAXEN_ANIMATED_GLYPHS || (!isPrimaryAnim && !isShadowAnim))) {
                            // Check for exact trigger color match
                            ivec3 colorInt = ivec3(rInt, gRaw, bRaw);
                            int effectType = -1;
                            for (int i = 0; i < ORAXEN_EFFECT_COUNT; i++) {
                                if (colorInt == ORAXEN_EFFECT_TRIGGERS[i]) {
                                    effectType = ORAXEN_EFFECT_IDS[i];
                                    break;
                                }
                            }

                            if (effectType >= 0) {
                                float speed = 3.0; // Default speed (configured in shader snippets)
                                float param = 3.0; // Default param (configured in shader snippets)
                                float charIndex = float(gl_VertexID >> 2);

                                float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s

                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
%s
                                // Pass effect data to fragment shader
                                effectData = vec4(float(effectType), speed, charIndex, param);
                            }
                        }%s
                    }
                """.formatted(
                config.fogDistanceInit.isEmpty() ? "" : "                        " + config.fogDistanceInit + "\n",
                config.vertexColorInit,
                config.moduloExpr,
                config.vertexColorAnimated,
                vertexEffects,
                config.fogDistanceRecalc.isEmpty() ? "" : "                            " + config.fogDistanceRecalc + "\n",
                scoreboardHiding
        );
    }

    /**
     * Generates animation vertex shader using visibility-based animation.
     * Each frame is a separate character; the shader hides frames that don't match current time.
     * Also handles text effects encoded in RGB low bits (alpha_lsb) with position-based effects.
     *
     * Color encoding for animated glyphs:
     * - R = 254: animation marker
     * - G bits 0-6: FPS, bit 7: loop flag
     * - B bits 0-3: frame index, bits 4-7: total frames - 1
     *
     * Color encoding for text effects (alpha_lsb):
     * - Low 4 bits of each channel are reserved
     * - Low nibble values between DATA_MIN and DATA_MAX carry data (0-7), skipping DATA_GAP
     * - R -> effectType, G -> speed, B -> param
     * - charIndex is derived from gl_VertexID
     */
    private String getAnimationVertexShader(TextShaderTarget target, TextShaderFeatures features, boolean seeThrough) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String textShaderConstants = getTextShaderConstants(target, features);
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String vertexPrelude = snippets.vertexPrelude();
        String vertexEffects = snippets.vertexEffects();

        VertexShaderConfig config = createVertexShaderConfig(is1_21_6Plus, seeThrough);
        String mainBody = getVertexShaderMainBody(config, vertexEffects, false);

        if (is1_21_6Plus) {
            String header = seeThrough ? """
                #version 330

                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;

                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""" : """
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
                %s
                %s

""";
            return header.formatted(textShaderConstants, vertexPrelude) + mainBody;
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            // Note: 1.21.4/1.21.5 still use the old fog.glsl (non-namespaced) with fog_distance() and linear_fog()
            String imports = "#moj_import <fog.glsl>";
            String header = seeThrough ? """
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
                out vec4 effectData;
                %s
                %s

""" : """
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
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(imports, textShaderConstants, vertexPrelude) + mainBody;
        }
    }

    /**
     * Creates the version-specific shader configuration based on MC version and seeThrough mode.
     */
    private VertexShaderConfig createVertexShaderConfig(boolean is1_21_6Plus, boolean seeThrough) {
        if (is1_21_6Plus) {
            if (seeThrough) {
                return new VertexShaderConfig(
                        "",  // No fog init for seeThrough
                        "",  // No fog recalc for seeThrough
                        "Color",
                        "vec4(1.0, 1.0, 1.0, visible)",
                        "(rawFrame % totalFrames)"
                );
            } else {
                return new VertexShaderConfig(
                        "sphericalVertexDistance = fog_spherical_distance(pos);\n                        cylindricalVertexDistance = fog_cylindrical_distance(pos);",
                        "sphericalVertexDistance = fog_spherical_distance(pos);\n                            cylindricalVertexDistance = fog_cylindrical_distance(pos);",
                        "Color * texelFetch(Sampler2, UV2 / 16, 0)",
                        "vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0)",
                        "(rawFrame % totalFrames)"
                );
            }
        } else {
            // Pre-1.21.6
            if (seeThrough) {
                return new VertexShaderConfig(
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "Color",
                        "vec4(1.0, 1.0, 1.0, visible)",
                        "int(mod(float(rawFrame), float(totalFrames)))"
                );
            } else {
                return new VertexShaderConfig(
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "Color * texelFetch(Sampler2, UV2 / 16, 0)",
                        "vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0)",
                        "int(mod(float(rawFrame), float(totalFrames)))"
                );
            }
        }
    }

    /**
     * Generates a simple fragment shader - visibility is handled in vertex shader.
     */
    private String getAnimationFragmentShader(TextShaderTarget target, boolean seeThrough) {
        return getAnimationFragmentShader(target, seeThrough, false);
    }

    private String getAnimationFragmentShader(TextShaderTarget target, boolean seeThrough, boolean intensity) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String sampleExpr = intensity ? "texture(Sampler0, texCoord0).rrrr" : "texture(Sampler0, texCoord0)";
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String fragmentPrelude = snippets.fragmentPrelude();
        String fragmentEffects = snippets.fragmentEffects();

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
                        vec4 texColor = color;

                        // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                        if (effectData.x >= 0.0 && effectData.y > 0.5) {
                            int effectType = int(effectData.x + 0.5);
                            float speed = effectData.y;
                            float charIndex = effectData.z;
                            float param = effectData.w;
                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                        }

                        color = texColor;

                        if (color.a < 0.1) {
                            discard;
                        }
                        fragColor = color;
                    }
                    """.formatted(fragmentPrelude, sampleExpr, fragmentEffects);
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
                    vec4 texColor = color;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                    }

                    color = texColor;

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
                }
                """.formatted(fragmentPrelude, sampleExpr, fragmentEffects);
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = "#moj_import <fog.glsl>";

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
                    vec4 texColor = color;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                    }

                    color = texColor;

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """.formatted(imports, fragmentPrelude, sampleExpr, fragmentEffects);
        }
    }

    // Common uniform definitions for shader JSON
    private static final String UNIFORM_MATRIX = """
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] }""";
    private static final String UNIFORM_FOG = """
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                        { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] }""";
    private static final String UNIFORM_GAMETIME = """
                        { "name": "GameTime", "type": "float", "count": 1, "values": [ 0.0 ] }""";
    private static final String UNIFORM_SCREENSIZE = """
                        { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] }""";

    /**
     * Builds a 1.21+ shader JSON configuration (vanilla-style).
     *
     * <p>In 1.21.4 Mojang switched core shaders to reference their own sources via
     * fully-qualified ids like {@code minecraft:core/rendertype_text}. Prior versions
     * used bare names like {@code rendertype_text}. The client treats invalid shader
     * references as a fatal pack load error.
     */
    private String build1_21ShaderJson(String shaderName, boolean seeThrough, boolean fullyQualifiedCoreRefs,
                                       boolean includeFog, boolean includeGameTime, boolean includeScreenSize) {
        String shaderRef = fullyQualifiedCoreRefs ? "minecraft:core/" + shaderName : shaderName;
        String samplers = seeThrough
                ? "{ \"name\": \"Sampler0\" }"
                : "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }";

        StringBuilder uniforms = new StringBuilder(UNIFORM_MATRIX);
        if (includeFog) uniforms.append(",\n").append(UNIFORM_FOG);
        if (includeGameTime) uniforms.append(",\n").append(UNIFORM_GAMETIME);
        if (includeScreenSize) uniforms.append(",\n").append(UNIFORM_SCREENSIZE);

        return """
            {
                "vertex": "%s",
                "fragment": "%s",
                "samplers": [ %s ],
                "uniforms": [
%s
                ]
            }
            """.formatted(shaderRef, shaderRef, samplers, uniforms.toString());
    }

    /**
     * Builds a pre-1.21.6 shader JSON configuration.
     */
    private String buildLegacyShaderJson(String shaderName, boolean hasUV2, boolean hasSampler2,
            boolean hasFog, boolean hasGameTime, boolean hasScreenSize) {
        String attributes = hasUV2
                ? "\"Position\", \"Color\", \"UV0\", \"UV2\""
                : "\"Position\", \"Color\", \"UV0\"";
        String samplers = hasSampler2
                ? "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }"
                : "{ \"name\": \"Sampler0\" }";

        StringBuilder uniforms = new StringBuilder(UNIFORM_MATRIX);
        if (hasFog) uniforms.append(",\n").append(UNIFORM_FOG);
        if (hasGameTime) uniforms.append(",\n").append(UNIFORM_GAMETIME);
        if (hasScreenSize) uniforms.append(",\n").append(UNIFORM_SCREENSIZE);

        return """
            {
                "blend": {
                    "func": "add",
                    "srcrgb": "srcalpha",
                    "dstrgb": "1-srcalpha"
                },
                "vertex": "%s",
                "fragment": "%s",
                "attributes": [ %s ],
                "samplers": [ %s ],
                "uniforms": [
%s
                ]
            }
            """.formatted(shaderName, shaderName, attributes, samplers, uniforms.toString());
    }

    /**
     * Generates the shader JSON configuration.
     */
    private String getAnimationShaderJson(TextShaderTarget target, boolean seeThrough) {
        String shaderName = seeThrough ? "rendertype_text_see_through" : "rendertype_text";
        return getAnimationShaderJson(target, shaderName, seeThrough);
    }

    private String getAnimationShaderJson(TextShaderTarget target, String shaderName, boolean seeThrough) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        boolean is1_21_4Plus = target.isAtLeast("1.21.4");

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks - most uniforms come from imported glsl files
            String samplers = seeThrough
                    ? "{ \"name\": \"Sampler0\" }"
                    : "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }";
            return """
                {
                    "vertex": "minecraft:core/%s",
                    "fragment": "minecraft:core/%s",
                    "samplers": [ %s ]
                }
                """.formatted(shaderName, shaderName, samplers);
        } else if (is1_21_4Plus) {
            // 1.21.4/1.21.5: still #version 150 shaders with explicit uniforms,
            // but core shader sources must be referenced as minecraft:core/<name>
            // (otherwise the client looks for minecraft:<name> and fails to load the pack).
            boolean includeFog = !seeThrough;
            return build1_21ShaderJson(shaderName, seeThrough, true, includeFog, true, false);
        } else {
            // 1.21.0-1.21.3 (and older): bare shader refs still work.
            return buildLegacyShaderJson(shaderName, !seeThrough, !seeThrough, true, true, false);
        }
    }

    /**
     * Generates a combined vertex shader that supports both animation and
     * scoreboard number hiding.
     * Uses visibility-based animation: each frame is a separate character,
     * and the shader hides frames that don't match current time.
     */
    private String getCombinedVertexShader(TextShaderTarget target, TextShaderFeatures features) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String textShaderConstants = getTextShaderConstants(target, features);
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String vertexPrelude = snippets.vertexPrelude();
        String vertexEffects = snippets.vertexEffects();

        // Combined shader always uses non-seeThrough config (has Sampler2, fog) with scoreboard hiding
        VertexShaderConfig config = createVertexShaderConfig(is1_21_6Plus, false);
        String mainBody = getVertexShaderMainBody(config, vertexEffects, true);

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks from globals.glsl
            String header = """
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
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(textShaderConstants, vertexPrelude) + mainBody;
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = "#moj_import <fog.glsl>";
            String header = """
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
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(imports, textShaderConstants, vertexPrelude) + mainBody;
        }
    }

    /**
     * Generates combined shader JSON that includes uniforms for both animation and
     * scoreboard hiding.
     */
    private String getCombinedShaderJson(TextShaderTarget target) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");

        if (is1_21_6Plus) {
            // 1.21.6+ uses uniform blocks - most uniforms come from imported glsl files
            return """
                {
                    "vertex": "minecraft:core/rendertype_text",
                    "fragment": "minecraft:core/rendertype_text",
                    "samplers": [ { "name": "Sampler0" }, { "name": "Sampler2" } ],
                    "uniforms": [ { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] } ]
                }
                """;
        } else {
            return buildLegacyShaderJson("rendertype_text", true, true, true, true, true);
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

    /**
     * Generic handler for sound entries with a specific material type.
     *
     * @param sounds The sound collection to filter
     * @param customSounds The custom block sounds config section
     * @param soundPrefix The sound prefix to filter (e.g., "wood" or "stone")
     * @param configKey The config key to check in customSounds (e.g., "noteblock_and_block")
     * @param section1 First mechanic section to check
     * @param section1EnabledDefault Default enabled value for section1
     * @param section2 Second mechanic section to check
     * @param section2EnabledDefault Default enabled value for section2
     */
    private void handleSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            String soundPrefix,
            String configKey,
            ConfigurationSection section1,
            boolean section1EnabledDefault,
            ConfigurationSection section2,
            boolean section2EnabledDefault) {
        java.util.function.Predicate<CustomSound> soundFilter =
                s -> s.getName().startsWith("required." + soundPrefix) || s.getName().startsWith("block." + soundPrefix);

        if (customSounds == null) {
            sounds.removeIf(soundFilter);
            return;
        }

        if (!customSounds.getBoolean(configKey, true)) {
            sounds.removeIf(soundFilter);
        }

        if (section1 != null && !section1.getBoolean("enabled", section1EnabledDefault) &&
                section2 != null && !section2.getBoolean("enabled", section2EnabledDefault)) {
            sounds.removeIf(soundFilter);
        }
    }

    private void handleWoodSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection noteblock,
            ConfigurationSection block) {
        handleSoundEntries(sounds, customSounds, "wood", "noteblock_and_block", noteblock, true, block, false);
    }

    private void handleStoneSoundEntries(Collection<CustomSound> sounds,
            ConfigurationSection customSounds,
            ConfigurationSection stringblock,
            ConfigurationSection furniture) {
        handleSoundEntries(sounds, customSounds, "stone", "stringblock_and_furniture", stringblock, true, furniture, true);
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

    private static boolean shouldIgnorePackFile(File file) {
        String name = file.getName();
        if (".DS_Store".equals(name) || "Thumbs.db".equalsIgnoreCase(name) || "desktop.ini".equalsIgnoreCase(name))
            return true;

        if (file.isDirectory() && "__MACOSX".equals(name))
            return true;

        return false;
    }

    private void getAllFiles(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null)
            for (final File file : files) {
                if (shouldIgnorePackFile(file))
                    continue;
                if (file.isDirectory())
                    getAllFiles(file, fileList, newFolder, excluded);
                else if (!blacklist.contains(file.getName()))
                    readFileToVirtuals(fileList, file, newFolder);
            }
    }

    private void getFilesInFolder(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null)
            for (final File file : files)
                if (!file.isDirectory() && !blacklist.contains(file.getName()) && !shouldIgnorePackFile(file))
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
            // Check if text shaders were already generated - need to combine them
            if (textShadersGenerated) {
                // Use combined shaders that support both text features and scoreboard hiding
                TextShaderTarget target = TextShaderTarget.current();
                boolean hasAnimatedGlyphs = !OraxenPlugin.get().getFontManager().getAnimatedGlyphs().isEmpty();
                TextShaderFeatures features = textShaderFeatures != null
                        ? textShaderFeatures
                        : resolveTextShaderFeatures(hasAnimatedGlyphs);
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh",
                        getCombinedVertexShader(target, features));
                writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json",
                        getCombinedShaderJson(target));
                // Fragment shader stays the same (text shader uses vertex shader for scoreboard hiding)
                Logs.logInfo("Using combined text + scoreboard hiding shaders");
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
