package io.th0rgal.oraxen.config;

import io.th0rgal.oraxen.utils.logs.Logs;

import java.util.Locale;

/**
 * Utility class for checking which item appearance systems are enabled on 1.21.4+.
 * <p>
 * Multiple systems can be enabled simultaneously:
 * <ul>
 *   <li>{@link #isItemPropertiesEnabled()} — uses {@code minecraft:item_model} component</li>
 *   <li>{@link #isModelDataIdsEnabled()} — uses {@code custom_model_data.strings[0]} with {@code minecraft:select}</li>
 *   <li>{@link #isModelDataFloatEnabled()} — uses {@code custom_model_data.floats[0]} with {@code minecraft:range_dispatch}</li>
 * </ul>
 * <p>
 * Note: {@code model_data_ids} and {@code model_data_float} cannot both be enabled
 * (they write to the same pack file path). If both are enabled, {@code model_data_ids} takes priority.
 * <p>
 * Pre-1.21.4 does not support item definitions, so Oraxen always falls back to legacy
 * predicate overrides + integer CustomModelData regardless of these settings.
 */
public final class AppearanceMode {

    private AppearanceMode() {
        // Utility class
    }

    /**
     * Whether ITEM_PROPERTIES mode is enabled.
     * Uses the minecraft:item_model component with Oraxen ids (oraxen:&lt;item_id&gt;).
     * Generates assets/oraxen/items/&lt;item_id&gt;.json.
     */
    public static boolean isItemPropertiesEnabled() {
        // Check new key first
        Boolean newKey = toBoolOrNull(Settings.APPEARANCE_ITEM_PROPERTIES);
        if (newKey != null) {
            return newKey;
        }

        // Backward compatibility: check deprecated keys
        String modeRaw = nullSafeTrim(Settings.APPEARANCE_MODE.toString());
        if (!modeRaw.isEmpty()) {
            return parseModeContains(modeRaw, "ITEM_PROPERTIES", "ITEM_MODEL", "ITEM_PROPERTY", "ITEM");
        }

        // Legacy boolean fallback
        return Settings.APPEARANCE_ITEM_MODEL.toBool();
    }

    /**
     * Whether MODEL_DATA_IDS mode is enabled.
     * Uses custom_model_data.strings[0] = "oraxen:&lt;item_id&gt;" and
     * generates assets/minecraft/items/&lt;material&gt;.json with minecraft:select.
     */
    public static boolean isModelDataIdsEnabled() {
        // Check new key first
        Boolean newKey = toBoolOrNull(Settings.APPEARANCE_MODEL_DATA_IDS);
        if (newKey != null) {
            return newKey;
        }

        // Backward compatibility: check deprecated mode key
        String modeRaw = nullSafeTrim(Settings.APPEARANCE_MODE.toString());
        if (!modeRaw.isEmpty()) {
            return parseModeContains(modeRaw, "MODEL_DATA_IDS", "MODEL_DATA_ID", "CMD_IDS", "CMD_ID", "STRINGS", "CMD_STRINGS", "MODEL_DATA");
        }

        // Legacy boolean fallback (predicates=true with new system means MODEL_DATA_IDS)
        // But only if item_model is false, otherwise default to false
        if (!Settings.APPEARANCE_ITEM_MODEL.toBool() && Settings.APPEARANCE_PREDICATES.toBool()) {
            return true;
        }

        return false;
    }

    /**
     * Whether MODEL_DATA_FLOAT mode is enabled.
     * Uses custom_model_data.floats[0] and generates assets/minecraft/items/&lt;material&gt;.json
     * with minecraft:range_dispatch. Also sets integer CustomModelData on items.
     */
    public static boolean isModelDataFloatEnabled() {
        // Check new key first
        Boolean newKey = toBoolOrNull(Settings.APPEARANCE_MODEL_DATA_FLOAT);
        if (newKey != null) {
            return newKey;
        }

        // Backward compatibility: check deprecated key (model_data_float_legacy)
        Boolean legacyKey = toBoolOrNull(Settings.APPEARANCE_MODEL_DATA_FLOAT_LEGACY);
        if (legacyKey != null) {
            return legacyKey;
        }

        // Backward compatibility: check deprecated mode key
        String modeRaw = nullSafeTrim(Settings.APPEARANCE_MODE.toString());
        if (!modeRaw.isEmpty()) {
            return parseModeContains(modeRaw, "MODEL_DATA_FLOAT_LEGACY", "MODEL_DATA_FLOAT", "MODEL_DATA_FLOATS",
                    "FLOAT", "FLOATS", "RANGE_DISPATCH", "LEGACY_FLOAT");
        }

        return false;
    }

    /**
     * Whether to generate legacy predicate overrides (assets/minecraft/models/item/*.json).
     * Not needed on 1.21.4+ since Minecraft uses the new item definition system.
     * Only enable for compatibility with external tools that read legacy JSON files.
     */
    public static boolean isGeneratePredicatesEnabled() {
        // Check new key first
        Boolean newKey = toBoolOrNull(Settings.APPEARANCE_GENERATE_PREDICATES);
        if (newKey != null) {
            return newKey;
        }

        // Backward compatibility: force_predicates or old model_data_float_legacy implied predicates
        if (Settings.APPEARANCE_FORCE_PREDICATES.toBool()) {
            return true;
        }

        // Old model_data_float_legacy always generated predicates
        Boolean legacyFloatKey = toBoolOrNull(Settings.APPEARANCE_MODEL_DATA_FLOAT_LEGACY);
        if (legacyFloatKey != null && legacyFloatKey) {
            return true;
        }

        return false;
    }

    /**
     * Validates the appearance configuration and logs warnings for conflicts.
     * Should be called during plugin initialization.
     */
    public static void validateAndLogWarnings() {
        boolean modelDataIds = isModelDataIdsEnabled();
        boolean modelDataFloat = isModelDataFloatEnabled();

        if (modelDataIds && modelDataFloat) {
            Logs.logWarning("Both model_data_ids and model_data_float are enabled. " +
                    "They write to the same pack file (assets/minecraft/items/*.json). " +
                    "model_data_ids will take priority; model_data_float is ignored for pack generation.");
        }

        if (!isItemPropertiesEnabled() && !modelDataIds && !modelDataFloat) {
            Logs.logWarning("No appearance system is enabled in settings.yml. " +
                    "Items will not have custom models on 1.21.4+. " +
                    "Enable at least one of: item_properties, model_data_ids, or model_data_float");
        }
    }

    /**
     * For MODEL_DATA_IDS vs MODEL_DATA_FLOAT pack generation.
     * Returns true if we should generate minecraft:select (strings), false for minecraft:range_dispatch (floats).
     * MODEL_DATA_IDS takes priority if both are enabled.
     */
    public static boolean shouldUseSelectForVanillaItemDefs() {
        return isModelDataIdsEnabled();
    }

    /**
     * Whether to generate vanilla item definitions (assets/minecraft/items/*.json).
     * True if either MODEL_DATA_IDS or MODEL_DATA_FLOAT is enabled.
     */
    public static boolean shouldGenerateVanillaItemDefinitions() {
        return isModelDataIdsEnabled() || isModelDataFloatEnabled();
    }

    /**
     * Whether to generate legacy predicate overrides (assets/minecraft/models/item/*.json).
     */
    public static boolean shouldGenerateLegacyPredicates() {
        return isGeneratePredicatesEnabled();
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private static Boolean toBoolOrNull(Settings setting) {
        String raw = nullSafeTrim(setting.toString());
        if (raw.isEmpty()) {
            return null;
        }
        return "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw) || "1".equals(raw);
    }

    private static boolean parseModeContains(String modeRaw, String... aliases) {
        String normalized = modeRaw.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        for (String alias : aliases) {
            if (normalized.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private static String nullSafeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
