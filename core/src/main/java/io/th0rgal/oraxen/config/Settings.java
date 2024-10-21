package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontEvents;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.pack.PackObfuscator;
import io.th0rgal.oraxen.pack.server.PackServerType;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.EnumUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum Settings {
    // Generic Plugin stuff
    DEBUG("debug", false, "test"),
    PLUGIN_LANGUAGE("Plugin.language", "english"),
    KEEP_UP_TO_DATE("Plugin.keep_this_up_to_date", true),
    FORMAT_INVENTORY_TITLES("Plugin.formatting.inventory_titles", true),
    FORMAT_TITLES("Plugin.formatting.titles", true),
    FORMAT_SUBTITLES("Plugin.formatting.subtitles", true),
    FORMAT_ACTION_BAR("Plugin.formatting.action_bar", true),
    FORMAT_SCOREBOARD("Plugin.formatting.scoreboard", true),
    FORMAT_ANVIL("Plugin.formatting.anvil", true),
    FORMAT_SIGNS("Plugin.formatting.signs", true),
    FORMAT_CHAT("Plugin.formatting.chat", true),
    FORMAT_BOOKS("Plugin.formatting.books", true),

    // WorldEdit
    WORLDEDIT_NOTEBLOCKS("WorldEdit.noteblock_mechanic", false),
    WORLDEDIT_STRINGBLOCKS("WorldEdit.stringblock_mechanic", false),
    WORLDEDIT_FURNITURE("WorldEdit.furniture_mechanic", false),

    // Glyphs
    GLYPH_HANDLER("Glyphs.glyph_handler", GlyphHandlers.GlyphHandler.VANILLA.name()),
    SHOW_PERMISSION_EMOJIS("Glyphs.emoji_list_permission_only", true),
    UNICODE_COMPLETIONS("Glyphs.unicode_completions", true),
    GLYPH_HOVER_TEXT("Glyphs.chat_hover_text", "<glyph_placeholder>"),
    SHIFT_FONT("Glyphs.shift_font", "minecraft:default"),

    // Chat
    CHAT_HANDLER("Chat.chat_handler", VersionUtil.isPaperServer() ? FontEvents.ChatHandler.MODERN.name() : FontEvents.ChatHandler.LEGACY.name()),

    // Config Tools
    //CONFIGS_VERSION("configs_version", "false"),
    UPDATE_CONFIGS("ConfigsTools.enable_configs_updater", true),
    GENERATE_DEFAULT_CONFIGS("ConfigTools.generate_default_configs", true),
    DISABLE_AUTOMATIC_MODEL_DATA("ConfigsTools.disable_automatic_model_data", false),
    DISABLE_AUTOMATIC_GLYPH_CODE("ConfigsTools.disable_automatic_glyph_code", false),
    SKIPPED_MODEL_DATA_NUMBERS("ConfigsTools.skipped_model_data_numbers", List.of()),
    ERROR_ITEM("ConfigsTools.error_item", Map.of("material", Material.PODZOL.name(), "excludeFromInventory", false, "injectID", false)),

    // Custom Armor
    CUSTOM_ARMOR_ENABLED("CustomArmor.enabled", true),
    CUSTOM_ARMOR_TRIMS_ASSIGN("CustomArmor.auto_assign_settings", true),

    // Custom Blocks
    LEGACY_NOTEBLOCKS("CustomBlocks.use_legacy_noteblocks", false),
    LEGACY_STRINGBLOCKS("CustomBlocks.use_legacy_stringblocks", false),

    // ItemUpdater
    UPDATE_ITEMS("ItemUpdater.update_items", true),
    UPDATE_ITEMS_ON_RELOAD("ItemUpdater.update_items_on_reload", true),
    OVERRIDE_RENAMED_ITEMS("ItemUpdater.override_renamed_items", false),
    OVERRIDE_ITEM_LORE("ItemUpdater.override_item_lore", false),

    //Misc
    RESET_RECIPES("Misc.reset_recipes", true),
    ADD_RECIPES_TO_BOOK("Misc.add_recipes_to_book", true),
    HIDE_SCOREBOARD_NUMBERS("Misc.hide_scoreboard_numbers", false),
    HIDE_SCOREBOARD_BACKGROUND("Misc.hide_scoreboard_background", false),
    HIDE_TABLIST_BACKGROUND("Misc.hide_tablist_background"),

    //Pack
    PACK_GENERATE("Pack.generation.generate", true),
    PACK_ZIP("Pack.generation.generate_zip", true),
    PACK_OBFUSCATION_TYPE("Pack.obfuscation.type", PackObfuscator.PackObfuscationType.SIMPLE.name()),
    PACK_CACHE_OBFUSCATION("Pack.obfuscation.cache", true),
    PACK_IMPORT_DEFAULT("Pack.import.default_assets", true),
    PACK_IMPORT_EXTERNAL("Pack.import.external_packs", true),
    PACK_IMPORT_MODEL_ENGINE("Pack.import.modelengine", true),
    PACK_EXCLUDED_FILE_EXTENSIONS("Pack.generation.excluded_file_extensions", List.of(".zip")),
    FIX_FORCE_UNICODE_GLYPHS("Pack.generation.fix_force_unicode_glyphs", true),

    COMMENT("Pack.generation.comment", """
            The content of this resourcepack
            belongs to the owner of the Oraxen
            plugin and any complete or partial
            use must comply with the terms and
            conditions of Oraxen
            """.trim()),

    PACK_SERVER_TYPE("Pack.server.type", PackServerType.POLYMATH.name()),
    SELFHOST_PACK_SERVER_PORT("Pack.server.selfhost.port", 8082),
    SELFHOST_PUBLIC_ADDRESS("Pack.server.selfhost.public_address"),
    POLYMATH_SERVER("Pack.server.polymath.server", "atlas.oraxen.com"),
    POLYMATH_SECRET("Pack.server.polymath.secret", "oraxen"),

    PACK_SEND_PRE_JOIN("Pack.dispatch.send_pre_join", VersionUtil.isPaperServer()),
    PACK_SEND_ON_JOIN("Pack.dispatch.send_on_join", !VersionUtil.isPaperServer()),
    PACK_SEND_RELOAD("Pack.dispatch.send_on_reload", true),
    PACK_SEND_DELAY("Pack.dispatch.delay", -1),
    PACK_SEND_MANDATORY("Pack.dispatch.mandatory", true),
    PACK_SEND_PROMPT("Pack.dispatch.prompt", "<#fa4943>Accept the pack to enjoy a full <b><gradient:#9055FF:#13E2DA>Oraxen</b><#fa4943> experience"),


    // Inventory
    ORAXEN_INV_LAYOUT("oraxen_inventory.menu_layout", Map.of(
            "armors", Map.of("slot", 1, "icon", "emerald_chestplate", "displayname", "<green>Armors", "title", "<main_menu_title><#362753><glyph:menu_items_overlay:colorable>"),
            "blocks", Map.of("slot", 2, "icon", "orax_ore", "displayname", "<green>Blocks", "title", "<main_menu_title><#EDCDEB><glyph:menu_items_overlay:colorable>"),
            "furniture", Map.of("slot", 3, "icon", "chair", "displayname", "<green>Furniture", "title", "<main_menu_title><#F2F2F2><glyph:menu_items_overlay:colorable>"),
            "flowers", Map.of("slot", 4, "icon", "dailily", "displayname", "<green>Flowers", "title", "<main_menu_title><#bf332c><glyph:menu_items_overlay:colorable>"),
            "hats", Map.of("slot", 5, "icon", "crown", "displayname", "<green>Hats", "title", "<main_menu_title><#81B125><glyph:menu_items_overlay:colorable>"),
            "items", Map.of("slot", 6, "icon", "ruby", "displayname", "<green>Items", "title", "<main_menu_title><#DA2E45><glyph:menu_items_overlay:colorable>"),
            "mystical", Map.of("slot", 7, "icon", "legendary_hammer", "displayname", "<green>Mystical", "title", "<main_menu_title><#9AB2E4><glyph:menu_items_overlay:colorable>"),
            "plants", Map.of("slot", 8, "icon", "weed_leaf", "displayname", "<green>Plants", "title", "<main_menu_title><#44C886><glyph:menu_items_overlay:colorable>"),
            "tools", Map.of("slot", 9, "icon", "iron_serpe", "displayname", "<green>Tools", "title", "<main_menu_title><#FFFFFF><glyph:menu_items_overlay:colorable>"),
            "weapons", Map.of("slot", 10, "icon", "energy_crystal_sword", "displayname", "<green>Weapons", "title", "<main_menu_title><#2FB6FF><glyph:menu_items_overlay:colorable>")

    )),
    ORAXEN_INV_ROWS("oraxen_inventory.menu_rows", 6),
    ORAXEN_INV_SIZE("oraxen_inventory.menu_size",  45),
    ORAXEN_INV_TITLE("oraxen_inventory.main_menu_title", "<shift:-18><glyph:menu_items><shift:-193>"),
    ORAXEN_INV_NEXT_ICON("oraxen_inventory.next_page_icon"),
    ORAXEN_INV_PREVIOUS_ICON("oraxen_inventory.previous_page_icon"),
    ORAXEN_INV_EXIT("oraxen_inventory.exit_icon");

    private final String path;
    private final Object defaultValue;
    private List<String> comments = Collections.emptyList();
    private List<String> inlineComments = Collections.emptyList();
    private Component richComment = Component.empty();

    Settings(String path) {
        this.path = path;
        this.defaultValue = null;
    }

    Settings(String path, Object defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    Settings(String path, Object defaultValue, String... comments) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.comments = List.of(comments);
    }

    Settings(String path, Object defaultValue, List<String> comments, String... inlineComments) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.comments = comments;
        this.inlineComments = List.of(inlineComments);
    }

    Settings(String path, Object defaultValue, Component richComment) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.richComment = richComment;
    }

    public String getPath() {
        return path;
    }
    public Object defaultValue() {
        return defaultValue;
    }

    public List<String> comments() {
        return comments;
    }

    public List<String> inlineComments() {
        return inlineComments;
    }

    public Component richComment() {
        return richComment;
    }

    public Object getValue() {
        return OraxenPlugin.get().configsManager().getSettings().get(path);
    }
    public void setValue(Object value) { setValue(value, true); }
    public void setValue(Object value, boolean save) {
        YamlConfiguration settingFile = OraxenPlugin.get().configsManager().getSettings();
        settingFile.set(path, value);
        try {
            if (save) settingFile.save(OraxenPlugin.get().getDataFolder().toPath().resolve("settings.yml").toFile());
        } catch (Exception e) {
            Logs.logError("Failed to apply changes to settings.yml");
        }
    }

    public String toString(String optionalDefault) {
        return getValue() == null ? optionalDefault : (String) getValue();
    }

    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

    @FunctionalInterface
    public interface MissingHandler<T> {
        T handle(String value);
    }

    public <E extends Enum<E>> E toEnum(Class<E> enumClass, E defaultValue) {
        return EnumUtils.getEnum(enumClass, toString(), defaultValue);
    }

    public <E extends Enum<E>> E toEnumOrGet(Class<E> enumClass, Function<String, ? extends E> function) {
        return EnumUtils.getEnumOrElse(enumClass, toString(), function);
    }

    public Component toComponent() {
        return AdventureUtils.MINI_MESSAGE.deserialize(getValue().toString());
    }

    public Integer toInt() {
        return toInt(-1);
    }

    /**
     * @param optionalDefault value to return if the path is not an integer
     * @return the value of the path as an int, or the default value if the path is not found
     */
    public Integer toInt(int optionalDefault) {
        try {
            return Integer.parseInt(getValue().toString());
        } catch (NumberFormatException | NullPointerException e) {
            return optionalDefault;
        }
    }

    public Boolean toBool(boolean defaultValue) {
        return getValue() == null ? defaultValue : (Boolean) getValue();
    }

    public Boolean toBool() {
        return (Boolean) getValue();
    }

    public List<String> toStringList() {
        return OraxenPlugin.get().configsManager().getSettings().getStringList(path);
    }

    public ConfigurationSection toConfigSection() {
        return OraxenPlugin.get().configsManager().getSettings().getConfigurationSection(path);
    }

    public static YamlConfiguration validateSettings() {
        File settingsFile = OraxenPlugin.get().getDataFolder().toPath().resolve("settings.yml").toFile();
        YamlConfiguration settings = settingsFile.exists() ? OraxenYaml.loadConfiguration(settingsFile) : new YamlConfiguration();
        settings.options().copyDefaults(true).indent(2).parseComments(true);
        YamlConfiguration defaults = defaultSettings();

        settings.addDefaults(defaults);

        try {
            settingsFile.createNewFile();
            settings.save(settingsFile);
        } catch (IOException e) {
            if (settings.getBoolean("debug")) e.printStackTrace();
        }

        return settings;
    }

    private static YamlConfiguration defaultSettings() {
        YamlConfiguration defaultSettings = new YamlConfiguration();
        defaultSettings.options().copyDefaults(true).indent(4).parseComments(true);

        for (Settings setting : Settings.values()) {
            defaultSettings.set(setting.getPath(), setting.defaultValue());
            defaultSettings.setComments(setting.getPath(), setting.comments());
            defaultSettings.setInlineComments(setting.getPath(), setting.inlineComments());
            //defaultSettings.setRichMessage(setting.getPath(), setting.richComment());
        }

        return defaultSettings;
    }

}
