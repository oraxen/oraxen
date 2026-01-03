package io.th0rgal.oraxen.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

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
 */
public final class EffectFontProvider {

    /**
     * Font keys for each effect type (0-7).
     */
    public static final Key[] EFFECT_FONT_KEYS = {
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
     * Returns the font key for a given effect ID.
     *
     * @param effectId Effect type ID (0-7)
     * @return The font key for this effect
     */
    @NotNull
    public static Key getFontKey(int effectId) {
        int index = effectId & 0x07;
        return EFFECT_FONT_KEYS[index];
    }

    /**
     * Returns the font key for a named effect.
     *
     * @param effectName Effect name (e.g., "rainbow", "wave")
     * @return The font key for this effect, or null if not found
     */
    @NotNull
    public static Key getFontKey(@NotNull String effectName) {
        TextEffect.Definition definition = TextEffect.getEffect(effectName);
        if (definition == null) {
            return EFFECT_FONT_KEYS[0]; // Default to first effect font
        }
        return getFontKey(definition.getId());
    }

    /**
     * Generates font JSON for an effect font.
     * Each effect font references the vanilla default font providers.
     *
     * @param effectId Effect type ID (0-7)
     * @return JsonObject for the font definition
     */
    @NotNull
    public JsonObject generateFontJson(int effectId) {
        JsonObject font = new JsonObject();
        JsonArray providers = new JsonArray();

        // Reference vanilla font for all standard characters
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "reference");
        reference.addProperty("id", "minecraft:include/default");
        providers.add(reference);

        font.add("providers", providers);
        return font;
    }

    /**
     * Returns all effect font keys.
     */
    @NotNull
    public static Key[] getAllFontKeys() {
        return EFFECT_FONT_KEYS.clone();
    }

    /**
     * Returns the number of effect fonts available.
     */
    public static int getEffectFontCount() {
        return EFFECT_FONT_KEYS.length;
    }
}
