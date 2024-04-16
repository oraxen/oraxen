package io.th0rgal.oraxen.config;

import java.util.Arrays;
import java.util.List;

public enum RemovedSettings {
    CONVERT_PACK_FOR_1_19_3("Plugin.experimental.convert_pack_for_1_19_3"),
    INVULNERABLE_DURING_PACK_LOADING("Pack.dispatch.invulnerable_during_pack_loading"),
    ATTEMPT_TO_MIGRATE_DUPLICATES("Pack.generation.attempt_to_migrate_duplicates"),
    ORAXEN_INV_TEXTURE("oraxen_inventory.menu_glyph"),
    ORAXEN_INV_TEXTURE_OVERLAY("oraxen_inventory.menu_overlay_glyph"),
    AUTOMATICALLY_SET_MODEL_DATA("ConfigsTools.automatically_set_model_data"),
    AUTOMATICALLY_SET_GLYPH_CODE("ConfigsTools.automatically_set_glyph_code"),
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
    SHIELD_DISPLAY("Misc.shield_display"),
    BOW_DISPLAY("Misc.bow_display"),
    CROSSBOW_DISPLAY("Misc.crossbow_display"),
    GENERATE_ATLAS_FILE("Pack.generation.atlas.generate"),
    EXCLUDE_MALFORMED_ATLAS("Pack.generation.atlas.exclude_malformed_from_atlas"),
    ATLAS_GENERATION_TYPE("Pack.generation.atlas.type"),
    ARMOR_EQUIP_EVENT_BYPASS("Misc.armor_equip_event_bypass"),
    UPLOAD_TYPE("Pack.upload.type"),
    UPLOAD("Pack.upload.enabled"),
    UPLOAD_OPTIONS("Pack.upload.options"),
    POLYMATH_SERVER("Pack.upload.polymath.server"),

    GESTURES_ENABLED("Gestures.enabled"),

    EXPERIMENTAL_FIX_BROKEN_FURNITURE("FurnitureUpdater.experimental_fix_broken_furniture"),
    EXPERIMENTAL_FURNITURE_TYPE_UPDATE("FurnitureUpdater.experimental_furniture_type_update"),

    SEND_JOIN_MESSAGE("Pack.dispatch.join_message.enabled"),
    JOIN_MESSAGE_DELAY("Pack.dispatch.join_message.delay"),
    GENERATE_DEFAULT_ASSETS("Plugin.generation.default_assets"),

    VERIFY_PACK_FILES("Pack.generation.verify_pack_files"),
    GENERATE_MODEL_BASED_ON_TEXTURE_PATH("Pack.generation.auto_generated_models_follow_texture_path"),
    COMPRESSION("Pack.generation.compression"),
    PROTECTION("Pack.generation.protection"),

    RECEIVE_ENABLED("Pack.receive.enabled"),
    RECEIVE_ALLOWED_ACTIONS("Pack.receive.accepted.actions"),
    RECEIVE_LOADED_ACTIONS("Pack.receive.loaded.actions"),
    RECEIVE_FAILED_ACTIONS("Pack.receive.failed_download.actions"),
    RECEIVE_DENIED_ACTIONS("Pack.receive.denied.actions"),
    RECEIVE_FAILED_RELOAD_ACTIONS("Pack.receive.failed_reload.actions"),
    RECEIVE_DOWNLOADED_ACTIONS("Pack.receive.downloaded.actions"),
    RECEIVE_INVALID_URL_ACTIONS("Pack.receive.invalid_url.actions"),
    RECEIVE_DISCARDED_ACTIONS("Pack.receive.discarded.actions"),

    BLOCK_CORRECTION("CustomBlocks.block_correction"),
    DOWNLOAD_DEFAULT_ASSETS("Plugin.default_content.download_resourcepack"),

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
