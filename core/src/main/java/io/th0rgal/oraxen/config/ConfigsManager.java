package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.items.ItemTemplate;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.pack.generation.DuplicationHandler;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.io.FileUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private File itemsFolder;
    private File glyphsFolder;
    private File schematicsFolder;
    private File gesturesFolder;

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

    public File getSettingsFile() {
        return new File(plugin.getDataFolder(), "settings.yml");
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
            return OraxenYaml.loadConfiguration(inputStreamReader);
        } finally {
            try {
                inputStreamReader.close();
            } catch (IOException e) {
                Logs.logError("Failed to extract default file: " + source);
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        }
    }

    public void validatesConfig() {
        ResourcesManager tempManager = new ResourcesManager(OraxenPlugin.get());
        mechanics = validate(tempManager, "mechanics.yml", defaultMechanics);
        settings = validate(tempManager, "settings.yml", defaultSettings);
        font = validate(tempManager, "font.yml", defaultFont);
        hud = validate(tempManager, "hud.yml", defaultHud);
        sound = validate(tempManager, "sound.yml", defaultSound);
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        languagesFolder.mkdir();
        String languageFile = "languages/" + settings.getString(Settings.PLUGIN_LANGUAGE.getPath()) + ".yml";
        language = validate(tempManager, languageFile, defaultLanguage);

        // check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                tempManager.extractConfigsInFolder("items", "yml");
        }

        // check glyphsFolder
        glyphsFolder = new File(plugin.getDataFolder(), "glyphs");
        if (!glyphsFolder.exists()) {
            glyphsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                tempManager.extractConfigsInFolder("glyphs", "yml");
            else tempManager.extractConfiguration("glyphs/interface.yml");
        }

        // check schematicsFolder
        schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                tempManager.extractConfigsInFolder("schematics", "schem");
        }

        // check gestures
        gesturesFolder = new File(plugin.getDataFolder(), "gestures");
        if (!gesturesFolder.exists()) {
            gesturesFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                tempManager.extractConfigsInFolder("gestures", "yml");
        }

    }

    private YamlConfiguration validate(ResourcesManager resourcesManager, String configName, YamlConfiguration defaultConfiguration) {
        File configurationFile = resourcesManager.extractConfiguration(configName);
        YamlConfiguration configuration = OraxenYaml.loadConfiguration(configurationFile);
        boolean updated = false;
        for (String key : defaultConfiguration.getKeys(true)) {
            if (!skippedYamlKeys.stream().filter(key::startsWith).toList().isEmpty()) continue;
            if (configuration.get(key) == null) {
                updated = true;
                Message.UPDATING_CONFIG.log(AdventureUtils.tagResolver("option", key));
                configuration.set(key, defaultConfiguration.get(key));
            }
        }

        for (String key : configuration.getKeys(false)) if (removedYamlKeys.contains(key)) {
                updated = true;
                Message.REMOVING_CONFIG.log(AdventureUtils.tagResolver("option", key));
                configuration.set(key, null);
        }

        if (updated)
            try {
                configuration.save(configurationFile);
            } catch (IOException e) {
                Logs.logError("Failed to save updated configuration file: " + configurationFile.getName());
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        return configuration;
    }

    // Skip optional keys and subkeys
    private final List<String> skippedYamlKeys =
            List.of(
                    "oraxen_inventory.menu_layout",
                    "Misc.armor_equip_event_bypass"
            );

    private final List<String> removedYamlKeys =
            List.of(
                    "armorpotioneffects"
            );

    public Collection<Glyph> parseGlyphConfigs() {
        List<File> glyphFiles = getGlyphFiles();
        List<Glyph> output = new ArrayList<>();
        Map<String, Character> charPerGlyph = new HashMap<>();
        for (File file : glyphFiles) {
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection glyphSection = configuration.getConfigurationSection(key);
                if (glyphSection == null) continue;
                String characterString = glyphSection.getString("char", "");
                char character = !characterString.isBlank() ? characterString.charAt(0) : Character.MIN_VALUE;
                if (character != Character.MIN_VALUE)
                    charPerGlyph.put(key, character);
            }
        }

        for (File file : glyphFiles) {
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            boolean fileChanged = false;
            for (String key : configuration.getKeys(false)) {
                char character = charPerGlyph.getOrDefault(key, Character.MIN_VALUE);
                if (character == Character.MIN_VALUE) {
                    character = Utils.firstEmpty(charPerGlyph, 42000);
                    charPerGlyph.put(key, character);
                }
                Glyph glyph = new Glyph(key, configuration.getConfigurationSection(key), character);
                if (glyph.isFileChanged())
                    fileChanged = true;
                glyph.verifyGlyph(output);
                output.add(glyph);
            }
            if (fileChanged && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
                try {
                    configuration.save(file);
                } catch (IOException e) {
                    Logs.logWarning("Failed to save updated glyph file: " + file.getName());
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                }
            }
        }
        return output;
    }

    public Map<File, Map<String, ItemBuilder>> parseItemConfig() {
        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        ItemBuilder errorItem = new ItemParser(Settings.ERROR_ITEM.toConfigSection()).buildItem();
        for (File file : getItemFiles()) parseMap.put(file, parseItemConfig(file, errorItem));
        return parseMap;
    }

    public void assignAllUsedModelDatas() {
        Map<Material, Map<Integer, String>> assignedModelDatas = new HashMap<>();
        for (File file : getItemFiles()) {
            if (!file.exists()) continue;
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            boolean fileChanged = false;

            for (String key : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(key);
                if (itemSection == null) continue;
                ConfigurationSection packSection = itemSection.getConfigurationSection("Pack");
                Material material = Material.getMaterial(itemSection.getString("material", ""));
                if (packSection == null || material == null) continue;
                int modelData = packSection.getInt("custom_model_data", -1);
                String model = getItemModelFromConfigurationSection(packSection);
                if (modelData == -1) continue;
                if (assignedModelDatas.containsKey(material) && assignedModelDatas.get(material).containsKey(modelData)) {
                    if (assignedModelDatas.get(material).get(modelData).equals(model)) continue;
                    Logs.logError("CustomModelData " + modelData + " is already assigned to another item with this material but different model");
                    if (file.getAbsolutePath().equals(DuplicationHandler.getDuplicateItemFile(material).getAbsolutePath()) && Settings.RETAIN_CUSTOM_MODEL_DATA.toBool()) {
                        Logs.logWarning("Due to " + Settings.RETAIN_CUSTOM_MODEL_DATA.getPath() + " being enabled,");
                        Logs.logWarning("the model data will not removed from " + file.getName() + ": " + key + ".");
                        Logs.logWarning("There will still be a conflict which you need to solve yourself.");
                        Logs.logWarning("Either reset the CustomModelData of this item, or change the CustomModelData of the conflicting item.", true);
                    } else {
                        Logs.logWarning("Removing custom model data from " + file.getName() + ": " + key, true);
                        packSection.set("custom_model_data", null);
                        fileChanged = true;
                    }
                    continue;
                }

                assignedModelDatas.computeIfAbsent(material, k -> new HashMap<>()).put(modelData, model);
                ModelData.DATAS.computeIfAbsent(material, k -> new HashMap<>()).put(key, modelData);
            }

            if (fileChanged) try {
                configuration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void parseAllItemTemplates() {
        for (File file : getItemFiles()) {
            if (file == null || !file.exists()) continue;
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(key);
                if (itemSection != null && itemSection.isBoolean("template")) new ItemTemplate(itemSection);
            }
        }
    }

    private String getItemModelFromConfigurationSection(ConfigurationSection packSection) {
        String model = packSection.getString("model", "");
        if (model.isEmpty() && packSection.getBoolean("generate_model", false)) {
            model = packSection.getParent().getName();
        }
        return model;
    }

    public Map<String, ItemBuilder> parseItemConfig(File itemFile, ItemBuilder errorItem) {
        YamlConfiguration config = OraxenYaml.loadConfiguration(itemFile);
        Map<String, ItemParser> parseMap = new LinkedHashMap<>();

        for (String itemKey : config.getKeys(false)) {
            ConfigurationSection itemSection = config.getConfigurationSection(itemKey);
            if (itemSection == null || ItemTemplate.isTemplate(itemKey)) continue;
            parseMap.put(itemKey, new ItemParser(itemSection));
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
                map.put(entry.getKey(), errorItem);
                Logs.logError("ERROR BUILDING ITEM \"" + entry.getKey() + "\"");
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                else Logs.logWarning(e.getMessage());
            }
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }

        if (configUpdated) {
            String content = config.saveToString();
            if (VersionUtil.atOrAbove("1.20.5"))
                content = content.replace("displayname: ", "itemname: ");
            else content = content.replace("itemname: ", "displayname: ");

            try {
                FileUtils.writeStringToFile(itemFile, content, StandardCharsets.UTF_8);
            } catch (Exception e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
                try {
                    config.save(itemFile);
                } catch (Exception e1) {
                    if (Settings.DEBUG.toBool()) e1.printStackTrace();
                }
            }
        }

        return map;
    }

    private List<File> getItemFiles() {
        if (itemsFolder == null || !itemsFolder.exists()) return new ArrayList<>();
        return FileUtils.listFiles(itemsFolder, new String[]{"yml"}, true).stream().filter(OraxenYaml::isValidYaml).sorted().toList();
    }

    private List<File> getGlyphFiles() {
        if (glyphsFolder == null || !glyphsFolder.exists()) return new ArrayList<>();
        return FileUtils.listFiles(glyphsFolder, new String[]{"yml"}, true).stream().filter(OraxenYaml::isValidYaml).sorted().toList();
    }
}
