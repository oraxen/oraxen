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
