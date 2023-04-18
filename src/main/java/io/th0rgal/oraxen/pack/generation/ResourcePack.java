package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.gestures.GestureManager;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.sound.CustomSound;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
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

    private static final String SHADER_PARAMETER_PLACEHOLDER = "{#TEXTURE_RESOLUTION#}";

    private Map<String, Collection<Consumer<File>>> packModifiers;
    private Map<String, VirtualFile> outputFiles;
    private CustomArmorsTextures customArmorsTextures;
    private File packFolder;
    private File pack;
    JavaPlugin plugin;

    public ResourcePack(final JavaPlugin plugin) {
        this.plugin = plugin;
        clear();
    }

    public void clear() {
        // we use maps to avoid duplicate
        packModifiers = new HashMap<>();
        outputFiles = new HashMap<>();
    }

    public void generate(final FontManager fontManager, final SoundManager soundManager) {
        outputFiles.clear();

        customArmorsTextures = new CustomArmorsTextures((int) Settings.ARMOR_RESOLUTION.getValue());
        packFolder = new File(plugin.getDataFolder(), "pack");
        makeDirsIfNotExists(packFolder);
        makeDirsIfNotExists(new File(packFolder, "assets"));
        pack = new File(packFolder, packFolder.getName() + ".zip");
        File assetsFolder = new File(packFolder, "assets");
        File modelsFolder = new File(packFolder, "models");
        File fontFolder = new File(packFolder, "font");
        File optifineFolder = new File(packFolder, "optifine");
        File langFolder = new File(packFolder, "lang");
        File textureFolder = new File(packFolder, "textures");
        File shaderFolder = new File(packFolder, "shaders");
        File soundFolder = new File(packFolder, "sounds");

        if (Settings.GENERATE_DEFAULT_ASSETS.toBool())
            extractFolders(!modelsFolder.exists(), !textureFolder.exists(),
                    !shaderFolder.exists(), !langFolder.exists(), !fontFolder.exists(),
                    !soundFolder.exists(), !assetsFolder.exists(), !optifineFolder.exists());
        else extractRequired();

        if (!Settings.GENERATE.toBool())
            return;

        if (Settings.HIDE_SCOREBOARD_NUMBERS.toBool() && Bukkit.getPluginManager().isPluginEnabled("HappyHUD")) {
            Logs.logError("HappyHUD detected with hide_scoreboard_numbers enabled!");
            Logs.logWarning("Recommend following this guide for compatibility: https://docs.oraxen.com/compatibility/happyhud");
        }

        try {
            Files.deleteIfExists(pack.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        extractInPackIfNotExists(plugin, new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(plugin, new File(packFolder, "pack.png"));

        // Sorting items to keep only one with models (and generate it if needed)
        generatePredicates(extractTexturedItems());
        generateFont(fontManager);
        generateSound(soundManager);
        if (Settings.GESTURES_ENABLED.toBool()) generateGestureFiles();

        for (final Collection<Consumer<File>> packModifiers : packModifiers.values())
            for (Consumer<File> packModifier : packModifiers)
                packModifier.accept(packFolder);
        List<VirtualFile> output = new ArrayList<>(outputFiles.values());
        // zipping resourcepack
        try {
            getFilesInFolder(packFolder, output,
                    packFolder.getCanonicalPath(),
                    packFolder.getName() + ".zip");

            // needs to be ordered, forEach cannot be used
            File[] files = packFolder.listFiles();
            if (files != null) for (final File folder : files) {
                if (folder.isDirectory() && folder.getName().equalsIgnoreCase("assets"))
                    getAllFiles(folder, output, "");
                else if (folder.isDirectory())
                    getAllFiles(folder, output, "assets/minecraft");
            }

            if (Settings.GENERATE_CUSTOM_ARMOR_TEXTURES.toBool() && customArmorsTextures.hasCustomArmors()) {
                String armorPath = "assets/minecraft/textures/models/armor";
                output.add(new VirtualFile(armorPath, "leather_layer_1.png", customArmorsTextures.getLayerOne()));
                output.add(new VirtualFile(armorPath, "leather_layer_2.png", customArmorsTextures.getLayerTwo()));
                if (Settings.AUTOMATICALLY_GENERATE_SHADER_COMPATIBLE_ARMOR.toBool())
                    output.addAll(customArmorsTextures.getOptifineFiles());
            }

            Collections.sort(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Set<String> malformedTextures = new HashSet<>();
        if (Settings.VERIFY_PACK_FILES.toBool())
            malformedTextures = verifyPackFormatting(output);

        if (Settings.GENERATE_ATLAS_FILE.toBool())
            AtlasGenerator.generateAtlasFile(output, malformedTextures);
        if (!Settings.MERGE_DUPLICATES.toBool()) {
            if (Settings.MERGE_FONTS.toBool())
                DuplicationHandler.mergeFontFiles(output);
            if (Settings.MERGE_ITEM_MODELS.toBool())
                DuplicationHandler.mergeBaseItemFiles(output);
        }

        List<String> excludedExtensions = Settings.EXCLUDED_FILE_EXTENSIONS.toStringList();
        excludedExtensions.removeIf(f -> f.equals("png") || f.equals("json"));
        if (!excludedExtensions.isEmpty() && !output.isEmpty()) {
            List<VirtualFile> newOutput = new ArrayList<>();
            for (VirtualFile virtual : output) for (String extension : excludedExtensions)
                if (virtual.getPath().endsWith(extension)) newOutput.add(virtual);
            output.removeAll(newOutput);
        }

        ZipUtils.writeZipFile(pack, output);
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
            if (virtualFile.getPath().endsWith(".json")) models.add(virtualFile);
            else if (virtualFile.getPath().endsWith(".png.mcmeta")) mcmeta.add(virtualFile.getPath());
            else if (virtualFile.getPath().endsWith(".png")) {
                textures.add(virtualFile);
                texturePaths.add(virtualFile.getPath());
            }
        }

        if (models.isEmpty() && !textures.isEmpty()) return Collections.emptySet();

        for (VirtualFile model : models) {
            if (model.getPath().contains(" ") || !model.getPath().toLowerCase().equals(model.getPath())) {
                Logs.logWarning("Found invalid model at <blue>" + model.getPath() + " </blue>.");
                Logs.logError("Models cannot contain spaces or Capital Letters in the filepath or filename");
                Logs.newline();
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
                JsonObject jsonModel = JsonParser.parseString(content).getAsJsonObject();
                if (jsonModel.has("textures")) {
                    for (JsonElement element : jsonModel.getAsJsonObject("textures").entrySet().stream().map(Map.Entry::getValue).toList()) {
                        String jsonTexture = element.getAsString();
                        if (!texturePaths.contains(modelPathToPackPath(jsonTexture))) {
                            if (!jsonTexture.startsWith("#") && !jsonTexture.startsWith("item/") && !jsonTexture.startsWith("block/")) {
                                try {
                                    Material.valueOf(Utils.removeParentDirs(Utils.removeExtension(jsonTexture)).toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    Logs.logWarning("Found invalid texture-path inside model-file <blue>" + model.getPath() + " </blue>.");
                                    malformedModels.add(model);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (VirtualFile texture : textures) {
            if (texture.getPath().contains(" ") || !texture.getPath().toLowerCase().equals(texture.getPath())) {
                Logs.debug(texture.getPath());

                Logs.logWarning("Found invalid texture at <blue>" + texture.getPath() + " </blue>.");
                Logs.logError("Textures cannot contain spaces or Capital Letters in the filepath or filename");
                Logs.newline();
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
                    Logs.logWarning("Found invalid texture at <blue>" + texture.getPath() + " </blue>.");
                    Logs.logError("Resolution of textures cannot exceed 256x256");
                    malformedTextures.add(texture);
                }
            }
        }

        if (!malformedTextures.isEmpty() || !malformedModels.isEmpty()) {
            Logs.logError("Pack contains malformed texture(s) and/or model(s)");
            Logs.logError("These need to be fixed, otherwise the resourcepack will be broken");
        } else Logs.logSuccess("No broken models or textures were found");

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

    private void extractFolders(boolean extractModels, boolean extractTextures, boolean extractShaders,
                                boolean extractLang, boolean extractFonts, boolean extractSounds, boolean extractAssets, boolean extractOptifine) {
        if (!extractModels && !extractTextures && !extractShaders && !extractLang && !extractAssets && !extractOptifine && !extractFonts && !extractSounds)
            return;

        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            final ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
            while (entry != null) {
                extract(entry, extractModels, extractTextures, extractShaders,
                        extractLang, extractFonts, extractSounds, extractAssets, extractOptifine, resourcesManager);
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
            zip.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void extractRequired() {
        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            final ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
            while (entry != null) {
                if (entry.getName().startsWith("pack/textures/required") || entry.getName().startsWith("pack/models/required")) {
                    resourcesManager.extractFileIfTrue(entry, entry.getName(), true);
                }
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
            zip.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void extract(ZipEntry entry, boolean extractModels, boolean extractTextures,
                         boolean extractShaders, boolean extractLang, boolean extractFonts,
                         boolean extractSounds, boolean extractAssets,
                         boolean extractOptifine, ResourcesManager resourcesManager) {
        final String name = entry.getName();
        final boolean isSuitable = (extractModels && name.startsWith("pack/models"))
                || (extractTextures && name.startsWith("pack/textures"))
                || (extractShaders && name.startsWith("pack/shaders"))
                || (extractLang && name.startsWith("pack/lang"))
                || (extractFonts && name.startsWith("pack/font"))
                || (extractSounds && name.startsWith("pack/sounds"))
                || (extractAssets && name.startsWith("/pack/assets"))
                || (extractOptifine && name.startsWith("pack/optifine"));
        resourcesManager.extractFileIfTrue(entry, name, isSuitable);
    }

    private Map<Material, List<ItemBuilder>> extractTexturedItems() {
        final Map<Material, List<ItemBuilder>> texturedItems = new HashMap<>();
        for (final Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            final ItemBuilder item = entry.getValue();
            if (item.getOraxenMeta().hasPackInfos()) {
                OraxenMeta oraxenMeta = item.getOraxenMeta();
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
                    for (int i = 0; i < items.size(); i++)
                        if (items.get(i).getOraxenMeta().getCustomModelData() > item
                                .getOraxenMeta()
                                .getCustomModelData()) {
                            items.add(i, item);
                            break;
                        } else if (i == items.size() - 1) {
                            items.add(item);
                            break;
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

    public void addOutputFiles(final VirtualFile... files) {
        for (VirtualFile file : files)
            outputFiles.put(file.getPath(), file);
    }

    public File getFile() {
        return pack;
    }

    private void extractInPackIfNotExists(final JavaPlugin plugin, final File file) {
        if (!file.exists())
            plugin.saveResource("pack/" + file.getName(), true);
    }

    private void makeDirsIfNotExists(final File folder) {
        if (!folder.exists())
            folder.mkdirs();
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

    private void generateFont(final FontManager fontManager) {
        if (!fontManager.autoGenerate)
            return;
        final JsonObject output = new JsonObject();
        final JsonArray providers = new JsonArray();
        for (final Glyph glyph : fontManager.getGlyphs())
            providers.add(glyph.toJson());
        for (final Font font : fontManager.getFonts())
            providers.add(font.toJson());
        output.add("providers", providers);
        writeStringToVirtual("assets/minecraft/font", "default.json", output.toString());
    }

    private void generateSound(final SoundManager soundManager) {
        if (!soundManager.isAutoGenerate())
            return;
        final JsonObject output = new JsonObject();

        for (CustomSound sound : handleCustomSoundEntries(soundManager.getCustomSounds()))
            output.add(sound.getName(), sound.toJson());
        writeStringToVirtual("assets/minecraft", "sounds.json", output.toString());
    }

    private void generateGestureFiles() {
        GestureManager gestureManager = OraxenPlugin.get().getGesturesManager();
        for (Map.Entry<String, String> entry : gestureManager.getPlayerHeadJsons().entrySet())
            writeStringToVirtual(StringUtils.removeEnd(Utils.getParentDirs(entry.getKey()), "/"), Utils.removeParentDirs(entry.getKey()), entry.getValue());
        writeStringToVirtual("assets/minecraft/models/item", "player_head.json", gestureManager.getSkullJson());
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_entity_translucent.vsh", gestureManager.getShaderVsh());
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_entity_translucent.fsh", gestureManager.getShaderFsh());
        writeStringToVirtual("assets/minecraft/shaders/core", "rendertype_entity_translucent.json", gestureManager.getShaderJson());
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
        } else if (!customSounds.getBoolean("noteblock_and_block", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
        } else if (!customSounds.getBoolean("stringblock_and_furniture", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
        } else if ((noteblock != null && !noteblock.getBoolean("enabled", true) && block != null && block.getBoolean("enabled", false))) {
            sounds.removeIf(s -> s.getName().startsWith("required.wood") || s.getName().startsWith("block.wood"));
        } else if (stringblock != null && !stringblock.getBoolean("enabled", true) && furniture != null && furniture.getBoolean("enabled", true)) {
            sounds.removeIf(s -> s.getName().startsWith("required.stone") || s.getName().startsWith("block.stone"));
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

    public void writeStringToVirtual(String folder, String name, String content) {
        addOutputFiles(new VirtualFile(folder, name,
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))));
    }

    private void getAllFiles(final File directory, final Collection<VirtualFile> fileList,
                             String newFolder,
                             final String... blacklisted) {
        final File[] files = directory.listFiles();
        final List<String> blacklist = Arrays.asList(blacklisted);
        if (files != null) for (final File file : files) {
            if (!blacklist.contains(file.getName()) && !file.isDirectory())
                readFileToVirtuals(fileList, file, newFolder);
            if (file.isDirectory())
                getAllFiles(file, fileList, newFolder, blacklisted);
        }
    }

    private void getFilesInFolder(final File dir, final Collection<VirtualFile> fileList,
                                  String newFolder,
                                  final String... blacklisted) {
        final File[] files = dir.listFiles();
        if (files != null) for (final File file : files)
            if (!file.isDirectory() && !Arrays.asList(blacklisted).contains(file.getName()))
                readFileToVirtuals(fileList, file, newFolder);
    }

    private void readFileToVirtuals(final Collection<VirtualFile> fileList, File file, String newFolder) {
        try {
            final InputStream fis;
            if (file.getName().endsWith(".json")) fis = processJsonFile(file);
            else if (file.getName().endsWith(".fsh")) fis = processShaderFile(file);
            else if (customArmorsTextures.registerImage(file)) return;
            else fis = new FileInputStream(file);

            fileList.add(new VirtualFile(getZipFilePath(file.getParentFile().getCanonicalPath(), newFolder), file.getName(), fis));
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

        // Deserialize said component to a string to handle other tags like glyphs
        content = AdventureUtils.parseMiniMessage(AdventureUtils.parseLegacy(content), AdventureUtils.tagResolver("prefix", Message.PREFIX.toString()));
        // Deserialize adventure component to legacy format due to resourcepacks not supporting adventure components
        content = AdventureUtils.parseLegacyThroughMiniMessage(content);
        newStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        newStream.close();
        return newStream;
    }

    private InputStream processShaderFile(File file) throws IOException {
        String content = Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8);
        content = content.replace(
                SHADER_PARAMETER_PLACEHOLDER, String.valueOf((int) Settings.ARMOR_RESOLUTION.getValue()));
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
