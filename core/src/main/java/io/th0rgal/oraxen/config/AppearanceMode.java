package io.th0rgal.oraxen.config;

import java.util.Locale;

/**
 * Controls how Oraxen drives item appearance on 1.21.4+.
 * <p>
 * Pre-1.21.4 does not support item definitions ({@code assets/&#42;/items/&#42;.json}), so Oraxen will
 * always fall back to legacy predicate overrides + integer CustomModelData, regardless of this mode.
 */
public enum AppearanceMode {
    /**
     * Use the minecraft:item_model component with Oraxen ids (oraxen:&lt;item_id&gt;).
     * Generates assets/oraxen/items/&lt;item_id&gt;.json.
     */
    ITEM_PROPERTIES,
    /**
     * Use minecraft:custom_model_data.strings[0] = "oraxen:&lt;item_id&gt;" and
     * generate assets/minecraft/items/&lt;material&gt;.json with minecraft:select.
     */
    MODEL_DATA_IDS,
    /**
     * Legacy numeric model data mapping using minecraft:custom_model_data.floats[0] and
     * generate assets/minecraft/items/&lt;material&gt;.json with minecraft:range_dispatch.
     */
    MODEL_DATA_FLOAT_LEGACY;

    /**
     * Preferred entrypoint used by runtime/pack generation.
     * <p>
     * If the new {@code Pack.generation.appearance.mode} key is missing, this falls back to the legacy
     * boolean settings for compatibility with older configs.
     */
    public static AppearanceMode fromSettings() {
        String raw = nullSafeTrim(Settings.APPEARANCE_MODE.toString());
        if (!raw.isEmpty()) {
            return parse(raw);
        }

        // Backward compatible fallback:
        // - item_model=true -> ITEM_PROPERTIES
        // - predicates=true -> MODEL_DATA_IDS (modern CMD strings on 1.21.4+; legacy predicates pre-1.21.4)
        if (Settings.APPEARANCE_ITEM_MODEL.toBool()) {
            return ITEM_PROPERTIES;
        }
        if (Settings.APPEARANCE_PREDICATES.toBool()) {
            return MODEL_DATA_IDS;
        }
        return ITEM_PROPERTIES;
    }

    public static AppearanceMode parse(String raw) {
        String normalized = nullSafeTrim(raw)
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        // Friendly aliases
        return switch (normalized) {
            case "ITEM_MODEL", "ITEM_PROPERTIES", "ITEM_PROPERTY", "ITEM" -> ITEM_PROPERTIES;
            case "MODEL_DATA", "MODEL_DATA_IDS", "MODEL_DATA_ID", "CMD_IDS", "CMD_ID", "STRINGS", "CMD_STRINGS" ->
                    MODEL_DATA_IDS;
            case "MODEL_DATA_FLOAT", "MODEL_DATA_FLOATS", "FLOAT", "FLOATS", "RANGE_DISPATCH", "LEGACY_FLOAT",
                 "MODEL_DATA_FLOAT_LEGACY" -> MODEL_DATA_FLOAT_LEGACY;
            default -> {
                try {
                    yield AppearanceMode.valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield ITEM_PROPERTIES;
                }
            }
        };
    }

    private static String nullSafeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}

