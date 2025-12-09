package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.GlyphGrid;
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
import java.util.concurrent.ConcurrentHashMap;

public class ConfigsManager {

    /**
     * Static cache for grid glyph unicodes to preserve across reloads when
     * DISABLE_AUTOMATIC_GLYPH_CODE is true. This prevents grid glyphs from
     * getting different character assignments on each plugin reload.
     */
    private static final Map<String, List<String>> GRID_GLYPH_UNICODE_CACHE = new ConcurrentHashMap<>();

    /**
     * Clears the grid glyph unicode cache. Use this to force regeneration of
     * grid glyph unicodes. Note: This is typically not needed as the cache
     * automatically resets on server restart.
     */
    public static void clearGridGlyphCache() {
        GRID_GLYPH_UNICODE_CACHE.clear();
    }

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
                if (Settings.DEBUG.toBool())
                    e.printStackTrace();
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
            else
                tempManager.extractConfiguration("glyphs/interface.yml");
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

    private YamlConfiguration validate(ResourcesManager resourcesManager, String configName,
            YamlConfiguration defaultConfiguration) {
        File configurationFile = resourcesManager.extractConfiguration(configName);
        YamlConfiguration configuration = OraxenYaml.loadConfiguration(configurationFile);
        boolean updated = false;
        for (String key : defaultConfiguration.getKeys(true)) {
            if (!skippedYamlKeys.stream().filter(key::startsWith).toList().isEmpty())
                continue;
            if (configuration.get(key) == null) {
                updated = true;
                Message.UPDATING_CONFIG.log(AdventureUtils.tagResolver("option", key));
                configuration.set(key, defaultConfiguration.get(key));
            }
        }

        for (String key : configuration.getKeys(false))
            if (removedYamlKeys.contains(key)) {
                updated = true;
                Message.REMOVING_CONFIG.log(AdventureUtils.tagResolver("option", key));
                configuration.set(key, null);
            }

        if (updated)
            try {
                configuration.save(configurationFile);
            } catch (IOException e) {
                Logs.logError("Failed to save updated configuration file: " + configurationFile.getName());
                if (Settings.DEBUG.toBool())
                    e.printStackTrace();
            }
        return configuration;
    }

    // Skip optional keys and subkeys
    private final List<String> skippedYamlKeys = List.of(
            "oraxen_inventory.menu_layout",
            "Misc.armor_equip_event_bypass");

    private final List<String> removedYamlKeys = List.of(
            "armorpotioneffects");

    /**
     * Holds parsing state for glyph configuration processing.
     */
    private static class GlyphParseContext {
        final Map<String, Character> charPerGlyph = new HashMap<>();
        final Set<Integer> usedCodepoints = new HashSet<>();
    }

    public Collection<Glyph> parseGlyphConfigs() {
        List<File> glyphFiles = getGlyphFiles();
        List<Glyph> output = new ArrayList<>();
        GlyphParseContext ctx = new GlyphParseContext();

        collectExistingCodepoints(glyphFiles, ctx);
        parseGlyphFiles(glyphFiles, output, ctx);

        return output;
    }

    /**
     * First pass: collects all existing codepoints from glyph files.
     */
    private void collectExistingCodepoints(List<File> glyphFiles, GlyphParseContext ctx) {
        for (File file : glyphFiles) {
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection glyphSection = configuration.getConfigurationSection(key);
                if (glyphSection == null)
                    continue;
                collectCodepointsFromSection(key, glyphSection, ctx);
            }
        }
    }

    /**
     * Collects codepoints from a single glyph section.
     * Also handles legacy 'code' field migration during collection.
     */
    private void collectCodepointsFromSection(String key, ConfigurationSection section, GlyphParseContext ctx) {
        if (section.isList("chars")) {
            List<String> charsList = section.getStringList("chars");
            for (String row : charsList) {
                for (char c : row.toCharArray()) {
                    ctx.usedCodepoints.add((int) c);
                }
            }
            if (!charsList.isEmpty() && !charsList.get(0).isEmpty()) {
                ctx.charPerGlyph.put(key, charsList.get(0).charAt(0));
            }
        } else {
            // Check for legacy 'code' field first (integer codepoint)
            // This ensures correct character is used before glyph creation
            if (section.contains("code") && section.isInt("code")) {
                char character = (char) section.getInt("code");
                ctx.charPerGlyph.put(key, character);
                ctx.usedCodepoints.add((int) character);
                return;
            }

            String characterString = section.getString("char", "");
            if (!characterString.isBlank()) {
                char character = characterString.charAt(0);
                ctx.charPerGlyph.put(key, character);
                ctx.usedCodepoints.add((int) character);
            }
        }
    }

    /**
     * Second pass: creates glyphs from all glyph files.
     */
    private void parseGlyphFiles(List<File> glyphFiles, List<Glyph> output, GlyphParseContext ctx) {
        for (File file : glyphFiles) {
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            boolean fileChanged = parseGlyphsFromFile(configuration, output, ctx);
            saveConfigIfChanged(file, configuration, fileChanged);
        }
    }

    /**
     * Parses all glyphs from a single configuration file.
     *
     * @return true if the file was modified
     */
    private boolean parseGlyphsFromFile(YamlConfiguration configuration, List<Glyph> output, GlyphParseContext ctx) {
        boolean fileChanged = false;

        for (String key : configuration.getKeys(false)) {
            ConfigurationSection glyphSection = configuration.getConfigurationSection(key);
            if (glyphSection == null)
                continue;

            GlyphParseResult result = createGlyph(key, glyphSection, ctx);
            result.glyph.verifyGlyph(output);
            output.add(result.glyph);

            if (result.fileChanged)
                fileChanged = true;
        }

        return fileChanged;
    }

    /**
     * Result of creating a single glyph.
     */
    private record GlyphParseResult(Glyph glyph, boolean fileChanged) {
    }

    /**
     * Creates a glyph from a configuration section.
     */
    private GlyphParseResult createGlyph(String key, ConfigurationSection section, GlyphParseContext ctx) {
        GlyphGrid grid = GlyphGrid.fromConfig(section.getConfigurationSection("grid"));

        if (section.isList("chars")) {
            return createMultiCharGlyph(key, section, grid);
        } else if (grid.isMultiCell()) {
            return createGridGlyph(key, section, grid, ctx);
        } else {
            return createSingleCharGlyph(key, section, ctx);
        }
    }

    private GlyphParseResult createMultiCharGlyph(String key, ConfigurationSection section, GlyphGrid grid) {
        List<String> unicodeRows = section.getStringList("chars");
        Glyph glyph = new Glyph(key, section, unicodeRows, grid);
        return new GlyphParseResult(glyph, false);
    }

    private GlyphParseResult createGridGlyph(String key, ConfigurationSection section, GlyphGrid grid,
            GlyphParseContext ctx) {
        List<String> unicodeRows;
        boolean fileChanged = false;

        // Check if we have cached unicodes from a previous load (for
        // DISABLE_AUTOMATIC_GLYPH_CODE support)
        List<String> cachedRows = GRID_GLYPH_UNICODE_CACHE.get(key);
        if (cachedRows != null && isValidForGrid(cachedRows, grid)) {
            // Reuse cached unicodes to preserve character assignments across reloads
            unicodeRows = cachedRows;
            // Mark these codepoints as used to prevent conflicts with other glyphs
            for (String row : unicodeRows) {
                for (char c : row.toCharArray()) {
                    ctx.usedCodepoints.add((int) c);
                }
            }
        } else {
            // Generate new unicodes
            unicodeRows = generateGridUnicodes(grid, ctx.usedCodepoints);

            if (Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
                // Cache for future reloads within this server session
                GRID_GLYPH_UNICODE_CACHE.put(key, unicodeRows);
                Logs.logWarning("Grid glyph '" + key + "' has no 'chars' list defined but " +
                        Settings.DISABLE_AUTOMATIC_GLYPH_CODE.getPath() + " is enabled.");
                Logs.logWarning(
                        "Characters will be preserved within this server session, but will change on server restart.");
                Logs.logWarning("To ensure consistent characters across restarts, either disable " +
                        Settings.DISABLE_AUTOMATIC_GLYPH_CODE.getPath() +
                        " or manually add a 'chars' list to this glyph's configuration.", true);
            } else {
                section.set("chars", unicodeRows);
                fileChanged = true;
            }
        }

        Glyph glyph = new Glyph(key, section, unicodeRows, grid);
        glyph.setFileChanged(fileChanged);
        return new GlyphParseResult(glyph, fileChanged);
    }

    /**
     * Validates that cached unicode rows match the expected grid dimensions.
     * If the grid size changed in config, cached unicodes are invalid.
     *
     * @param rows The cached unicode rows
     * @param grid The current grid configuration
     * @return true if the cached rows match the grid dimensions
     */
    private boolean isValidForGrid(List<String> rows, GlyphGrid grid) {
        if (rows.size() != grid.rows())
            return false;
        for (String row : rows) {
            if (row.length() != grid.columns())
                return false;
        }
        return true;
    }

    private GlyphParseResult createSingleCharGlyph(String key, ConfigurationSection section, GlyphParseContext ctx) {
        char character = ctx.charPerGlyph.getOrDefault(key, Character.MIN_VALUE);
        if (character == Character.MIN_VALUE) {
            character = findNextCodepoint(ctx.usedCodepoints, 42000);
            ctx.charPerGlyph.put(key, character);
            ctx.usedCodepoints.add((int) character);
        }
        Glyph glyph = new Glyph(key, section, character);
        return new GlyphParseResult(glyph, glyph.isFileChanged());
    }

    private void saveConfigIfChanged(File file, YamlConfiguration configuration, boolean fileChanged) {
        if (fileChanged && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
            try {
                configuration.save(file);
            } catch (IOException e) {
                Logs.logWarning("Failed to save updated glyph file: " + file.getName());
                if (Settings.DEBUG.toBool())
                    e.printStackTrace();
            }
        }
    }

    /**
     * Generates unicode characters for a grid-based glyph.
     *
     * @param grid           The grid configuration
     * @param usedCodepoints Set of already used codepoints (will be modified)
     * @return List of strings, one per row, each containing columns characters
     */
    private List<String> generateGridUnicodes(GlyphGrid grid, Set<Integer> usedCodepoints) {
        List<String> rows = new ArrayList<>();

        for (int row = 0; row < grid.rows(); row++) {
            StringBuilder rowBuilder = new StringBuilder();
            for (int col = 0; col < grid.columns(); col++) {
                char nextChar = findNextCodepoint(usedCodepoints, 42000);
                usedCodepoints.add((int) nextChar);
                rowBuilder.append(nextChar);
            }
            rows.add(rowBuilder.toString());
        }

        return rows;
    }

    /**
     * Finds the next unused codepoint starting from a minimum value.
     *
     * @param usedCodepoints Set of already used codepoints
     * @param min            Minimum codepoint value to start searching from
     * @return The next available character
     */
    private char findNextCodepoint(Set<Integer> usedCodepoints, int min) {
        while (usedCodepoints.contains(min))
            min++;
        return (char) min;
    }

    public Map<File, Map<String, ItemBuilder>> parseItemConfig() {
        Map<File, Map<String, ItemBuilder>> parseMap = new LinkedHashMap<>();
        ItemBuilder errorItem = new ItemParser(Settings.ERROR_ITEM.toConfigSection()).buildItem();
        for (File file : getItemFiles())
            parseMap.put(file, parseItemConfig(file, errorItem));
        return parseMap;
    }

    public void assignAllUsedModelDatas() {
        Map<Material, Map<Integer, String>> assignedModelDatas = new HashMap<>();
        for (File file : getItemFiles()) {
            if (!file.exists())
                continue;
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            boolean fileChanged = false;

            for (String key : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(key);
                if (itemSection == null)
                    continue;
                ConfigurationSection packSection = itemSection.getConfigurationSection("Pack");
                Material material = Material.getMaterial(itemSection.getString("material", ""));
                if (packSection == null || material == null)
                    continue;
                int modelData = packSection.getInt("custom_model_data", -1);
                String model = getItemModelFromConfigurationSection(packSection);
                if (modelData == -1)
                    continue;
                if (assignedModelDatas.containsKey(material)
                        && assignedModelDatas.get(material).containsKey(modelData)) {
                    if (assignedModelDatas.get(material).get(modelData).equals(model))
                        continue;
                    Logs.logError("CustomModelData " + modelData
                            + " is already assigned to another item with this material but different model");
                    if (file.getAbsolutePath()
                            .equals(DuplicationHandler.getDuplicateItemFile(material).getAbsolutePath())
                            && Settings.RETAIN_CUSTOM_MODEL_DATA.toBool()) {
                        Logs.logWarning("Due to " + Settings.RETAIN_CUSTOM_MODEL_DATA.getPath() + " being enabled,");
                        Logs.logWarning("the model data will not removed from " + file.getName() + ": " + key + ".");
                        Logs.logWarning("There will still be a conflict which you need to solve yourself.");
                        Logs.logWarning(
                                "Either reset the CustomModelData of this item, or change the CustomModelData of the conflicting item.",
                                true);
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

            if (fileChanged)
                try {
                    configuration.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public void parseAllItemTemplates() {
        for (File file : getItemFiles()) {
            if (file == null || !file.exists())
                continue;
            YamlConfiguration configuration = OraxenYaml.loadConfiguration(file);
            for (String key : configuration.getKeys(false)) {
                ConfigurationSection itemSection = configuration.getConfigurationSection(key);
                if (itemSection != null && itemSection.isBoolean("template"))
                    ItemTemplate.register(itemSection);
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
            if (itemSection == null || ItemTemplate.isTemplate(itemKey))
                continue;
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
                if (Settings.DEBUG.toBool())
                    e.printStackTrace();
                else
                    Logs.logWarning(e.getMessage());
            }
            if (itemParser.isConfigUpdated())
                configUpdated = true;
        }

        if (configUpdated) {
            String content = config.saveToString();
            if (VersionUtil.atOrAbove("1.20.5"))
                content = content.replace("displayname: ", "itemname: ");
            else
                content = content.replace("itemname: ", "displayname: ");

            try {
                FileUtils.writeStringToFile(itemFile, content, StandardCharsets.UTF_8);
            } catch (Exception e) {
                if (Settings.DEBUG.toBool())
                    e.printStackTrace();
                try {
                    config.save(itemFile);
                } catch (Exception e1) {
                    if (Settings.DEBUG.toBool())
                        e1.printStackTrace();
                }
            }
        }

        return map;
    }

    private List<File> getItemFiles() {
        if (itemsFolder == null || !itemsFolder.exists())
            return new ArrayList<>();
        return FileUtils.listFiles(itemsFolder, new String[] { "yml" }, true).stream().filter(OraxenYaml::isValidYaml)
                .sorted().toList();
    }

    private List<File> getGlyphFiles() {
        if (glyphsFolder == null || !glyphsFolder.exists())
            return new ArrayList<>();
        return FileUtils.listFiles(glyphsFolder, new String[] { "yml" }, true).stream().filter(OraxenYaml::isValidYaml)
                .sorted().toList();
    }
}
