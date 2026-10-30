package io.th0rgal.oraxen.configs;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public enum Settings {
    // Generic Plugin stuff
    DEBUG("debug"),
    UPDATE_CHECKER_ENABLED("general.update-checker.enabled"),
    UPDATE_CHECKER_INTERVAL("general.update-checker.interval"),
    PLUGIN_LANGUAGE("general.language"),
    KEEP_UP_TO_DATE("general.auto-update.settings"),
    INTRODUCTION_ENABLED("general.introduction.enabled"),
    INTRODUCTION_CONSOLE_SENT("general.introduction.console-sent"),
    AUTO_UPDATE_PAPER_CONFIG("general.auto-update.paper-config"),
    GENERATE_DEFAULT_ASSETS("configuration.defaults.assets"),
    GENERATE_DEFAULT_CONFIGS("configuration.defaults.configs"),
    FORMAT_INVENTORY_TITLES("general.formatting.inventory-titles"),
    FORMAT_TITLES("general.formatting.titles"),
    FORMAT_SUBTITLES("general.formatting.subtitles"),
    FORMAT_ACTION_BAR("general.formatting.action-bar"),
    FORMAT_ANVIL("general.formatting.anvil"),
    FORMAT_SIGNS("general.formatting.signs"),
    FORMAT_CHAT("general.formatting.chat"),
    FORMAT_BOOKS("general.formatting.books"),

    // Glyphs
    SHOW_PERMISSION_EMOJIS("glyphs.emoji-list-permission-only"),
    UNICODE_COMPLETIONS("glyphs.unicode-completions"),

    // Config Tools
    DISABLE_AUTOMATIC_MODEL_DATA("configuration.items.model-data.disable-automatic"),
    DISABLE_AUTOMATIC_GLYPH_CODE("glyphs.disable-automatic-code"),
    SKIPPED_MODEL_DATA_NUMBERS("configuration.items.model-data.skipped-numbers"),
    ERROR_ITEM("configuration.items.fallback"),

    // Custom Armor
    CUSTOM_ARMOR_TYPE("custom-armor.type"),
    CUSTOM_ARMOR_AUTO_SELECT_TYPE("custom-armor.auto-select-type"),
    CUSTOM_ARMOR_COMPONENT_ASSIGN("custom-armor.component.auto-assign-component"),
    DISABLE_LEATHER_REPAIR_CUSTOM("custom-armor.disable-leather-repair"),
    CUSTOM_ARMOR_TRIMS_MATERIAL("custom-armor.trims.material-replacement"),
    CUSTOM_ARMOR_TRIMS_ASSIGN("custom-armor.trims.auto-assign-settings"),

    // ItemUpdater
    UPDATE_ITEMS("updater.items.update-items"),
    UPDATE_ITEMS_ON_RELOAD("updater.items.update-on-reload"),
    UPDATE_TILE_ENTITY_CONTENTS("updater.items.update-tile-entity-contents"),
    UPDATE_ENTITY_CONTENTS("updater.items.update-entity-contents"),
    OVERRIDE_RENAMED_ITEMS("updater.items.override-renamed-items"),
    OVERRIDE_ITEM_LORE("updater.items.override-item-lore"),

    // FurnitureUpdater
    UPDATE_FURNITURE("updater.furniture.update-furniture"),
    UPDATE_FURNITURE_ON_RELOAD("updater.furniture.update-on-reload"),
    UPDATE_FURNITURE_ON_LOAD("updater.furniture.update-on-load"),
    EXPERIMENTAL_FURNITURE_TYPE_UPDATE("updater.furniture.experimental-type-update"),
    EXPERIMENTAL_FIX_BROKEN_FURNITURE("updater.furniture.experimental-fix-broken"),

    //Misc
    RESET_RECIPES("utils.recipes.reset"),
    ADD_RECIPES_TO_BOOK("utils.recipes.add-to-book"),
    ARMOR_EQUIP_EVENT_BYPASS("utils.armor-equip-event-bypass"),
    SHIELD_DISPLAY("utils.display.shield"),
    BOW_DISPLAY("utils.display.bow"),
    CROSSBOW_DISPLAY("utils.display.crossbow"),
    HIDE_SCOREBOARD_NUMBERS("utils.hide.scoreboard.numbers"),
    HIDE_SCOREBOARD_BACKGROUND("utils.hide.scoreboard.background"),
    HIDE_TABLIST_BACKGROUND("utils.hide.tablist.background"),

    //Pack
    GENERATE("pack.generation.generate"),
    UNPROTECTED_PACK_LOCATION("pack.generation.unprotected-location"),
    GENERATE_MCMETA("pack.generation.mcmeta"),
    MULTI_VERSION_PACKS("pack.generation.multi-version-packs"),
    EXCLUDED_FILE_EXTENSIONS("pack.excluded-file-extensions"),
    FIX_FORCE_UNICODE_GLYPHS("pack.generation.fix-force-unicode-glyphs"),
    VERIFY_PACK_FILES("pack.verification"),
    GENERATE_ATLAS_FILE("pack.generation.atlas.generate"),
    EXCLUDE_MALFORMED_ATLAS("pack.generation.atlas.exclude-malformed-from-atlas"),
    ATLAS_GENERATION_TYPE("pack.generation.atlas.type"),
    GENERATE_MODEL_BASED_ON_TEXTURE_PATH("pack.generation.auto-generated-models-follow-texture-path"),
    COMPRESSION("pack.compression"),
    PACK_OBFUSCATION_TYPE("pack.protection.obfuscation.type"),
    PACK_SIZE_SPOOFING("pack.protection.size-spoofing"),
    // 1.21.4+ appearance systems (can be combined)
    APPEARANCE_ITEM_PROPERTIES("pack.generation.model-definitions"),
    APPEARANCE_MODEL_DATA_IDS("pack.generation.model-data.ids"),
    APPEARANCE_MODEL_DATA_FLOAT("pack.generation.model-data.float"),
    APPEARANCE_GENERATE_PREDICATES("pack.generation.predicates"),
    // Deprecated keys (kept for backward compatibility with older configs)
    @Deprecated APPEARANCE_MODEL_DATA_FLOAT_LEGACY("pack.generation.appearance.model-data-float-legacy"),
    @Deprecated APPEARANCE_MODE("pack.generation.appearance.mode"),
    @Deprecated APPEARANCE_FORCE_PREDICATES("pack.generation.appearance.force-predicates"),
    @Deprecated APPEARANCE_ITEM_MODEL("pack.generation.appearance.item-model"),
    @Deprecated APPEARANCE_PREDICATES("pack.generation.appearance.predicates"),
    COMMENT("pack.comment"),
    IMPORT_MODEL_ENGINE("pack.import.modelengine"),
    MERGE_DUPLICATE_FONTS("pack.import.merge.fonts"),
    MERGE_DUPLICATES("pack.import.merge.duplicates"),
    RETAIN_CUSTOM_MODEL_DATA("pack.import.retain-custom-model-data"),
    MERGE_ITEM_MODELS("pack.import.merge.item-base-models"),
    REMOVE_CORE_SHADERS_FROM_IMPORTED_PACKS("pack.import.exclude.core-shaders"),

    UPLOAD_TYPE("pack.upload.type"),
    UPLOAD("pack.upload.enabled"),
    UPLOAD_OPTIONS("pack.upload.options"),

    POLYMATH_SERVER("pack.upload.polymath.server"),
    POLYMATH_SECRET("pack.upload.polymath.secret"),
    LOBFILE_API_KEY("pack.upload.lobfile.api-key"),
    LOBFILE_PACK_NAME("pack.upload.lobfile.pack-name"),

    SEND_PACK("pack.dispatch.send"),
    SEND_PACK_MODE("pack.dispatch.mode"),
    SEND_PACK_TIMEOUT("pack.dispatch.timeout"),
    DISPATCH_EXCLUDE("pack.dispatch.exclude"),
    SEND_PACK_DELAY("pack.dispatch.delay"),
    SEND_PACK_MANDATORY("pack.dispatch.mandatory"),
    SEND_PACK_PROMPT("pack.dispatch.prompt"),
    SEND_PACK_LAYER("pack.dispatch.layer"),
    DISABLE_MOVEMENT_ON_LOAD("pack.dispatch.disable.movement"),
    DISABLE_DAMAGE_ON_LOAD("pack.dispatch.disable.damage"),

    RECEIVE_ENABLED("pack.receive.enabled"),
    RECEIVE_ALLOWED_ACTIONS("pack.receive.accepted.actions"),
    RECEIVE_LOADED_ACTIONS("pack.receive.loaded.actions"),
    RECEIVE_LOADED_SOUND("pack.receive.loaded.actions.sound.enabled"),
    RECEIVE_FAILED_ACTIONS("pack.receive.failed-download.actions"),
    RECEIVE_DENIED_ACTIONS("pack.receive.denied.actions"),
    RECEIVE_FAILED_RELOAD_ACTIONS("pack.receive.failed-reload.actions"),
    RECEIVE_DOWNLOADED_ACTIONS("pack.receive.downloaded.actions"),
    RECEIVE_INVALID_URL_ACTIONS("pack.receive.invalid-url.actions"),
    RECEIVE_DISCARDED_ACTIONS("pack.receive.discarded.actions"),


    // Inventory
    ORAXEN_INV_LAYOUT("inventory.layout"),
    ORAXEN_INV_ROWS("inventory.rows"),
    ORAXEN_INV_SIZE("inventory.slots"),
    ORAXEN_INV_TITLE("inventory.title"),
    ORAXEN_INV_NEXT_ICON("inventory.next-page-icon"),
    ORAXEN_INV_PREVIOUS_ICON("inventory.previous-page-icon"),
    ORAXEN_INV_EXIT("inventory.exit-icon"),

    // Studio
    STUDIO_URL("studio.url"),
    STUDIO_SECRET("studio.secret"),
    STUDIO_AUTO_SYNC("studio.auto-sync");

    private final String path;

    /**
     * Cached result of the last YAML lookup for this setting. Hot paths (packet
     * listeners, per-event handlers) read settings on every call, so a volatile
     * field read replaces a YamlConfiguration path traversal. {@code null} means
     * "not cached"; settings that resolve to null are simply re-read each call.
     * Invalidated by {@link #invalidateCache()} whenever the settings
     * configuration is (re)loaded or mutated directly.
     */
    private volatile Object cachedValue;

    /**
     * Bumped by every cache invalidation (before the cached values are nulled)
     * so {@link #getValue()} can detect an invalidation that raced with its
     * config read and avoid re-publishing a pre-change value. Safe as a static
     * field because it is only touched from methods, never from the enum
     * constructor (which runs before static initialization).
     */
    private static final AtomicLong CACHE_GENERATION = new AtomicLong();

    Settings(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        Object value = cachedValue;
        if (value == null) {
            long generation = CACHE_GENERATION.get();
            value = OraxenPlugin.get().getConfigsManager().getSettings().get(path);
            cachedValue = value;
            // An invalidation may have raced with the config read above. Invalidators
            // bump the generation before nulling caches, so a changed generation means
            // the value read may predate the change and must not stay cached.
            if (CACHE_GENERATION.get() != generation) cachedValue = null;
        }
        return value;
    }
    public void setValue(Object value) { setValue(value, true); }
    public void setValue(Object value, boolean save) {
        YamlConfiguration settingFile = OraxenPlugin.get().getConfigsManager().getSettings();
        settingFile.set(path, value);
        CACHE_GENERATION.incrementAndGet();
        cachedValue = null;
        try {
            if (save) settingFile.save(OraxenPlugin.get().getDataFolder().toPath().resolve("settings.yml").toFile());
        } catch (Exception e) {
            Logs.logError("Failed to apply changes to settings.yml.");
        }
    }

    /**
     * Drops every cached setting value so the next read hits the freshly
     * (re)loaded settings configuration. Must be called whenever the settings
     * YamlConfiguration is reloaded or mutated without going through
     * {@link #setValue(Object, boolean)}.
     */
    public static void invalidateCache() {
        // Bump the generation first: a getValue() racing with this call will then
        // see a changed generation and discard the value it read, instead of
        // re-publishing a pre-invalidation value after the null below.
        CACHE_GENERATION.incrementAndGet();
        for (Settings setting : values()) setting.cachedValue = null;
    }

    @Override
    public String toString() {
        return (String) getValue();
    }

    public Component toComponent() {
        return AdventureUtils.MINI_MESSAGE.deserialize(getValue().toString());
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
