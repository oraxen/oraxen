package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.pack.generation.DuplicationHandler;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ConfigsManager {

    private final JavaPlugin plugin;
    private final YamlConfiguration defaultMechanics;
    private final YamlConfiguration defaultSettings;
    private final YamlConfiguration defaultFont;
    private final YamlConfiguration defaultSound;
    private final YamlConfiguration defaultLanguage;
    private final YamlConfiguration defaultHud;
    private YamlConfiguration mechanics;
    private YamlConfiguration settings;
    private YamlConfiguration font;
    private YamlConfiguration sound;
    private YamlConfiguration language;
    private YamlConfiguration hud;
    private YamlConfiguration gestures;
    private File itemsFolder;
    private File glyphsFolder;
    private File schematicsFolder;

    public ConfigsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        defaultMechanics = extractDefault("mechanics.yml");
        defaultSettings = extractDefault("settings.yml");
        defaultFont = extractDefault("font.yml");
        defaultSound = extractDefault("sound.yml");
        defaultLanguage = extractDefault("languages/english.yml");
        defaultHud = extractDefault("hud.yml");
    }

    public YamlConfiguration getMechanics() {
        return mechanics != null ? mechanics : defaultMechanics;
    }

    public YamlConfiguration getSettings() {
        return settings != null ? settings : defaultSettings;
    }

    public YamlConfiguration getLanguage() {
        return language != null ? language : defaultLanguage;
    }

    public YamlConfiguration getFont() {
        return font != null ? font : defaultFont;
    }

    public YamlConfiguration getHud() {
        return hud != null ? hud : defaultHud;
    }

    public YamlConfiguration getSound() {
        return sound != null ? sound : defaultSound;
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    private YamlConfiguration extractDefault(String source) {
        InputStreamReader inputStreamReader = new InputStreamReader(plugin.getResource(source));
        try {
            return YamlConfiguration.loadConfiguration(inputStreamReader);
        } finally {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean validatesConfig() {
        ResourcesManager resourcesManager = new ResourcesManager(OraxenPlugin.get());
        mechanics = validate(resourcesManager, "mechanics.yml", defaultMechanics);
        settings = validate(resourcesManager, "settings.yml", defaultSettings);
        font = validate(resourcesManager, "font.yml", defaultFont);
        hud = validate(resourcesManager, "hud.yml", defaultHud);
        sound = validate(resourcesManager, "sound.yml", defaultSound);
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        languagesFolder.mkdir();
        String languageFile = "languages/" + settings.getString(Settings.PLUGIN_LANGUAGE.getPath()) + ".yml";
        language = validate(resourcesManager, languageFile, defaultLanguage);

        // check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                new ResourcesManager(plugin).extractConfigsInFolder("items", "yml");
        }

        // check glyphsFolder
        glyphsFolder = new File(plugin.getDataFolder(), "glyphs");
        if (!glyphsFolder.exists()) {
            glyphsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                new ResourcesManager(plugin).extractConfigsInFolder("glyphs", "yml");
            else new ResourcesManager(plugin).extractConfiguration("glyphs/interface.yml");
        }

        // check schematicsFolder
        schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                new ResourcesManager(plugin).extractConfigsInFolder("schematics", "schem");
        }

        return true; // todo : return false when an error is detected + prints a detailed error
    }

    private YamlConfiguration validate(ResourcesManager resourcesManager, String configName, YamlConfiguration defaultConfiguration) {
        File configurationFile = resourcesManager.extractConfiguration(configName);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile);
        boolean updated = false;
        for (String key : defaultConfiguration.getKeys(true)) {
            if (!skippedYamlKeys.stream().filter(key::startsWith).toList().isEmpty()) continue;
            if (configuration.get(key) == null) {
                updated = true;
                Message.UPDATING_CONFIG.log(AdventureUtils.tagResolver("option", key));
                configuration.set(key, defaultConfiguration.get(key));
            }        }
        if (updated)
            try {
                configuration.save(configurationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        return configuration;
    }

    // Skip optional keys and subkeys
    private final List<String> skippedYamlKeys =
            List.of(
                    "gui_inventory",
                    "Misc.armor_equip_event_bypass"
            );

    public Collection<Glyph> parseGlyphConfigs() {
        List<Glyph> output = new ArrayList<>();
        List<File> configs = Arrays
                .stream(getGlyphsFiles())
                .filter(file -> file.getName().endsWith(".yml"))
                .toList();
        Map<String, Integer> codePerGlyph = new HashMap<>();
        for (File file : configs) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection glyphSection = configuration.getConfigurationSection(key);
                if (glyphSection == null) continue;
                int code = glyphSection.getInt("code", -1);
                if (code != -1)
                    codePerGlyph.put(key, code);
            }
        }

        for (File file : configs) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            boolean fileChanged = false;
            for (String key : configuration.getKeys(false)) {
                int code = codePerGlyph.getOrDefault(key, -1);
                if (code == -1) {
                    code = Utils.firstEmpty(codePerGlyph, 42000);
                    codePerGlyph.put(key, code);
                }
                Glyph glyph = new Glyph(key, configuration.getConfigurationSection(key), code);
                if (glyph.isFileChanged())
                    fileChanged = true;
                glyph.verifyGlyph(output);
                output.add(glyph);
            }
            if (fileChanged && Settings.AUTOMATICALLY_SET_GLYPH_CODE.toBool())
                try {
                    configuration.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return output;
    }

    private File[] getGlyphsFiles() {
        File[] glyphConfigs = glyphsFolder.listFiles();
        Arrays.sort(glyphConfigs);
        return glyphConfigs;
    }

    public Map<File, Map<String, ItemBuilder>> parseItemConfig() {

        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        List<File> configs = getItemsFiles();
        for (File file : configs) {
            parseMap.put(file, parseItemConfig(YamlConfiguration.loadConfiguration(file), file));
        }
        return parseMap;
    }

    public void assignAllUsedModelDatas() {
        List<File> itemConfigs = getItemsFiles();
        Map<Material, List<Integer>> assignedModelDatas = new HashMap<>();
        for (File file : itemConfigs) {
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(key);
                if (itemSection == null) continue;
                Material material = Material.matchMaterial(itemSection.getString("material", ""));
                if (material == null) continue;
                int modelData = itemSection.getInt("Pack.custom_model_data", -1);
                if (modelData == -1) continue;
                if (assignedModelDatas.containsKey(material) && assignedModelDatas.get(material).contains(modelData)) {
                    Logs.logError("CustomModelData " + modelData + " is already assigned to " + material + " in " + file.getName() + " " + key);
                    if (file.getName().equals(DuplicationHandler.DUPLICATE_FILE_MERGE_NAME) && Settings.RETAIN_CUSTOM_MODEL_DATA.toBool()) {
                        Logs.logWarning("Due to " + Settings.RETAIN_CUSTOM_MODEL_DATA.getPath() + " being enabled,");
                        Logs.logWarning("the model data will not removed from " + file.getName() + ": " + key + ".");
                        Logs.logWarning("There will still be a conflict which you need to solve yourself.");
                        Logs.logWarning("Either reset the CustomModelData of this item, or change the CustomModelData of the conflicting item.");
                    } else {
                        Logs.logWarning("Removing custom model data from " + file.getName() + ": " + key);
                        itemSection.set("Pack.custom_model_data", null);
                    }
                    Logs.newline();
                    continue;
                }
                assignedModelDatas.computeIfAbsent(material, k -> new ArrayList<>()).add(modelData);
                ModelData.DATAS.computeIfAbsent(material, k -> new HashMap<>()).put(key, modelData);
            }
            try {
                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Material, List<Integer>> entry : assignedModelDatas.entrySet()) {
            Collections.sort(entry.getValue());
        }
    }

    public Map<String, ItemBuilder> parseItemConfig(YamlConfiguration config, File itemFile) {
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();
        ItemParser errorItem = new ItemParser(Settings.ERROR_ITEM.toConfigSection());
        for (String itemSectionName : config.getKeys(false)) {
            ConfigurationSection itemSection = config.getConfigurationSection(itemSectionName);
            if (itemSection == null) continue;
            parseMap.put(itemSectionName, new ItemParser(itemSection));
        }
        boolean configUpdated = false;
        // because we must have parse all the items before building them to be able to
        // use available models
        Map<String, ItemBuilder> map = new LinkedHashMap<>();
        for (Map.Entry<String, ItemParser> entry : parseMap.entrySet()) {
            ItemParser itemParser = entry.getValue();
            try {
                map.put(entry.getKey(), itemParser.buildItem());
            } catch (Exception e) {
                map.put(entry.getKey(),
                        errorItem.buildItem(String.valueOf(ChatColor.DARK_RED) + ChatColor.BOLD
                                + e.getClass().getSimpleName() + ": " + ChatColor.RED + entry.getKey()));
                Logs.logError("ERROR BUILDING ITEM \"" + entry.getKey() + "\"");
                e.printStackTrace();
            }
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }
        if (configUpdated)
            try {
                config.save(itemFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        return map;
    }

    private List<File> getItemsFiles() {
        File[] itemFiles = itemsFolder.listFiles(pathname -> pathname.getName().endsWith(".yml"));
        if (itemFiles == null) return new ArrayList<>();
        List<File> itemConfigs = new ArrayList<>(Arrays.stream(itemFiles).toList());
        Collections.sort(itemConfigs);
        return itemConfigs;
    }

}
