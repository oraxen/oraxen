package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public enum Settings {
    // Generic Plugin stuff
    DEBUG("debug"),
    PLUGIN_LANGUAGE("Plugin.language"),
    KEEP_UP_TO_DATE("Plugin.keep_this_up_to_date"),
    REPAIR_COMMAND_ORAXEN_DURABILITY("Plugin.commands.repair.oraxen_durability_only"),
    GENERATE_DEFAULT_ASSETS("Plugin.generation.default_assets"),
    GENERATE_DEFAULT_CONFIGS("Plugin.generation.default_configs"),
    WORLDEDIT_NOTEBLOCKS("Plugin.worldedit.noteblock_mechanic"),
    WORLDEDIT_STRINGBLOCKS("Plugin.worldedit.stringblock_mechanic"),
    FORMAT_INVENTORY_TITLES("Plugin.formatting.inventory_titles"),
    FORMAT_TITLES("Plugin.formatting.titles"),
    FORMAT_SUBTITLES("Plugin.formatting.subtitles"),
    FORMAT_ACTION_BAR("Plugin.formatting.action_bar"),
    FORMAT_ANVIL("Plugin.formatting.anvil"),
    FORMAT_SIGNS("Plugin.formatting.signs"),
    FORMAT_CHAT("Plugin.formatting.chat"),
    FORMAT_BOOKS("Plugin.formatting.books"),
    VERIFY_PACK_FILES("Plugin.experimental.verify_pack_files"),
    EXCLUDE_MALFORMED_ATLAS("Plugin.experimental.exclude_malformed_from_atlas"),

    // Config Tools
    CONFIGS_VERSION("configs_version"),
    UPDATE_CONFIGS("ConfigsTools.enable_configs_updater"),
    DISABLE_AUTOMATIC_MODEL_DATA("ConfigsTools.disable_automatic_model_data"),
    DISABLE_AUTOMATIC_GLYPH_CODE("ConfigsTools.disable_automatic_glyph_code"),
    SKIPPED_MODEL_DATA_NUMBERS("ConfigsTools.skipped_model_data_numbers"),
    ERROR_ITEM("ConfigsTools.error_item"),

    DISABLE_LEATHER_REPAIR_CUSTOM("CustomArmor.disable_leather_repair"),
    CUSTOM_ARMOR_SHADER_TYPE("CustomArmor.shader_type"),
    GESTURES_ENABLED("Gestures.enabled"),

    // ItemUpdater
    UPDATE_ITEMS("ItemUpdater.update_items"),
    UPDATE_ITEMS_ON_RELOAD("ItemUpdater.update_items_on_reload"),
    OVERRIDE_RENAMED_ITEMS("ItemUpdater.override_renamed_items"),
    OVERRIDE_ITEM_LORE("ItemUpdater.override_item_lore"),

    // FurnitureUpdater
    UPDATE_FURNITURE("FurnitureUpdater.update_furniture"),
    UPDATE_FURNITURE_ON_RELOAD("FurnitureUpdater.update_furniture_on_reload"),
    UPDATE_FURNITURE_ON_LOAD("FurnitureUpdater.update_furniture_on_load"),
    FURNITURE_UPDATE_DELAY("FurnitureUpdater.furniture_update_delay_in_seconds"),
    EXPERIMENTAL_FURNITURE_TYPE_UPDATE("FurnitureUpdater.experimental_furniture_type_update"),
    EXPERIMENTAL_FIX_BROKEN_FURNITURE("FurnitureUpdater.experimental_fix_broken_furniture"),

    //Misc
    RESET_RECIPES("Misc.reset_recipes"),
    ADD_RECIPES_TO_BOOK("Misc.add_recipes_to_book"),
    UNICODE_COMPLETIONS("Misc.unicode_completions"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display"),
    CROSSBOW_DISPLAY("Misc.crossbow_display"),
    HIDE_SCOREBOARD_NUMBERS("Misc.hide_scoreboard_numbers"),

    //Pack
    GENERATE("Pack.generation.generate"),
    EXCLUDED_FILE_EXTENSIONS("Pack.generation.excluded_file_extensions"),
    GENERATE_ATLAS_FILE("Pack.generation.atlas.generate"),
    ATLAS_GENERATION_TYPE("Pack.generation.atlas.type"),
    GENERATE_MODEL_BASED_ON_TEXTURE_PATH("Pack.generation.auto_generated_models_follow_texture_path"),
    ARMOR_RESOLUTION("Pack.generation.armor_resolution"),
    ANIMATED_ARMOR_FRAMERATE("Pack.generation.animated_armor_framerate"),
    GENERATE_ARMOR_SHADER_FILES("Pack.generation.generate_armor_shader_files"),
    GENERATE_CUSTOM_ARMOR_TEXTURES("Pack.generation.generate_custom_armor_textures"),
    AUTOMATICALLY_GENERATE_SHADER_COMPATIBLE_ARMOR("Pack.generation.automatically_generate_shader_compatible_armor"),
    COMPRESSION("Pack.generation.compression"),
    PROTECTION("Pack.generation.protection"),
    COMMENT("Pack.generation.comment"),
    MERGE_DUPLICATE_FONTS("Pack.import.merge_duplicate_fonts"),
    MERGE_DUPLICATES("Pack.import.merge_duplicates"),
    RETAIN_CUSTOM_MODEL_DATA("Pack.import.retain_custom_model_data"),
    MERGE_ITEM_MODELS("Pack.import.merge_item_base_models"),

    UPLOAD_TYPE("Pack.upload.type"),
    UPLOAD("Pack.upload.enabled"),
    UPLOAD_OPTIONS("Pack.upload.options"),

    POLYMATH_SERVER("Pack.upload.polymath.server"),

    SEND_PACK("Pack.dispatch.send_pack"),
    SEND_ON_RELOAD("Pack.dispatch.send_on_reload"),
    SEND_PACK_DELAY("Pack.dispatch.delay"),
    SEND_PACK_ADVANCED("Pack.dispatch.send_pack_advanced.enabled"),
    SEND_PACK_ADVANCED_MANDATORY("Pack.dispatch.send_pack_advanced.mandatory"),
    SEND_PACK_ADVANCED_MESSAGE("Pack.dispatch.send_pack_advanced.message"),
    SEND_JOIN_MESSAGE("Pack.dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("Pack.dispatch.join_message.delay"),

    RECEIVE_ENABLED("Pack.receive.enabled"),
    RECEIVE_ALLOWED_ACTIONS("Pack.receive.accepted.actions"),
    RECEIVE_LOADED_ACTIONS("Pack.receive.loaded.actions"),
    RECEIVE_FAILED_ACTIONS("Pack.receive.failed_download.actions"),
    RECEIVE_DENIED_ACTIONS("Pack.receive.denied.actions"),

    // Inventory
    ORAXEN_INV_LAYOUT("oraxen_inventory.menu_layout"),
    ORAXEN_INV_ROWS("oraxen_inventory.menu_rows"),
    ORAXEN_INV_TITLE("oraxen_inventory.main_menu_title"),
    ;

    private final String path;

    Settings(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return OraxenPlugin.get().getConfigsManager().getSettings().get(path);
    }
    public void setValue(Object value) {
        YamlConfiguration settingFile = OraxenPlugin.get().getConfigsManager().getSettings();
        settingFile.set(path, value);
        try {
            settingFile.save(OraxenPlugin.get().getDataFolder().getAbsoluteFile().toPath().resolve("settings.yml").toFile());
        } catch (Exception e) {
            Logs.logError("Failed to apply changes to settings.yml");
        }
    }

    @Override
    public String toString() {
        return (String) getValue();
    }

    public Boolean toBool() {
        return (Boolean) getValue();
    }

    public List<String> toStringList() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getStringList(path);
    }

    public ConfigurationSection toConfigSection() {
        return OraxenPlugin.get().getConfigsManager().getSettings().getConfigurationSection(path);
    }

}
