package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.sound.CustomSound;
import io.th0rgal.oraxen.sound.SoundManager;
import io.th0rgal.oraxen.utils.CustomArmorsTextures;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.ZipUtils;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePack {

    private static final String SHADER_PARAMETER_PLACEHOLDER = "{#TEXTURE_RESOLUTION#}";

    private Map<String, Collection<Consumer<File>>> packModifiers;
    private Map<String, VirtualFile> outputFiles;
    private CustomArmorsTextures customArmorsTextures;
    private File modelsFolder;
    private File fontFolder;
    private File assetsFolder;
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
        customArmorsTextures = new CustomArmorsTextures((int) Settings.ARMOR_RESOLUTION.getValue());
        packFolder = new File(plugin.getDataFolder(), "pack");
        makeDirsIfNotExists(packFolder);
        pack = new File(packFolder, packFolder.getName() + ".zip");
        assetsFolder = new File(packFolder, "assets");
        modelsFolder = new File(packFolder, "models");
        fontFolder = new File(packFolder, "font");
        final File langFolder = new File(packFolder, "lang");
        extractFolders(!modelsFolder.exists(), !new File(packFolder, "textures").exists(),
                !new File(packFolder, "shaders").exists(),
                !langFolder.exists(), !new File(packFolder, "sounds").exists(), !assetsFolder.exists());

        if (!Settings.GENERATE.toBool())
            return;

        if (pack.exists()) {
            try {
                Files.delete(pack.toPath());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        extractInPackIfNotExists(plugin, new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(plugin, new File(packFolder, "pack.png"));

        // Sorting items to keep only one with models (and generate it if needed)
        final Map<Material, List<ItemBuilder>> texturedItems = extractTexturedItems();
        generatePredicates(texturedItems);
        generateFont(fontManager);
        generateSound(soundManager);
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
            for (final File folder : packFolder.listFiles())
                if (folder.isDirectory() && folder.getName().equalsIgnoreCase("assets"))
                    getAllFiles(folder, output, "");
                else if (folder.isDirectory())
                    getAllFiles(folder, output, "assets/minecraft");

            if (customArmorsTextures.hasCustomArmors()) {
                output.add(new VirtualFile("assets/minecraft/textures/models/armor",
                        "leather_layer_1.png",
                        customArmorsTextures.getLayerOne()));
                output.add(new VirtualFile("assets/minecraft/textures/models/armor",
                        "leather_layer_2.png",
                        customArmorsTextures.getLayerTwo()));
            }
            Collections.sort(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ZipUtils.writeZipFile(pack, packFolder, output);
    }

    private void extractFolders(boolean extractModels, boolean extractTextures, boolean extractShaders,
                                boolean extractLang, boolean extractSounds, boolean extractAssets) {
        if (!extractModels && !extractTextures && !extractShaders && !extractLang && !extractAssets)
            return;
        final ZipInputStream zip = ResourcesManager.browse();
        try {
            ZipEntry entry = zip.getNextEntry();
            final ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
            while (entry != null) {
                extract(entry, extractModels, extractTextures,
                        extractLang, extractSounds, extractAssets, resourcesManager);
                entry = zip.getNextEntry();
            }
            zip.closeEntry();
            zip.close();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void extract(ZipEntry entry, boolean extractModels,
                         boolean extractTextures, boolean extractLang,
                         boolean extractSounds, boolean extractAssets,
                         ResourcesManager resourcesManager) {
        final String name = entry.getName();
        final boolean isSuitable = (extractModels && name.startsWith("pack/models"))
                || (extractTextures && name.startsWith("pack/textures"))
                || (extractTextures && name.startsWith("pack/shaders"))
                || (extractLang && name.startsWith("pack/lang"))
                || (extractSounds && name.startsWith("pack/sounds"))
                || (extractAssets && name.startsWith("/pack/assets"));
        resourcesManager.extractFileIfTrue(entry, name, isSuitable);
    }

    private Map<Material, List<ItemBuilder>> extractTexturedItems() {
        final Map<Material, List<ItemBuilder>> texturedItems = new HashMap<>();
        for (final Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            final ItemBuilder item = entry.getValue();
            if (item.getOraxenMeta().hasPackInfos()) {
                if (item.getOraxenMeta().shouldGenerateModel())
                    writeStringToVirtual("assets/minecraft/models",
                            item.getOraxenMeta().getModelName() + ".json",
                            new ModelGenerator(item.getOraxenMeta()).getJson().toString());
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
        for (CustomSound sound : soundManager.getCustomSounds())
            output.add(sound.getName(), sound.toJson());
        writeStringToVirtual("assets/oraxen", "sounds.json", output.toString());
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
        for (final File file : files) {
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
        for (final File file : files)
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

            fileList.add(new VirtualFile(getZipFilePath(file.getParentFile().getCanonicalPath(), newFolder),
                    file.getName(),
                    fis));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream processJsonFile(File file) throws IOException {
        String content = Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8);
        content = Utils.LEGACY_COMPONENT_SERIALIZER.serialize(Utils.MINI_MESSAGE.deserialize(content,
                TemplateResolver.templates(Template.template("prefix", Message.PREFIX.toComponent()))));
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
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
