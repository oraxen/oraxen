package io.th0rgal.oraxen.configs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum UpdatedSettings {
    GUI_INVENTORY("gui_inventory", "inventory.layout"),
    MERGE_ITEM_MODELS("Plugin.experimental.merge_item_base_models", "pack.import.merge.item-base-models"),
    MERGE_FONTS("Plugin.experimental.merge_font_files", "pack.import.merge.fonts"),
    GENERATE_ATLAS_FILE("Pack.generation.generate_atlas_file", "pack.generation.atlas.generate"),
    AUTO_UPDATE_ITEMS("Misc.auto_update_items", "updater.items.update-items"),
    SEND_PACK_ADVANCED_MANDATORY("Pack.dispatch.send_pack_advanced.mandatory", "pack.dispatch.mandatory"),
    SEND_PACK_ADVANCED_MESSAGE("Pack.dispatch.send_pack_advanced.message", "pack.dispatch.prompt"),
    VERIFY_PACK_FILES("Plugin.experimental.verify_pack_files", "pack.verification"),
    EXCLUDE_MALFORMED_ATLAS("Plugin.experimental.exclude_malformed_from_atlas", "pack.generation.atlas.exclude-malformed-from-atlas"),
    SHOW_PERMISSION_EMOJIS("Plugin.commands.emoji_list.only_show_emojis_with_permission", "glyphs.emoji-list-permission-only"),
    UNICODE_COMPLETIONS("Misc.unicode_completions", "glyphs.unicode-completions"),
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
