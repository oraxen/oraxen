package io.th0rgal.oraxen.pack.generation;

import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.AppearanceMode;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.AnimatedGlyph;
import io.th0rgal.oraxen.font.EffectFontProvider;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.ShiftProvider;
import io.th0rgal.oraxen.font.TextEffect;
import net.kyori.adventure.key.Key;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.customarmor.ComponentArmorModels;
import io.th0rgal.oraxen.utils.customarmor.CustomArmorType;
import io.th0rgal.oraxen.utils.customarmor.ShaderArmorTextures;
import io.th0rgal.oraxen.utils.customarmor.TrimArmorDatapack;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;

public class ResourcePack {

    private final Map<String, Collection<Consumer<File>>> packModifiers;
    private static Map<String, VirtualFile> outputFiles;
    private ShaderArmorTextures shaderArmorTextures;
    private TrimArmorDatapack trimArmorDatapack;
    private ComponentArmorModels componentArmorModels;
    private static final File packFolder = new File(OraxenPlugin.get().getDataFolder(), "pack");
    private static final Set<String> LEGACY_SHADER_OVERLAY_DIRECTORIES = Set.of("overlay_1_21_6_plus", "overlay_1_21_4_5");
    private final File pack = new File(packFolder, packFolder.getName() + ".zip");
    private final SoundGenerator soundGenerator = new SoundGenerator();
    private PackFileCollector fileCollector;
    private final TextShaderGenerator textShaderGenerator = new TextShaderGenerator();
    /** Resolved multi-version flag (may differ from Settings if fallback occurred). */
    private boolean multiVersionResolved = false;
    private final AtomicBoolean generationInProgress = new AtomicBoolean(false);

    public ResourcePack() {
        // we use maps to avoid duplicate
        packModifiers = new HashMap<>();
        outputFiles = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public void generate() {
        if (!generationInProgress.compareAndSet(false, true)) {
            Logs.logWarning("Resource-pack generation is already in progress, skipping duplicate request");
            return;
        }

        // Re-evaluate dispatch mode normalization on each generation (covers reload)
        io.th0rgal.oraxen.pack.dispatch.PackSender.resetDispatchNormalization();

        boolean multiVersionEnabled = Settings.MULTI_VERSION_PACKS.toBool();
        boolean isSelfHost = Settings.UPLOAD_TYPE.toString().equalsIgnoreCase("self-host");

        try {
            if (multiVersionEnabled) {
                if (isSelfHost) {
                    Logs.logError("Multi-version packs are incompatible with self-host upload!");
                    Logs.logError("SelfHost can only serve one file at /pack.zip");
                    Logs.logError("Falling back to single-pack mode for this generation");
                    multiVersionEnabled = false;
                } else if (!Settings.UPLOAD.toBool()) {
                    Logs.logWarning("Multi-version packs require upload to be enabled!");
                    Logs.logWarning("Falling back to single-pack mode for this generation");
                    multiVersionEnabled = false;
                }
            }

            this.multiVersionResolved = multiVersionEnabled;

            if (multiVersionEnabled) {
                // Detect mode-switch BEFORE nulling the old manager, so generateMultiVersion
                // knows this is a reload even though both managers will be null by then.
                boolean switchingFromSinglePack = OraxenPlugin.get().getUploadManager() != null;

                // Unregister and clear single-pack manager if switching from single-pack mode.
                // Clearing the reference prevents getPackURL()/getPackSHA1() from returning
                // stale data from the old mode's manager.
                UploadManager oldUploadManager = OraxenPlugin.get().getUploadManager();
                if (oldUploadManager != null) {
                    oldUploadManager.unregister();
                    OraxenPlugin.get().setUploadManager(null);
                }

                generateMultiVersion(switchingFromSinglePack);
                finishGeneration();
                return;
            }

            // Unregister and clear multi-version manager if switching from multi-version mode.
            // Without clearing, OraxenPlugin.getPackURL()/getPackSHA1() would still check
            // the stale multiVersionUploadManager first and return wrong pack data.
            // setMultiVersionUploadManager(null) internally calls unregister() on the
            // old manager, so a separate unregister() call is not needed.
            OraxenPlugin.get().setMultiVersionUploadManager(null);

            if (!prepareGenerationPreamble()) {
                finishGeneration();
                return;
            }

            startSinglePackGenerationAsync();
        } catch (RuntimeException exception) {
            finishGeneration();
            throw exception;
        }
    }

    private void startSinglePackGenerationAsync() {
        ExecutorService packWorker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Oraxen-PackWorker");
            thread.setDaemon(true);
            return thread;
        });

        try {
            packWorker.submit(() -> {
                try {
                    generateAsyncSafeItemAssets();
                    SchedulerUtil.runTask(() -> continueSinglePackGenerationOnMain(packWorker));
                } catch (Exception exception) {
                    handleGenerationFailure(packWorker, "async item asset generation", exception);
                }
            });
        } catch (RuntimeException exception) {
            packWorker.shutdown();
            finishGeneration();
            throw exception;
        }
    }

    private void continueSinglePackGenerationOnMain(ExecutorService packWorker) {
        try {
            generateMiscAssets();
            applyPackModifiers();

            List<VirtualFile> output = new ArrayList<>(outputFiles.values());
            submitAsyncPackCollection(packWorker, output);
        } catch (Exception exception) {
            handleGenerationFailure(packWorker, "sync pack generation", exception);
        }
    }

    private void submitAsyncPackCollection(ExecutorService packWorker, List<VirtualFile> output) {
        try {
            packWorker.submit(() -> {
                try {
                    collectPackFilesAsyncSafe(output);
                    SchedulerUtil.runTask(() -> continuePostCollectionOnMain(packWorker, output));
                } catch (Exception exception) {
                    handleGenerationFailure(packWorker, "async pack collection", exception);
                }
            });
        } catch (RuntimeException exception) {
            handleGenerationFailure(packWorker, "async pack collection scheduling", exception);
        }
    }

    private void continuePostCollectionOnMain(ExecutorService packWorker, List<VirtualFile> output) {
        try {
            handleCustomArmor(output);
            applyArmorStandModelOverrides(output);
            Collections.sort(output);
            submitAsyncPostProcessing(packWorker, output);
        } catch (Exception exception) {
            handleGenerationFailure(packWorker, "sync custom armor generation", exception);
        }
    }

    private void submitAsyncPostProcessing(ExecutorService packWorker, List<VirtualFile> output) {
        try {
            packWorker.submit(() -> {
                try {
                    postProcessOutputAsyncSafe(output);
                    SchedulerUtil.runTask(() -> finishSinglePackOutputOnMain(packWorker, output));
                } catch (Exception exception) {
                    handleGenerationFailure(packWorker, "async pack post-processing", exception);
                }
            });
        } catch (RuntimeException exception) {
            handleGenerationFailure(packWorker, "async pack post-processing scheduling", exception);
        }
    }

    private void finishSinglePackOutputOnMain(ExecutorService packWorker, List<VirtualFile> output) {
        try {
            soundGenerator.generateSound(output);

            OraxenPackGeneratedEvent event = new OraxenPackGeneratedEvent(output);
            EventUtils.callEvent(event);
            output = event.getOutput();
            PackObfuscator.obfuscate(output);
            writeSinglePackAsync(packWorker, output);
        } catch (Exception exception) {
            handleGenerationFailure(packWorker, "sync pack finalization", exception);
        }
    }

    private void writeSinglePackAsync(ExecutorService packWorker, List<VirtualFile> output) {
        try {
            packWorker.submit(() -> {
                try {
                    ZipUtils.writeZipFile(pack, output);
                    SchedulerUtil.runTask(this::uploadGeneratedPackAndFinish);
                } catch (Exception exception) {
                    handleGenerationFailure(packWorker, "async pack writing", exception);
                } finally {
                    packWorker.shutdown();
                }
            });
        } catch (RuntimeException exception) {
            handleGenerationFailure(packWorker, "async pack writing scheduling", exception);
        }
    }

    private void uploadGeneratedPackAndFinish() {
        try {
            uploadGeneratedPack();
        } finally {
            finishGeneration();
        }
    }

    private void uploadGeneratedPack() {
        UploadManager uploadManager = OraxenPlugin.get().getUploadManager();
        if (uploadManager != null) { // If the uploadManager isnt null, this was triggered by a pack-reload
            uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), true, true);
        } else { // Otherwise this is was triggered on server-startup
            uploadManager = new UploadManager(OraxenPlugin.get());
            OraxenPlugin.get().setUploadManager(uploadManager);
            uploadManager.uploadAsyncAndSendToPlayers(OraxenPlugin.get().getResourcePack(), false, false);
        }
    }

    private void finishGeneration() {
        generationInProgress.set(false);
    }

    private void handleGenerationFailure(ExecutorService packWorker, String phase, Exception exception) {
        Logs.logError("Failed during " + phase);
        exception.printStackTrace();
        packWorker.shutdown();
        // Reset directly — do not rely on scheduler which may be unavailable during shutdown
        finishGeneration();
    }

    /**
     * Prepares the environment and generates all base pack assets.
     * This is shared logic between single-pack and multi-version generation.
     *
     * @return List of generated VirtualFiles ready for zipping
     */
    private boolean prepareGenerationPreamble() {
        // Reset state
        outputFiles.clear();
        textShaderGenerator.reset();

        makeDirsIfNotExists(packFolder, new File(packFolder, "assets"));
        cleanLegacyShaderOverlayDirectories();

        componentArmorModels = CustomArmorType.getSetting() == CustomArmorType.COMPONENT ? new ComponentArmorModels()
                : null;
        trimArmorDatapack = CustomArmorType.getSetting() == CustomArmorType.TRIMS ? new TrimArmorDatapack() : null;
        shaderArmorTextures = CustomArmorType.getSetting() == CustomArmorType.SHADER ? new ShaderArmorTextures() : null;
        fileCollector = new PackFileCollector(packFolder, shaderArmorTextures);

        if (Settings.GENERATE_DEFAULT_ASSETS.toBool())
            extractDefaultFolders();
        extractRequired();

        if (!Settings.GENERATE.toBool())
            return false;

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

        return true;
    }

    private void cleanLegacyShaderOverlayDirectories() {
        for (String directory : LEGACY_SHADER_OVERLAY_DIRECTORIES) {
            File legacyOverlayDirectory = new File(packFolder, directory);
            if (!legacyOverlayDirectory.isDirectory()) continue;

            try {
                FileUtils.deleteDirectory(legacyOverlayDirectory);
                if (Settings.DEBUG.toBool()) {
                    Logs.logInfo("Removed legacy shader overlay directory: " + directory);
                }
            } catch (IOException e) {
                Logs.logWarning("Failed to remove legacy shader overlay directory " + directory + ": " + e.getMessage());
            }
        }
    }

    /**
     * Prepares the environment and generates all base pack assets.
     * This is shared logic between single-pack and multi-version generation.
     *
     * @return List of generated VirtualFiles ready for zipping
     */
    private List<VirtualFile> prepareAndGenerateBaseAssets() {
        if (!prepareGenerationPreamble()) {
            return new ArrayList<>();
        }

        generateAsyncSafeItemAssets();
        return generateBaseAssets();
    }

    /**
     * Generates all base pack assets (items, fonts, shaders, blocks, etc.).
     * This is the core generation logic shared between single-pack and multi-version.
     *
     * @return List of generated VirtualFiles
     */
    private List<VirtualFile> generateBaseAssets() {
        generateMiscAssets();
        applyPackModifiers();

        List<VirtualFile> output = new ArrayList<>(outputFiles.values());
        collectPackFiles(output);
        applyArmorStandModelOverrides(output);
        postProcessOutput(output);

        return output;
    }

    private void applyArmorStandModelOverrides(List<VirtualFile> output) {
        Map<String, org.bukkit.util.Vector> scaleByModelPath = new LinkedHashMap<>();

        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            ItemBuilder item = entry.getValue();
            if (!item.hasOraxenMeta()) continue;

            OraxenMeta meta = item.getOraxenMeta();
            if (!meta.hasPackInfos() || !meta.hasArmorStandHeadScale()) continue;

            String modelFilePath = meta.getModelPath() + "/" + meta.getModelName() + ".json";
            org.bukkit.util.Vector newScale = meta.getArmorStandHeadScale();
            org.bukkit.util.Vector existingScale = scaleByModelPath.get(modelFilePath);

            if (existingScale != null && !sameScale(existingScale, newScale)) {
                Logs.logWarning("Multiple armor-stand furniture items target the same model with different scale values: " + modelFilePath);
                Logs.logWarning("Using the latest configured scale from item <gold>" + entry.getKey(), true);
            }

            scaleByModelPath.put(modelFilePath, newScale);
        }

        if (scaleByModelPath.isEmpty()) return;

        for (VirtualFile virtualFile : output) {
            org.bukkit.util.Vector scale = scaleByModelPath.get(virtualFile.getPath());
            if (scale == null) continue;

            JsonObject modelJson = virtualFile.toJsonObject();
            if (modelJson == null) continue;

            ModelGenerator.applyHeadScale(modelJson, scale);
            InputStream previousStream = virtualFile.getInputStream();
            if (previousStream != null) {
                try {
                    previousStream.close();
                } catch (IOException ignored) {
                }
            }
            virtualFile.setInputStream(new ByteArrayInputStream(modelJson.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private boolean sameScale(org.bukkit.util.Vector first, org.bukkit.util.Vector second) {
        return Double.compare(first.getX(), second.getX()) == 0
                && Double.compare(first.getY(), second.getY()) == 0
                && Double.compare(first.getZ(), second.getZ()) == 0;
    }

    private void generateAsyncSafeItemAssets() {
        final Map<Material, Map<String, ItemBuilder>> texturedItems = extractTexturedItems();
        generateItemAppearanceAssets(texturedItems);
    }

    /**
     * Generates item appearance assets (predicates, item definitions, model definitions)
     * based on the server version and appearance mode configuration.
     */
    private void generateItemAppearanceAssets(Map<Material, Map<String, ItemBuilder>> texturedItems) {
        final boolean is1_21_4Plus = VersionUtil.atOrAbove("1.21.4");

        if (is1_21_4Plus) {
            AppearanceMode.validateAndLogWarnings();

            if (AppearanceMode.isItemPropertiesEnabled()) {
                generateModelDefinitions(filterForItemModel(texturedItems));
            }

            if (AppearanceMode.shouldGenerateVanillaItemDefinitions()) {
                boolean useSelect = AppearanceMode.shouldUseSelectForVanillaItemDefs();
                boolean includeBothModes = AppearanceMode.shouldUseBothDispatchModes();
                generateVanillaItemDefinitions(filterForPredicates(texturedItems), useSelect, includeBothModes);
            }

            // Multi-version mode ALWAYS needs predicates because older target clients (1.20-1.21.3)
            // cannot use item definitions and rely solely on legacy predicate model overrides.
            // Uses the resolved flag (not the raw setting) to respect single-pack fallback.
            if (AppearanceMode.shouldGenerateLegacyPredicates() || multiVersionResolved) {
                generatePredicates(filterForPredicates(texturedItems));
            }
        } else {
            generatePredicates(filterForPredicates(texturedItems));
        }
    }

    /**
     * Generates non-item assets: fonts, shaders, scoreboard tweaks, armor, texture slicing.
     */
    private void generateMiscAssets() {
        generateFont();
        updatePackMcmetaOverlays();
        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool())
            textShaderGenerator.hideScoreboardNumbers();
        textShaderGenerator.hideScoreboardOrTablistBackgrounds();
        if (Settings.TEXTURE_SLICER.toBool())
            PackSlicer.slicePackFiles();
        if (CustomArmorType.getSetting() == CustomArmorType.SHADER
                && Settings.CUSTOM_ARMOR_SHADER_GENERATE_FILES.toBool())
            ShaderArmorTextures.generateArmorShaderFiles();
    }

    /** Runs registered pack modifier callbacks. */
    private void applyPackModifiers() {
        for (final Collection<Consumer<File>> modifiers : packModifiers.values())
            for (Consumer<File> modifier : modifiers)
                modifier.accept(packFolder);
    }

    /**
     * Collects all pack files from the pack folder into the output list,
     * merges uploaded packs, converts lang files, and handles custom armor.
     */
    private void collectPackFiles(List<VirtualFile> output) {
        try {
            collectPackFilesAsyncSafe(output);
            handleCustomArmor(output);
            Collections.sort(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void collectPackFilesAsyncSafe(List<VirtualFile> output) throws IOException {
        fileCollector.getFilesInFolder(packFolder, output, packFolder.getCanonicalPath(), packFolder.getName() + ".zip");

        File[] files = packFolder.listFiles();
        if (files != null)
            for (final File folder : files) {
                if (!folder.isDirectory()) continue;
                if (folder.getName().equals("uploads") || folder.getName().equals("__MACOSX")) continue;
                fileCollector.getAllFiles(folder, output,
                        folder.getName().matches("models|textures|lang|font|sounds") ? "assets/minecraft" : "");
            }

        mergeUploadedPacks(output);
        convertGlobalLang(output);
    }

    /**
     * Post-processes the output: verifies textures, generates atlases,
     * merges duplicates, filters excluded extensions, and generates sounds.
     */
    private void postProcessOutput(List<VirtualFile> output) {
        postProcessOutputAsyncSafe(output);
        soundGenerator.generateSound(output);
    }

    private void postProcessOutputAsyncSafe(List<VirtualFile> output) {
        Set<String> malformedTextures = new HashSet<>();
        if (Settings.VERIFY_PACK_FILES.toBool())
            malformedTextures = PackFileCollector.verifyPackFormatting(output);

        if (Settings.GENERATE_ATLAS_FILE.toBool())
            AtlasGenerator.generateAtlasFile(output, malformedTextures);

        if (Settings.MERGE_DUPLICATE_FONTS.toBool())
            DuplicationHandler.mergeFontFiles(output);
        if (Settings.MERGE_ITEM_MODELS.toBool())
            DuplicationHandler.mergeBaseItemFiles(output);
        DuplicationHandler.mergeVanillaItemDefinitions(output);

        List<String> excludedExtensions = Settings.EXCLUDED_FILE_EXTENSIONS.toStringList();
        excludedExtensions.removeIf(f -> f.equals("png") || f.equals("json"));
        if (!excludedExtensions.isEmpty() && !output.isEmpty()) {
            List<VirtualFile> excluded = new ArrayList<>();
            for (VirtualFile virtual : output)
                for (String extension : excludedExtensions)
                    if (virtual.getPath().endsWith(extension))
                        excluded.add(virtual);
            output.removeAll(excluded);
        }
    }

    /**
     * Generates multiple resource pack versions for different Minecraft client versions.
     * This method delegates to MultiVersionPackGenerator when multi_version_packs is enabled.
     *
     * @param switchingFromSinglePack true if we're switching from single-pack mode (treat as reload)
     */
    private void generateMultiVersion(boolean switchingFromSinglePack) {
        // Use shared generation logic
        List<VirtualFile> output = prepareAndGenerateBaseAssets();

        // Early exit if generation is disabled (empty output)
        if (output.isEmpty()) {
            return;
        }

        // Use MultiVersionPackGenerator for multi-version zip and upload
        MultiVersionPackGenerator multiVersionGenerator = new MultiVersionPackGenerator(packFolder);
        multiVersionGenerator.generateMultipleVersions(output, switchingFromSinglePack);
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

        PackMcmetaUtils.updatePackMcmetaFile(mcmetaPath);
    }

    /**
     * Updates pack.mcmeta to add shader overlay entries after shaders have been generated.
     * This must be called after {@link #generateFont()} to ensure overlay directories exist.
     */
    private void updatePackMcmetaOverlays() {
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

            boolean overlaysChanged = addShaderOverlayEntries(root);

            if (!overlaysChanged) {
                return;
            }

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
     * <p>Also expands the base pack's supported format range to cover all overlay
     * format ranges, so that clients of any supported version see the pack as
     * compatible.</p>
     */
    private boolean addShaderOverlayEntries(JsonObject root) {
        JsonObject overlays;
        if (root.has("overlays")) {
            overlays = root.getAsJsonObject("overlays");
        } else {
            overlays = new JsonObject();
        }

        JsonArray entries;
        if (overlays.has("entries")) {
            entries = overlays.getAsJsonArray("entries");
        } else {
            entries = new JsonArray();
        }

        int originalEntriesSize = entries.size();
        Set<String> knownDirectories = new HashSet<>(java.util.Arrays.stream(ShaderOverlay.values())
                .map(ShaderOverlay::directory)
                .collect(java.util.stream.Collectors.toSet()));
        knownDirectories.addAll(LEGACY_SHADER_OVERLAY_DIRECTORIES);
        JsonArray filteredEntries = new JsonArray();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                filteredEntries.add(element);
                continue;
            }

            JsonObject existingEntry = element.getAsJsonObject();
            String directory = existingEntry.has("directory") ? existingEntry.get("directory").getAsString() : null;
            if (directory == null || !knownDirectories.contains(directory)) {
                filteredEntries.add(existingEntry);
            }
        }
        entries = filteredEntries;

        int filteredEntriesSize = entries.size();

        for (ShaderOverlay overlay : textShaderGenerator.getGeneratedOverlays()) {
            JsonObject entry = new JsonObject();
            JsonObject formats = new JsonObject();
            formats.addProperty("min_inclusive", overlay.minFormat());
            formats.addProperty("max_inclusive", overlay.maxFormat());
            entry.add("formats", formats);
            entry.addProperty("directory", overlay.directory());
            if (overlay.minFormat() > 64 || overlay.maxFormat() > 64) {
                entry.addProperty("min_format", overlay.minFormat());
                entry.addProperty("max_format", overlay.maxFormat());
            }
            entries.add(entry);
        }

        int newEntriesAdded = entries.size() - filteredEntriesSize;
        boolean entriesRemoved = filteredEntriesSize != originalEntriesSize;
        boolean entriesChanged = newEntriesAdded > 0 || entriesRemoved;
        if (!entriesChanged) {
            return false;
        }

        overlays.add("entries", entries);
        root.add("overlays", overlays);

        expandSupportedFormatsRange(root);

        if (Settings.DEBUG.toBool()) {
            if (newEntriesAdded > 0 && entriesRemoved) {
                Logs.logInfo("Refreshed shader overlay entries in pack.mcmeta");
            } else if (newEntriesAdded > 0) {
                Logs.logInfo("Added " + newEntriesAdded + " shader overlay entries to pack.mcmeta");
            } else {
                Logs.logInfo("Removed stale shader overlay entries from pack.mcmeta");
            }
        }

        return true;
    }

    /**
     * Expands the base pack's supported_formats / min_format / max_format range
     * to cover all overlay format ranges, ensuring that clients of any supported
     * version see the pack as compatible.
     *
     * <p>For servers on 1.21.9+ (format 65+), also adds the legacy
     * {@code supported_formats} field so that pre-1.21.9 clients can read the
     * format range (they don't understand {@code min_format}/{@code max_format}).</p>
     */
    private void expandSupportedFormatsRange(JsonObject root) {
        List<ShaderOverlay> overlayList = textShaderGenerator.getGeneratedOverlays();
        if (overlayList.isEmpty()) return;

        int minOverlayFormat = overlayList.stream()
                .mapToInt(ShaderOverlay::minFormat).min().orElse(Integer.MAX_VALUE);
        int maxOverlayFormat = overlayList.stream()
                .mapToInt(ShaderOverlay::maxFormat).max().orElse(0);

        JsonObject pack = root.has("pack") && root.get("pack").isJsonObject()
                ? root.getAsJsonObject("pack") : new JsonObject();

        int serverFormat = ResourcePackFormatUtil.getCurrentResourcePackFormat();
        ShaderOverlay serverOverlay = ShaderOverlay.forPackFormat(serverFormat);
        int serverGroupMax = serverOverlay != null ? serverOverlay.maxFormat() : serverFormat;
        int effectiveMin = Math.min(serverFormat, minOverlayFormat);
        int effectiveMax = Math.max(serverGroupMax, maxOverlayFormat);

        if (serverFormat >= 65) {
            pack.addProperty("min_format", effectiveMin);
            pack.addProperty("max_format", effectiveMax);
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", effectiveMin);
            supportedFormats.addProperty("max_inclusive", effectiveMax);
            pack.add("supported_formats", supportedFormats);
        } else if (serverFormat >= 18) {
            JsonObject supportedFormats = new JsonObject();
            supportedFormats.addProperty("min_inclusive", effectiveMin);
            supportedFormats.addProperty("max_inclusive", effectiveMax);
            pack.add("supported_formats", supportedFormats);
            pack.remove("min_format");
            pack.remove("max_format");
        }

        root.add("pack", pack);
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
                final Map<String, ItemBuilder> items = texturedItems.computeIfAbsent(item.getType(),
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

                texturedItems.put(item.getType(), newItems);
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

        boolean serverIs1214Plus = VersionUtil.atOrAbove("1.21.4");
        if (!serverIs1214Plus && !multiVersionResolved) {
            // MultiVersionPacks disabled + Server is 1.21.3 or lower
            // -> No TextEffects and animated Glyphs should be generated (no Shaders)
            Logs.logInfo("Skipping text effects and animated glyphs generation (server is 1.21.3 or lower without multi-version packs)");
        } else {
            // Generate effect fonts for text effects
            generateEffectFonts();

            // Process animated glyph fonts
            boolean hasAnimatedGlyphs = processAnimatedGlyphs(fontManager);

            // Generate text shaders when needed (animated glyphs and/or text effects).
            if (!serverIs1214Plus && multiVersionResolved) {
                // MultiVersionPacks enabled + Server is 1.21.3 or lower
                // -> Shaders only generated for Packs that are above 1.21.4+
                textShaderGenerator.maybeGenerateTextShaders(hasAnimatedGlyphs, true, TextShaderTarget.PACK_FORMAT_1_21_4);
            } else {
                textShaderGenerator.maybeGenerateTextShaders(hasAnimatedGlyphs);
            }
        }
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


    public static void writeStringToVirtual(String folder, String name, String content) {
        folder = !folder.endsWith("/") ? folder : folder.substring(0, folder.length() - 1);
        addOutputFiles(
                new VirtualFile(folder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    }

    public static void deleteFileFromVirtualAndDisk(String folder, String name) {
        folder = !folder.endsWith("/") ? folder : folder.substring(0, folder.length() - 1);
        String virtualPath = folder.isEmpty() ? name : folder + "/" + name;
        outputFiles.remove(virtualPath);

        File file = new File(packFolder, virtualPath);
        if (file.exists()) {
            file.delete();
            if (Settings.DEBUG.toBool()) {
                Logs.logInfo("Deleted stale file from disk: " + virtualPath);
            }
        }
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

            InputStream langStream = PackFileCollector.processJson(langJson.toString());
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

}
