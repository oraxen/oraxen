package io.th0rgal.oraxen.configs;

import java.util.Arrays;
import java.util.List;

public enum RemovedSettings {
    CONVERT_PACK_FOR_1_19_3("Plugin.experimental.convert_pack_for_1_19_3"),
    INVULNERABLE_DURING_PACK_LOADING("Pack.dispatch.invulnerable_during_pack_loading"),
    ATTEMPT_TO_MIGRATE_DUPLICATES("Pack.generation.attempt_to_migrate_duplicates"),
    PACK_SLICER("Pack.generation.texture_slicer"),
    ORAXEN_INV_TEXTURE("oraxen_inventory.menu_glyph"),
    ORAXEN_INV_TEXTURE_OVERLAY("oraxen_inventory.menu_overlay_glyph"),
    ORAXEN_INV_EXIT_ICON("oraxen_inventory.exit_icon"),
    ORAXEN_INV_NEXT_PAGE_ICON("oraxen_inventory.next_page_icon"),
    ORAXEN_INV_PREVIOUS_PAGE_ICON("oraxen_inventory.previous_page_icon"),
    AUTOMATICALLY_SET_MODEL_DATA("ConfigsTools.automatically_set_model_data"),
    AUTOMATICALLY_SET_GLYPH_CODE("ConfigsTools.automatically_set_glyph_code"),
    DISABLE_AUTOMATIC_MODEL_DATA("ConfigsTools.disable_automatic_model_data"),
    SKIPPED_MODEL_DATA_NUMBERS("ConfigsTools.skipped_model_data_numbers"),
    MERGE_FONTS("Pack.import.merge_font_files"),
    AUTO_UPDATE_ITEMS("ItemUpdater.auto_update_items"),
    OVERRIDE_LORE("ItemUpdater.override_lore"),
    UPDATE_FURNITURE_ON_RELOAD("ItemUpdater.update_furniture_on_reload"),
    UPDATE_FURNITURE_ON_LOAD("ItemUpdater.update_furniture_on_load"),
    FURNITURE_UPDATE_DELAY("ItemUpdater.furniture_update_delay_in_seconds"),
    FURNITURE_UPDATE_DELAY2("FurnitureUpdater.furniture_update_delay_in_seconds"),
    UPDATE_FURNITURE_ON_LOAD2("FurnitureUpdater.update_furniture_on_load"),
    UPDATE_FURNITURE_ON_RELOAD2("FurnitureUpdater.update_furniture_on_reload"),
    SEND_PACK_ADVANCED("Pack.dispatch.send_pack_advanced"),
    SEND_PRE_JOIN("Pack.dispatch.send_pre_join"),
    SEND_ON_JOIN("Pack.dispatch.send_on_join"),
    SEND_PACK("Pack.dispatch.send_pack"),
    SEND_ON_RELOAD("Pack.dispatch.send_on_reload"),
    DISPATCH_STOP("Pack.dispatch.stop"),
    DISABLE_MOVEMENT_ON_LOAD("Pack.dispatch.disable_movement_on_load"),
    DISABLE_DAMAGE_ON_LOAD("Pack.dispatch.disable_damage_on_load"),
    SEND_JOIN_MESSAGE("Pack.dispatch.join_message"),
    NMS_BLOCK_CORRECTION("Plugin.experimental.nms.block_correction"),
    SPIGOT_CHAT_FORMATTING("Plugin.experimental.spigot_chat_formatting"),
    CHAT_HANDLER("Chat.chat_handler"),
    ORAXEN_INV_TYPE("oraxen_inventory.main_menu_type"),
    GESTURES_ENABLED("Gestures.enabled"),
    CONFIGS_VERSION("configs_version"),
    ENABLE_CONFIGS_UPDATER("ConfigsTools.enable_configs_updater"),
    LEGACY_NOTEBLOCKS("CustomBlocks.use_legacy_noteblocks"),
    BLOCK_CORRECTION("CustomBlocks.block_correction"),
    NMS_GLYPHS("Glyphs.nms_glyphs"),
    NMS_GLYPHS_EXPERIMENTAL("Plugin.experimental.nms.glyphs"),
    NMS_GLYPHS_LEGACY("Plugin.experimental.nms_glyphs"),
    NMS_GLYPHS_LEGACY_2("Plugin.experimental.use_nms_glyphs"),
    GLYPH_HANDLER("Glyphs.glyph_handler"),
    CUSTOM_ARMOR_SHADER_SETTINGS("CustomArmor.shader_settings"),
    REPAIR_COMMAND_ORAXEN_DURABILITY("Plugin.commands.repair.oraxen_durability_only"),
    REPAIR_COMMAND("Plugin.commands.repair"),
    WORLDEDIT_STRINGBLOCKS("WorldEdit.stringblock_mechanic"),
    WORLDEDIT_NOTEBLOCKS("WorldEdit.noteblock_mechanic"),
    WORLDEDIT_FURNITURE("WorldEdit.furniture_mechanic"),
    WORLDEDIT("WorldEdit")

    ;

    private final String path;

    RemovedSettings(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return this.path;
    }

    public static List<String> toStringList() {
        return Arrays.stream(RemovedSettings.values()).map(RemovedSettings::toString).toList();
    }
}
