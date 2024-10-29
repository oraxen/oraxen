package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.ItemParser;
import io.th0rgal.oraxen.items.ItemTemplate;
import io.th0rgal.oraxen.items.ModelData;
import io.th0rgal.oraxen.utils.*;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
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
    private final YamlConfiguration defaultFont;
    private final YamlConfiguration defaultSounds;
    private final YamlConfiguration defaultLanguage;
    private final YamlConfiguration defaultHud;
    private YamlConfiguration mechanics;
    private YamlConfiguration settings;
    private YamlConfiguration font;
    private YamlConfiguration sounds;
    private YamlConfiguration language;
    private YamlConfiguration hud;
    private File itemsFolder;
    private File glyphsFolder;
    private File schematicsFolder;

    public ConfigsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        defaultMechanics = extractDefault("mechanics.yml");
        defaultFont = extractDefault("bitmaps.yml");
        defaultSounds = extractDefault("sounds.yml");
        defaultLanguage = extractDefault("languages/english.yml");
        defaultHud = extractDefault("hud.yml");
    }

    public YamlConfiguration getMechanics() {
        return mechanics != null ? mechanics : defaultMechanics;
    }

    public YamlConfiguration getSettings() {
        if (settings == null) settings = Settings.validateSettings();
        return settings;
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

    public YamlConfiguration getSounds() {
        return sounds != null ? sounds : defaultSounds;
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
        ResourcesManager resourcesManager = OraxenPlugin.get().resourceManager();
        settings = Settings.validateSettings();
        mechanics = validate(resourcesManager, "mechanics.yml", defaultMechanics);
        font = validate(resourcesManager, "bitmaps.yml", defaultFont);
        hud = validate(resourcesManager, "hud.yml", defaultHud);
        sounds = validate(resourcesManager, "sounds.yml", defaultSounds);
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        languagesFolder.mkdir();
        String languageFile = "languages/" + Settings.PLUGIN_LANGUAGE + ".yml";
        language = validate(resourcesManager, languageFile, defaultLanguage);

        // check itemsFolder
        itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                resourcesManager.extractConfigsInFolder("items", "yml");
        }

        // check glyphsFolder
        glyphsFolder = new File(plugin.getDataFolder(), "glyphs");
        if (!glyphsFolder.exists()) {
            glyphsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                resourcesManager.extractConfigsInFolder("glyphs", "yml");
            else resourcesManager.extractConfiguration("glyphs/interface.yml");
        }

        // check schematicsFolder
        schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            if (Settings.GENERATE_DEFAULT_CONFIGS.toBool())
                resourcesManager.extractConfigsInFolder("schematics", "schem");
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
            List.of("armorpotioneffects");

    public Collection<Glyph> parseGlyphConfigs() {
        List<File> glyphFiles = getGlyphFiles();
        List<Glyph> output = new ArrayList<>();
        Map<String, Character> charPerGlyph = new HashMap<>();
        for (File file : glyphFiles) {
            if (file.getName().startsWith("shift")) continue;
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
            if (file.getName().startsWith("shift")) continue;
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
                //glyph.verifyGlyph(output);
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

        // add RequiredGlyph
        char requiredChar = charPerGlyph.getOrDefault("required", Utils.firstEmpty(charPerGlyph, 42000));
        output.add(new Glyph.RequiredGlyph(requiredChar));

        return output;
    }

    public Map<File, Map<String, ItemBuilder>> parseItemConfig() {
        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        ItemBuilder errorItem = new ItemBuilder(Material.PODZOL);
        for (File file : getItemFiles()) parseMap.put(file, parseItemConfig(file, errorItem));
        return parseMap;
    }

    public void assignAllUsedModelDatas() {
        Map<Material, Map<Integer, Key>> assignedModelDatas = new HashMap<>();
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
                validatePackSection(key, packSection);
                int modelData = packSection.getInt("custom_model_data", -1);
                Key model = getItemModelFromConfigurationSection(key, packSection);
                if (modelData == -1) continue;
                if (assignedModelDatas.containsKey(material) && assignedModelDatas.get(material).containsKey(modelData)) {
                    if (assignedModelDatas.get(material).get(modelData).equals(model)) continue;
                    Logs.logError("CustomModelData " + modelData + " is already assigned to another item with this material but different model");
                    continue;
                }

                assignedModelDatas.computeIfAbsent(material, k -> new HashMap<>()).put(modelData, model);
                ModelData.DATAS.computeIfAbsent(material, k -> new HashMap<>()).put(Key.key(key), modelData);
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

    private void validatePackSection(String itemId, ConfigurationSection packSection) {
        String model = packSection.getString("model", "");
        if (model.isEmpty()) model = itemId;

        if (!Key.parseable(model)) {
            Logs.logWarning("Found invalid model in OraxenItem <blue>" + itemId + "</blue>: <aqua>" + model);
            Logs.logWarning("Model-paths must only contain characters <yellow>[a-z0-9/._-]");
        }

        for (String texture : packSection.getStringList("textures")) {
            if (!Key.parseable(texture)) {
                Logs.logWarning("Found invalid texture in OraxenItem <blue>" + itemId + "</blue>: <aqua>" + texture);
                Logs.logWarning("Texture-paths must only contain characters <yellow>[a-z0-9/._-]");
            }
        }
    }

    private Key getItemModelFromConfigurationSection(String itemId, ConfigurationSection packSection) {
        String model = packSection.getString("model", "");
        if (model.isEmpty()) model = itemId;

        if (Key.parseable(model)) return Key.key(model);
        else return KeyUtils.MALFORMED_KEY_PLACEHOLDER;
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
