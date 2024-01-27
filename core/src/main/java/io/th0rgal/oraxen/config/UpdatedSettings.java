package io.th0rgal.oraxen.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum UpdatedSettings {
    GUI_INVENTORY("gui_inventory", "oraxen_inventory.menu_layout"),
    MERGE_ITEM_MODELS("Plugin.experimental.merge_item_base_models", "Pack.import.merge_item_base_models"),
    MERGE_FONTS("Plugin.experimental.merge_font_files", "Pack.import.merge_font_files"),
    GENERATE_ATLAS_FILE("Pack.generation.generate_atlas_file", "Pack.generation.atlas.generate"),
    AUTO_UPDATE_ITEMS("Misc.auto_update_items", "ItemUpdater.auto_update_items"),
    USE_NMS_GLYPHS("Plugin.experimental.nms_glyphs", "Plugin.experimental.nms.glyphs"),
    USE_NMS_GLYPHS2("Plugin.experimental.use_nms_glyphs", "Plugin.experimental.nms.glyphs"),
    SEND_PACK_ADVANCED_MANDATORY("Pack.dispatch.send_pack_advanced.mandatory", "Pack.dispatch.mandatory"),
    SEND_PACK_ADVANCED_MESSAGE("Pack.dispatch.send_pack_advanced.message", "Pack.dispatch.prompt"),
    VERIFY_PACK_FILES("Plugin.experimental.verify_pack_files", "Pack.generation.verify_pack_files"),
    EXCLUDE_MALFORMED_ATLAS("Plugin.experimental.exclude_malformed_from_atlas", "Pack.generation.atlas.exclude_malformed_from_atlas"),
    NMS_GLYPHS("Plugin.experimental.nms.glyphs", "Glyphs.nms_glyphs"),
    SHOW_PERMISSION_EMOJIS("Plugin.commands.emoji_list.only_show_emojis_with_permission", "Glyphs.emoji_list_permission_only"),
    UNICODE_COMPLETIONS("Misc.unicode_completions", "Glyphs.unicode_completions"),
    WORLDEDIT_NOTEBLOCKS("Plugin.worldedit.noteblock_mechanic", "WorldEdit.noteblock_mechanic"),
    WORLDEDIT_STRINGBLOCKS("Plugin.worldedit.stringblock_mechanic", "WorldEdit.stringblock_mechanic"),
    ;

    private final String path;
    private final String newPath;

    UpdatedSettings(String path, String newPath) {
        this.path = path;
        this.newPath = newPath;
    }

    @Override
    public String toString() {
        return this.path + ", " + newPath;
    }

    public static List<String> toStringList() {
        return Arrays.stream(UpdatedSettings.values()).map(UpdatedSettings::toString).toList();
    }

    public static Map<String, String> toStringMap() {
        return Arrays.stream(UpdatedSettings.values()).map(u -> u.toString().split(", ", 2)).collect(Collectors.toMap(e -> e[0], e -> e[1]));
    }
}
