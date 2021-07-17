package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.ResourcesManager;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.ZipUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePack {

    private static File modelsFolder;
    private static File fontFolder;
    private static File assetsFolder;
    private static final List<Consumer<File>> PACK_MODIFIERS = new ArrayList<>();
    JavaPlugin plugin;
    private final File pack;

    public ResourcePack(JavaPlugin plugin, FontManager fontManager) {
        this.plugin = plugin;
        File packFolder = new File(plugin.getDataFolder(), "pack");
        makeDirsIfNotExists(packFolder);

        File texturesFolder = new File(packFolder, "textures");
        assetsFolder = new File(packFolder, "assets");
        modelsFolder = new File(packFolder, "models");
        fontFolder = new File(packFolder, "font");
        ;

        boolean extractModels = !modelsFolder.exists();
        boolean extractTextures = !texturesFolder.exists();
        boolean extractassets = !assetsFolder.exists();

        if (extractModels || extractTextures || extractassets) {
            ZipInputStream zip = ResourcesManager.browse();
            try {
                ZipEntry entry = zip.getNextEntry();
                ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());

                while (entry != null) {
                    String name = entry.getName();
                    boolean isSuitable = (extractModels && name.startsWith("pack/models"))
                            || (extractTextures && name.startsWith("pack/textures"))
                            || (extractassets && name.startsWith("/pack/assets"));

                    resourcesManager.extractFileIfTrue(entry, name, isSuitable);
                    entry = zip.getNextEntry();
                }
                zip.closeEntry();
                zip.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        this.pack = new File(packFolder, packFolder.getName() + ".zip");

        if (!Settings.GENERATE.toBool())
            return;

        if (pack.exists())
            pack.delete();

        extractInPackIfNotExists(plugin, new File(packFolder, "pack.mcmeta"));
        extractInPackIfNotExists(plugin, new File(packFolder, "pack.png"));

        // Sorting items to keep only one with models (and generate it if needed)
        Map<Material, List<ItemBuilder>> texturedItems = new HashMap<>();
        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            ItemBuilder item = entry.getValue();
            if (item.getOraxenMeta().hasPackInfos()) {
                if (item.getOraxenMeta().shouldGenerateModel()) {
                    Utils
                            .writeStringToFile(new File(modelsFolder, item.getOraxenMeta().getModelName() + ".json"),
                                    new ModelGenerator(item.getOraxenMeta()).getJson().toString());
                }
                List<ItemBuilder> items = texturedItems.getOrDefault(item.build().getType(), new ArrayList<>());
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
        for (Consumer<File> packModifier : PACK_MODIFIERS) {
            packModifier.accept(packFolder);
        }

        // zipping resourcepack
        List<File> rootFolder = new ArrayList<>();
        ZipUtils.getFilesInFolder(packFolder, rootFolder, packFolder.getName() + ".zip");

        List<File> subfolders = new ArrayList<>();
        List<File> assetFoldersCustom = new ArrayList<>();
        // needs to be ordered, forEach cannot be used
        for (File folder : packFolder.listFiles()) {
            if (folder.isDirectory() && folder.getName().equalsIgnoreCase("assets")) {
                System.out.println
                        (ChatColor.DARK_AQUA + "Experimental Custom Assets : You used a custom assets/minecraft !");
                ZipUtils.getAllFiles(folder, assetFoldersCustom);
            } else if (folder.isDirectory()) {
                ZipUtils.getAllFiles(folder, subfolders);
            }

        }

        rootFolder.addAll(assetFoldersCustom);

        Map<String, List<File>> fileListByZipDirectory = new LinkedHashMap<>();
        fileListByZipDirectory.put("assets/minecraft", subfolders);
        fileListByZipDirectory.put("", rootFolder);

        ZipUtils.writeZipFile(pack, packFolder, fileListByZipDirectory);
    }

    public File getFile() {
        return pack;
    }

    public static File getAssetsFolder() {
        return assetsFolder;
    }

    private void extractInPackIfNotExists(JavaPlugin plugin, File file) {
        if (!file.exists())
            plugin.saveResource("pack/" + file.getName(), true);
    }

    private void makeDirsIfNotExists(File folder) {
        if (!folder.exists())
            folder.mkdirs();
    }

    @SafeVarargs
    public static void addModifiers(Consumer<File>... modifiers) {
        PACK_MODIFIERS.addAll(Arrays.asList(modifiers));
    }

    private void generatePredicates(Map<Material, List<ItemBuilder>> texturedItems) {
        File itemsFolder = new File(modelsFolder, "item");
        makeDirsIfNotExists(itemsFolder);
        File blocksFolder = new File(modelsFolder, "block");
        makeDirsIfNotExists(blocksFolder);

        for (Map.Entry<Material, List<ItemBuilder>> texturedItemsEntry : texturedItems.entrySet()) {
            Material entryMaterial = texturedItemsEntry.getKey();
            PredicatesGenerator predicatesGenerator = new PredicatesGenerator(entryMaterial,
                    texturedItemsEntry.getValue());
            String vanillaModelName = predicatesGenerator.getVanillaModelName(entryMaterial) + ".json";

            Utils.writeStringToFile(new File(modelsFolder, vanillaModelName), predicatesGenerator.toJSON().toString());
        }
    }

    private void generateFont(FontManager fontManager) {
        if (!fontManager.autoGenerate)
            return;
        makeDirsIfNotExists(fontFolder);
        File fontFile = new File(fontFolder, "default.json");
        JsonObject output = new JsonObject();
        JsonArray providers = new JsonArray();
        for (Glyph glyph : fontManager.getGlyphs())
            providers.add(glyph.toJson());
        output.add("providers", providers);
        Utils.writeStringToFile(fontFile, output.toString());
    }

}
