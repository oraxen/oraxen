package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Font;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.ZipUtils;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePack {

    private static final List<Consumer<File>> PACK_MODIFIERS = new ArrayList<>();
    private static File modelsFolder;
    private static File fontFolder;
    private static File assetsFolder;
    private final File pack;
    JavaPlugin plugin;

    public ResourcePack(final JavaPlugin plugin, final FontManager fontManager) {
        this.plugin = plugin;
        final File packFolder = new File(plugin.getDataFolder(), "pack");
        makeDirsIfNotExists(packFolder);

        final File texturesFolder = new File(packFolder, "textures");
        final File shadersFolder = new File(packFolder, "shaders");
        assetsFolder = new File(packFolder, "assets");
        modelsFolder = new File(packFolder, "models");
        fontFolder = new File(packFolder, "font");
        final File langFolder = new File(packFolder, "lang");

        final boolean extractModels = !modelsFolder.exists();
        final boolean extractTextures = !texturesFolder.exists();
        final boolean extractShaders = !shadersFolder.exists();
        final boolean extractLang = !langFolder.exists();
        final boolean extractassets = !assetsFolder.exists();

        if (extractModels || extractTextures || extractShaders || extractLang || extractassets) {
            final ZipInputStream zip = ResourcesManager.browse();
            try {
                ZipEntry entry = zip.getNextEntry();
                final ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());

                while (entry != null) {
                    final String name = entry.getName();
                    final boolean isSuitable = (extractModels && name.startsWith("pack/models"))
                            || (extractTextures && name.startsWith("pack/textures"))
                            || (extractTextures && name.startsWith("pack/shaders"))
                            || (extractLang && name.startsWith("pack/lang"))
                            || (extractassets && name.startsWith("/pack/assets"));

                    resourcesManager.extractFileIfTrue(entry, name, isSuitable);
                    entry = zip.getNextEntry();
                }
                zip.closeEntry();
                zip.close();
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        pack = new File(packFolder, packFolder.getName() + ".zip");

        if (!Settings.GENERATE.toBool())
            return;

        if (pack.exists())
            pack.delete();

        extractInPackIfNotExists(plugin, new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(plugin, new File(packFolder, "pack.png"));

        // Sorting items to keep only one with models (and generate it if needed)
        final Map<Material, List<ItemBuilder>> texturedItems = new HashMap<>();
        for (final Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            final ItemBuilder item = entry.getValue();
            if (item.getOraxenMeta().hasPackInfos()) {
                if (item.getOraxenMeta().shouldGenerateModel()) Utils
                        .writeStringToFile(new File(modelsFolder, item.getOraxenMeta().getModelName() + ".json"),
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
        generatePredicates(texturedItems);
        generateFont(fontManager);
        for (final Consumer<File> packModifier : PACK_MODIFIERS) packModifier.accept(packFolder);

        // zipping resourcepack
        final List<File> rootFolder = new ArrayList<>();
        ZipUtils.getFilesInFolder(packFolder, rootFolder, packFolder.getName() + ".zip");

        final List<File> subfolders = new ArrayList<>();
        final List<File> assetFoldersCustom = new ArrayList<>();
        // needs to be ordered, forEach cannot be used
        for (final File folder : packFolder.listFiles())
            if (folder.isDirectory() && folder.getName().equalsIgnoreCase("assets"))
                ZipUtils.getAllFiles(folder, assetFoldersCustom);
            else if (folder.isDirectory())
                ZipUtils.getAllFiles(folder, subfolders);

        rootFolder.addAll(assetFoldersCustom);

        final Map<String, List<File>> fileListByZipDirectory = new LinkedHashMap<>();
        fileListByZipDirectory.put("assets/minecraft", subfolders);
        fileListByZipDirectory.put("", rootFolder);
        ZipUtils.writeZipFile(pack, packFolder, fileListByZipDirectory);
    }

    public static File getAssetsFolder() {
        return assetsFolder;
    }

    @SafeVarargs
    public static void addModifiers(final Consumer<File>... modifiers) {
        PACK_MODIFIERS.addAll(Arrays.asList(modifiers));
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
        final File itemsFolder = new File(modelsFolder, "item");
        makeDirsIfNotExists(itemsFolder);
        final File blocksFolder = new File(modelsFolder, "block");
        makeDirsIfNotExists(blocksFolder);

        for (final Map.Entry<Material, List<ItemBuilder>> texturedItemsEntry : texturedItems.entrySet()) {
            final Material entryMaterial = texturedItemsEntry.getKey();
            final PredicatesGenerator predicatesGenerator = new PredicatesGenerator(entryMaterial,
                    texturedItemsEntry.getValue());
            final String vanillaModelName = predicatesGenerator.getVanillaModelName(entryMaterial) + ".json";

            Utils.writeStringToFile(new File(modelsFolder, vanillaModelName), predicatesGenerator.toJSON().toString());
        }
    }

    private void generateFont(final FontManager fontManager) {
        if (!fontManager.autoGenerate)
            return;
        makeDirsIfNotExists(fontFolder);
        final File fontFile = new File(fontFolder, "default.json");
        final JsonObject output = new JsonObject();
        final JsonArray providers = new JsonArray();
        for (final Glyph glyph : fontManager.getGlyphs())
            providers.add(glyph.toJson());
        for (final Font font : fontManager.getFonts())
            providers.add(font.toJson());
        output.add("providers", providers);
        Utils.writeStringToFile(fontFile, output.toString().replace("\\\\u", "\\u"));
    }

}
