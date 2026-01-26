package io.th0rgal.oraxen.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides effect-specific fonts for text effects.
 * <p>
 * Each effect type has a dedicated font that references vanilla glyphs.
 * Text using these fonts is detected by the shader via a tight color marker,
 * not by UV coordinates (since reference fonts share UV with the default font).
 * <p>
 * This approach ensures:
 * - Default font text: 100% color preserved, never triggers effects
 * - Effect font text: Color encodes effect parameters freely
 * <p>
 * The font generation is now dynamic, supporting unlimited effects (up to 256).
 * Font keys are generated on-demand and cached for performance.
 */
public final class EffectFontProvider {

    /**
     * Legacy font keys for backwards compatibility with effects 0-7.
     * These are kept for any code that might reference them directly.
     */
    public static final Key[] LEGACY_EFFECT_FONT_KEYS = {
            Key.key("oraxen", "effect_rainbow"),
            Key.key("oraxen", "effect_wave"),
            Key.key("oraxen", "effect_shake"),
            Key.key("oraxen", "effect_pulse"),
            Key.key("oraxen", "effect_gradient"),
            Key.key("oraxen", "effect_typewriter"),
            Key.key("oraxen", "effect_6"),
            Key.key("oraxen", "effect_7")
    };

    /**
     * Cache for dynamically created font keys.
     * Thread-safe for concurrent access during pack generation.
     */
    private static final Map<Integer, Key> fontKeyCache = new ConcurrentHashMap<>();

    /**
     * Returns the font key for a given effect ID.
     * <p>
     * For effects 0-7, returns the legacy named keys for backwards compatibility.
     * For effects 8+, generates a key based on the effect's name from TextEffect.Definition.
     *
     * @param effectId Effect type ID (0-255)
     * @return The font key for this effect
     */
    @NotNull
    public static Key getFontKey(int effectId) {
        int id = effectId & EffectFontEncoding.EFFECT_ID_MASK;

        return fontKeyCache.computeIfAbsent(id, EffectFontProvider::createFontKey);
    }

    /**
     * Creates a font key for the given effect ID.
     */
    @NotNull
    private static Key createFontKey(int effectId) {
        // For legacy effects 0-7, use the predefined names
        if (effectId < LEGACY_EFFECT_FONT_KEYS.length) {
            return LEGACY_EFFECT_FONT_KEYS[effectId];
        }

        // For effects 8+, use the effect name if available
        TextEffect.Definition def = TextEffect.getEffect(effectId);
        String suffix;
        if (def != null) {
            // Sanitize name for font key (alphanumeric + underscore only)
            suffix = def.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        } else {
            suffix = String.valueOf(effectId);
        }
        return Key.key("oraxen", "effect_" + suffix);
    }

    /**
     * Returns the font key for a named effect.
     *
     * @param effectName Effect name (e.g., "rainbow", "wave")
     * @return The font key for this effect, or the first effect font if not found
     */
    @NotNull
    public static Key getFontKey(@NotNull String effectName) {
        TextEffect.Definition definition = TextEffect.getEffect(effectName);
        if (definition == null) {
            return LEGACY_EFFECT_FONT_KEYS[0]; // Default to first effect font
        }
        return getFontKey(definition.getId());
    }

    /**
     * Generates font JSON for an effect font.
     * Each effect font references the full default font (including Oraxen glyphs).
     *
     * @param effectId Effect type ID (0-255)
     * @return JsonObject for the font definition
     */
    @NotNull
    public JsonObject generateFontJson(int effectId) {
        JsonObject font = new JsonObject();
        JsonArray providers = new JsonArray();

        // Reference the full default font (includes Oraxen glyphs, shift providers, etc.)
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "reference");
        reference.addProperty("id", "minecraft:default");
        providers.add(reference);

        font.add("providers", providers);
        return font;
    }

    /**
     * Returns all font keys for enabled effects.
     * This replaces the old getAllFontKeys() that returned a fixed array.
     *
     * @return Collection of font keys for all enabled effects
     */
    @NotNull
    public static Collection<Key> getAllEnabledFontKeys() {
        return TextEffect.getEnabledEffects().stream()
                .map(def -> getFontKey(def.getId()))
                .toList();
    }

    /**
     * Returns the legacy effect font keys array.
     *
     * @deprecated Use {@link #getAllEnabledFontKeys()} for dynamic font key retrieval.
     */
    @Deprecated
    @NotNull
    public static Key[] getAllFontKeys() {
        return LEGACY_EFFECT_FONT_KEYS.clone();
    }

    /**
     * Returns the number of legacy effect fonts.
     *
     * @deprecated Use {@link TextEffect#getEnabledEffects()}.size() for the actual count.
     */
    @Deprecated
    public static int getEffectFontCount() {
        return LEGACY_EFFECT_FONT_KEYS.length;
    }

    /**
     * Clears the font key cache.
     * Should be called when effects are reloaded to ensure fresh keys are generated.
     */
    public static void clearCache() {
        fontKeyCache.clear();
    }
}
