package io.th0rgal.oraxen.pack.generation;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.gson.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.packets.ScoreboardPacketListener;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.pack.upload.UploadManager;
import io.th0rgal.oraxen.sound.CustomSound;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.*;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private static final File packFolder = new File(OraxenPlugin.get().getDataFolder(), "pack");
    private final File pack = new File(packFolder, packFolder.getName() + ".zip");

    public ResourcePack() {
        // we use maps to avoid duplicate
        packModifiers = new HashMap<>();
        outputFiles = new HashMap<>();
    }

    public void generate() {
        outputFiles.clear();

        makeDirsIfNotExists(packFolder, new File(packFolder, "assets"));

        trimArmorDatapack = CustomArmorType.getSetting() == CustomArmorType.TRIMS ? new TrimArmorDatapack() : null;
        shaderArmorTextures = CustomArmorType.getSetting() == CustomArmorType.SHADER ? new ShaderArmorTextures() : null;

        if (Settings.GENERATE_DEFAULT_ASSETS.toBool()) extractDefaultFolders();
        extractRequired();

        if (!Settings.GENERATE.toBool()) return;

        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool() && PluginUtils.isEnabled("HappyHUD")) {
            Logs.logError("HappyHUD detected with hide_scoreboard_numbers enabled!");
            Logs.logWarning("Recommend following this guide for compatibility: https://docs.oraxen.com/compatibility/happyhud");
        }

        try {
            Files.deleteIfExists(pack.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        extractInPackIfNotExists(new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(new File(packFolder, "pack.png"));

        // Sorting items to keep only one with models (and generate it if needed)
        generatePredicates(extractTexturedItems());
        generateFont();
        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool()) hideScoreboardNumbers();
        hideScoreboardOrTablistBackgrounds();
        if (Settings.TEXTURE_SLICER.toBool()) PackSlicer.slicePackFiles();
        if (CustomArmorType.getSetting() == CustomArmorType.SHADER && Settings.CUSTOM_ARMOR_SHADER_GENERATE_FILES.toBool())
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
            if (files != null) for (final File folder : files) {
                if (!folder.isDirectory()) continue;
                getAllFiles(folder, output, folder.getName().matches("models|textures|lang|font|sounds") ? "assets/minecraft" : "");
            }

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

        List<String> excludedExtensions = Settings.EXCLUDED_FILE_EXTENSIONS.toStringList();
        excludedExtensions.removeIf(f -> f.equals("png") || f.equals("json"));
        if (!excludedExtensions.isEmpty() && !output.isEmpty()) {
            List<VirtualFile> newOutput = new ArrayList<>();
            for (VirtualFile virtual : output)
                for (String extension : excludedExtensions)
                    if (virtual.getPath().endsWith(extension)) newOutput.add(virtual);
            output.removeAll(newOutput);
        }

        generateSound(output);

        Bukkit.getScheduler().scheduleSyncDelayedTask(OraxenPlugin.get(), () -> {
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

    private static Set<String> verifyPackFormatting(List<VirtualFile> output) {
        Logs.logInfo("Verifying formatting for textures and models...");
        Set<VirtualFile> textures = new HashSet<>();
        Set<String> texturePaths = new HashSet<>();
        Set<String> mcmeta = new HashSet<>();
        Set<VirtualFile> models = new HashSet<>();
        Set<VirtualFile> malformedTextures = new HashSet<>();
        Set<VirtualFile> malformedModels = new HashSet<>();
        for (VirtualFile virtualFile : output) {
            String path = virtualFile.getPath();
            if (path.matches("assets/.*/models/.*.json")) models.add(virtualFile);
            else if (path.matches("assets/.*/textures/.*.png.mcmeta")) mcmeta.add(path);
            else if (path.matches("assets/.*/textures/.*.png")) {
                textures.add(virtualFile);
                texturePaths.add(path);
            }
        }

        if (models.isEmpty() && !textures.isEmpty()) return Collections.emptySet();

        for (VirtualFile model : models) {
            if (!model.getPath().matches("[a-z0-9/._-]+")) {
                Logs.logWarning("Found invalid model at <blue>" + model.getPath());
                Logs.logError("Model-paths must only contain characters [a-z0-9/._-]");
                malformedModels.add(model);
            }

            String content;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream inputStream = model.getInputStream();
            try {
                inputStream.transferTo(baos);
                content = baos.toString(StandardCharsets.UTF_8);
                baos.close();
                inputStream.reset();
                inputStream.close();
            } catch (IOException e) {
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
                    for (JsonElement element : jsonModel.getAsJsonObject("textures").entrySet().stream().map(Map.Entry::getValue).toList()) {
                        String jsonTexture = element.getAsString();
                        if (!texturePaths.contains(modelPathToPackPath(jsonTexture))) {
                            if (!jsonTexture.startsWith("#") && !jsonTexture.startsWith("item/") && !jsonTexture.startsWith("block/") && !jsonTexture.startsWith("entity/")) {
                                if (Material.matchMaterial(Utils.getFileNameOnly(jsonTexture).toUpperCase()) == null) {
                                    Logs.logWarning("Found invalid texture-path inside model-file <blue>" + model.getPath() + "</blue>: " + jsonTexture);
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
                if (mcmeta.contains(texture.getPath() + ".mcmeta")) continue;
                BufferedImage image;
                InputStream inputStream = texture.getInputStream();
                try {
                    image = ImageIO.read(new File("fake_file.png"));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    inputStream.transferTo(baos);
                    ImageIO.write(image, "png", baos);
                    baos.close();
                    inputStream.reset();
                    inputStream.close();
                } catch (IOException e) {
                    continue;
                }

                if (image.getHeight() > 256 || image.getWidth() > 256) {
                    Logs.logWarning("Found invalid texture at <blue>" + texture.getPath());
                    Logs.logError("Resolution of textures cannot exceed 256x256");
                    malformedTextures.add(texture);
                }
            }
        }

        if (!malformedTextures.isEmpty() || !malformedModels.isEmpty()) {
            Logs.logError("Pack contains malformed texture(s) and/or model(s)");
            Logs.logError("These need to be fixed, otherwise the resourcepack will be broken", true);
        } else Logs.logSuccess("No broken models or textures were found in the resourcepack", true);

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
        if (name.equals("textures") && extractTextures) return true;
        if (name.equals("models") && extractModels) return true;
        if (name.equals("font") && extractFonts) return true;
        if (name.equals("optifine") && extractOptifine) return true;
        if (name.equals("lang") && extractLang) return true;
        if (name.equals("sounds") && extractSounds) return true;
        return name.equals("assets") && extractAssets;
    }

    private void extractRequired() {
        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (entry.getName().startsWith("pack/textures/models/armor/leather_layer_") || entry.getName().startsWith("pack/textures/required") || entry.getName().startsWith("pack/models/required")) {
                    OraxenPlugin.get().getResourceManager().extractFileIfTrue(entry, !OraxenPlugin.get().getDataFolder().toPath().resolve(entry.getName()).toFile().exists());
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

    private Map<Material, List<ItemBuilder>> extractTexturedItems() {
        final Map<Material, List<ItemBuilder>> texturedItems = new HashMap<>();
        for (final Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            final ItemBuilder item = entry.getValue();
            OraxenMeta oraxenMeta = item.getOraxenMeta();
            if (item.hasOraxenMeta() && oraxenMeta.hasPackInfos()) {
                if (oraxenMeta.shouldGenerateModel()) {
                    writeStringToVirtual(oraxenMeta.getModelPath(),
                            item.getOraxenMeta().getModelName() + ".json",
                            new ModelGenerator(oraxenMeta).getJson().toString());
                }
                final List<ItemBuilder> items = texturedItems.getOrDefault(item.build().getType(), new ArrayList<>());
                // todo: could be improved by using
                // items.get(i).getOraxenMeta().getCustomModelData() when
                // items.add(customModelData, item) with catch when not possible
                if (items.isEmpty())
                    items.add(item);
                else
                    // for some reason those breaks are needed to avoid some nasty "memory leak"
                    for (int i = 0; i < items.size(); i++) {
                        if (items.get(i).getOraxenMeta().getCustomModelData() > item.getOraxenMeta().getCustomModelData()) {
                            items.add(i, item);
                            break;
                        } else if (i == items.size() - 1) {
                            items.add(item);
                            break;
                        }
                    }
                texturedItems.put(item.build().getType(), items);
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
        if (!file.exists()) OraxenPlugin.get().saveResource("pack/" + file.getName(), true);
    }

    private void makeDirsIfNotExists(final File... folders) {
        for (final File folder : folders)
            if (!folder.exists()) folder.mkdirs();
    }

    private void generatePredicates(final Map<Material, List<ItemBuilder>> texturedItems) {
        for (final Map.Entry<Material, List<ItemBuilder>> texturedItemsEntry : texturedItems.entrySet()) {
            final Material entryMaterial = texturedItemsEntry.getKey();
            final PredicatesGenerator predicatesGenerator = new PredicatesGenerator(entryMaterial,
                    texturedItemsEntry.getValue());
            final String[] vanillaModelPath =
                    (predicatesGenerator.getVanillaModelName(entryMaterial) + ".json").split("/");
            writeStringToVirtual("assets/minecraft/models/" + vanillaModelPath[0], vanillaModelPath[1],
                    predicatesGenerator.toJSON().toString());
        }
    }

    private void generateFont() {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        if (!fontManager.autoGenerate) return;
        final JsonObject output = new JsonObject();
        final JsonArray providers = new JsonArray();
        for (final Glyph glyph : fontManager.getGlyphs()) {
            if (!glyph.hasBitmap()) providers.add(glyph.toJson());
        }
        for (FontManager.GlyphBitMap glyphBitMap : FontManager.glyphBitMaps.values()) {
            providers.add(glyphBitMap.toJson(fontManager));
        }
        for (final Font font : fontManager.getFonts()) {
            providers.add(font.toJson());
        }
        output.add("providers", providers);
        writeStringToVirtual("assets/minecraft/font", "default.json", output.toString());
        if (Settings.FIX_FORCE_UNICODE_GLYPHS.toBool())
            writeStringToVirtual("assets/minecraft/font", "uniform.json", output.toString());
    }

    private void generateSound(List<VirtualFile> output) {
        SoundManager soundManager = OraxenPlugin.get().getSoundManager();
        if (!soundManager.isAutoGenerate()) return;

        List<VirtualFile> soundFiles = output.stream().filter(file -> file.getPath().equals("assets/minecraft/sounds.json")).toList();
        JsonObject outputJson = new JsonObject();

        // If file was imported by other means, we attempt to merge in sound.yml entries
        for (VirtualFile soundFile : soundFiles) {
            if (soundFile != null) {
                try {
                    JsonElement soundElement = JsonParser.parseString(IOUtils.toString(soundFile.getInputStream(), StandardCharsets.UTF_8));
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

        for (CustomSound sound : handleCustomSoundEntries(soundManager.getCustomSounds())) {
            outputJson.add(sound.getName(), sound.toJson());
        }

        InputStream soundInput = new ByteArrayInputStream(outputJson.toString().getBytes(StandardCharsets.UTF_8));
        output.add(new VirtualFile("assets/minecraft", "sounds.json", soundInput));
        try {
            soundInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Collection<CustomSound> handleCustomSoundEntries(Collection<CustomSound> sounds) {
        ConfigurationSection mechanic = OraxenPlugin.get().getConfigsManager().getMechanics();
        ConfigurationSection customSounds = mechanic.getConfigurationSection("custom_block_sounds");
        ConfigurationSection noteblock = mechanic.getConfigurationSection("noteblock");
        ConfigurationSection stringblock = mechanic.getConfigurationSection("stringblock");
        ConfigurationSection furniture = mechanic.getConfigurationSection("furniture");
        ConfigurationSection block = mechanic.getConfigurationSection("block");

        if (customSounds == null) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
        } else {
            if (!customSounds.getBoolean("noteblock_and_block", true)) {
                sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
            }
            if (!customSounds.getBoolean("stringblock_and_furniture", true)) {
                sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
            }
            if ((noteblock != null && !noteblock.getBoolean("enabled", true) && block != null && !block.getBoolean("enabled", false))) {
                sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
            }
            if (stringblock != null && !stringblock.getBoolean("enabled", true) && furniture != null && !furniture.getBoolean("enabled", true)) {
                sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
            }
        }

        // Clear the sounds.json file of yaml configuration entries that should not be there
        sounds.removeIf(s ->
                s.getName().equals("required") ||
                        s.getName().equals("block") ||
                        s.getName().equals("block.wood") ||
                        s.getName().equals("block.stone") ||
                        s.getName().equals("required.wood") ||
                        s.getName().equals("required.stone")
        );

        return sounds;
    }

    public static void writeStringToVirtual(String folder, String name, String content) {
        folder = !folder.endsWith("/") ? folder : folder.substring(0, folder.length() - 1);
        addOutputFiles(new VirtualFile(folder, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    }

    private void getAllFiles(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        final List<String> blacklist = Arrays.asList(excluded);
        if (files != null) for (final File file : files) {
            if (file.isDirectory()) getAllFiles(file, fileList, newFolder, excluded);
            else if (!file.isDirectory() && !blacklist.contains(file.getName()))
                readFileToVirtuals(fileList, file, newFolder);
        }
    }

    private void getFilesInFolder(File dir, Collection<VirtualFile> fileList, String newFolder, String... excluded) {
        final File[] files = dir.listFiles();
        if (files != null) for (final File file : files)
            if (!file.isDirectory() && !Arrays.asList(excluded).contains(file.getName()))
                readFileToVirtuals(fileList, file, newFolder);
    }

    private void readFileToVirtuals(final Collection<VirtualFile> output, File file, String newFolder) {
        try {
            final InputStream fis;
            if (file.getName().endsWith(".json")) fis = processJsonFile(file);
            else if (CustomArmorType.getSetting() == CustomArmorType.SHADER && shaderArmorTextures.registerImage(file))
                return;
            else fis = new FileInputStream(file);

            output.add(new VirtualFile(getZipFilePath(file.getParentFile().getCanonicalPath(), newFolder), file.getName(), fis));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream processJsonFile(File file) throws IOException {
        InputStream newStream;
        String content;
        if (!file.exists()) return new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
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
        if (newFolder.equals(packFolder.getCanonicalPath())) return "";
        String prefix = newFolder.isEmpty() ? newFolder : newFolder + "/";
        return prefix + path.substring(packFolder.getCanonicalPath().length() + 1);
    }

    private void handleCustomArmor(List<VirtualFile> output) {
        CustomArmorType customArmorType = CustomArmorType.getSetting();
        // Clear out old datapacks before generating new ones, in case type changed or otherwise
        TrimArmorDatapack.clearOldDataPacks();

        switch (customArmorType) {
            case TRIMS -> trimArmorDatapack.generateTrimAssets(output);
            case SHADER -> {
                if (Settings.CUSTOM_ARMOR_SHADER_GENERATE_CUSTOM_TEXTURES.toBool() && shaderArmorTextures.hasCustomArmors())
                    try {
                        String armorPath = "assets/minecraft/textures/models/armor";
                        output.add(new VirtualFile(armorPath, "leather_layer_1.png", shaderArmorTextures.getLayerOne()));
                        output.add(new VirtualFile(armorPath, "leather_layer_2.png", shaderArmorTextures.getLayerTwo()));
                        if (Settings.CUSTOM_ARMOR_SHADER_GENERATE_SHADER_COMPATIBLE_ARMOR.toBool()) {
                            output.addAll(shaderArmorTextures.getOptifineFiles());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        if (VersionUtil.isPaperServer()) {
            Bukkit.getDatapackManager().getPacks().stream().filter(d -> d.getName().equals(TrimArmorDatapack.datapackKey.value()))
                    .findFirst().ifPresent(d -> d.setEnabled(CustomArmorType.getSetting() == CustomArmorType.TRIMS));
        }
    }

    private void convertGlobalLang(List<VirtualFile> output) {
        Logs.logWarning("Converting global lang file to individual language files...");
        Set<VirtualFile> virtualLangFiles = new HashSet<>();
        File globalLangFile = new File(packFolder, "lang/global.json");
        JsonObject globalLang = new JsonObject();
        String content = "";
        if (!globalLangFile.exists()) OraxenPlugin.get().saveResource("pack/lang/global.json", false);

        try {
            content = Files.readString(globalLangFile.toPath(), StandardCharsets.UTF_8);
            globalLang = JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException | IllegalStateException | IllegalArgumentException ignored) {
        }

        if (content.isEmpty() || globalLang.isJsonNull()) return;

        for (String lang : availableLanguageCodes) {
            File langFile = new File(packFolder, "lang/" + lang + ".json");
            JsonObject langJson = new JsonObject();

            // If the file is in the pack, we want to keep the existing entries over global ones
            if (langFile.exists()) {
                try {
                    langJson = JsonParser.parseString(Files.readString(langFile.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
                } catch (IOException | IllegalStateException ignored) {
                }
            }

            for (Map.Entry<String, JsonElement> entry : globalLang.entrySet()) {
                if (entry.getKey().equals("DO_NOT_ALTER_THIS_LINE")) continue;
                // If the entry already exists in the lang file, we don't want to overwrite it
                if (langJson.has(entry.getKey())) continue;
                langJson.add(entry.getKey(), entry.getValue());
            }

            InputStream langStream = processJson(langJson.toString());
            virtualLangFiles.add(new VirtualFile("assets/minecraft/lang", lang + ".json", langStream));
        }
        // Remove previous langfiles as these have been migrated in above
        output.removeIf(virtualFile -> virtualLangFiles.stream().anyMatch(v -> v.getPath().equals(virtualFile.getPath())));
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
        if (PluginUtils.isEnabled("ProtocolLib") && VersionUtil.isPaperServer() && VersionUtil.atOrAbove("1.20.3")) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new ScoreboardPacketListener());
        } else { // Pre 1.20.3 rely on shaders
            writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json", getScoreboardJson());
            writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh", getScoreboardVsh());
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

        if (!scoreTabBackground.isEmpty()) writeStringToVirtual("assets/minecraft/shaders/core/", fileName, scoreTabBackground);
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
        else return """
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
