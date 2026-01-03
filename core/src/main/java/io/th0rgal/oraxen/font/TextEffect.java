package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.utils.MinecraftVersion;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Text effects that can be applied to any text using shader-based rendering.
 * <p>
 * Unlike animated glyphs which swap between sprite frames, text effects modify
 * how existing characters render (color, position, opacity) using GLSL shaders.
 * <p>
 * Text effects use dedicated effect fonts (one per effect type) and encode
 * parameters in the color using {@link EffectFontEncoding}. This approach
 * eliminates false positives from gradient colors since the shader only
 * triggers effects when the tight dual marker (R=253, G low nibble=0xD) is present.
 * <p>
 * Encoding layout (effect_font):
 * <ul>
 *   <li>R = 253 (marker)</li>
 *   <li>G high nibble = effect type (0-7)</li>
 *   <li>G low nibble = 0xD (marker)</li>
 *   <li>B high nibble = speed (1-7)</li>
 *   <li>B low nibble = param (0-7)</li>
 *   <li>Character index derived from {@code gl_VertexID}</li>
 * </ul>
 * <p>
 * Example configuration:
 * <pre>
 * settings.yml:
 *   TextEffects:
 *     enabled: true
 *     shader:
 *       template: auto
 *
 * text_effects.yml:
 *   effects:
 *     rainbow:
 *       id: 0
 *       enabled: true
 *       snippets:
 *         - fragment: |
 *             // GLSL snippet
 * </pre>
 */
public class TextEffect {

    /**
     * Legacy magic red value - now used as the primary marker in effect font encoding.
     *
     * @deprecated Use {@link EffectFontEncoding#R_MARKER} instead.
     */
    @Deprecated
    public static final int MAGIC_RED = 0xFD; // 253

    /**
     * Legacy marker bits for the deprecated encoding.
     *
     * @deprecated Use {@link EffectFontEncoding} instead.
     */
    @Deprecated
    public static final int EFFECT_MARKER = 0x88;

    /**
     * Default speed for effects (1-7).
     */
    public static final int DEFAULT_SPEED = 3;

    /**
     * Default parameter for effects (amplitude, intensity, etc.) (0-7).
     */
    public static final int DEFAULT_PARAM = 3;

    /**
     * Default base color when none is specified.
     */
    private static final TextColor DEFAULT_BASE_COLOR = TextColor.color(255, 255, 255);

    /**
     * Effect types available for text.
     */
    public enum Type {
        /**
         * Rainbow effect - cycles hue over time per character.
         */
        RAINBOW(0, "rainbow"),

        /**
         * Wave effect - vertical sine wave motion per character.
         */
        WAVE(1, "wave"),

        /**
         * Shake effect - random jitter per character.
         */
        SHAKE(2, "shake"),

        /**
         * Pulse effect - opacity fades in/out.
         */
        PULSE(3, "pulse"),

        /**
         * Gradient effect - static color gradient across text.
         */
        GRADIENT(4, "gradient"),

        /**
         * Typewriter effect - characters appear sequentially.
         */
        TYPEWRITER(5, "typewriter");

        private final int id;
        private final String name;

        Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * Gets a Type by its string name (case-insensitive).
         */
        @Nullable
        public static Type fromName(String name) {
            for (Type type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }

        /**
         * Gets a Type by its numeric ID.
         */
        @Nullable
        public static Type fromId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Shader template selection for text rendering.
     */
    public enum ShaderTemplate {
        AUTO("auto"),
        TEXT_EFFECTS("effects_only"),
        ANIMATED_GLYPHS("animated_only");

        private final String name;

        ShaderTemplate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Nullable
        public static ShaderTemplate fromName(@Nullable String name) {
            if (name == null) return null;
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            for (ShaderTemplate template : values()) {
                if (template.name.equals(normalized)) {
                    return template;
                }
            }

            return switch (normalized) {
                case "effects", "text_effects", "text-effects" -> TEXT_EFFECTS;
                case "animated", "animated_glyphs", "animated-glyphs" -> ANIMATED_GLYPHS;
                default -> null;
            };
        }
    }

    /**
     * Definition of a text effect loaded from text_effects.yml.
     * <p>
     * Each effect has a single color defined in the config. This color is baked
     * into the shader at pack generation time. Users apply effects with
     * {@code <effect:NAME>text</effect>} tags - the color comes from config.
     */
    public static final class Definition {
        private final String name;
        private final int id;
        private final String description;
        private boolean enabled;
        private final TextColor color;
        private final int speed;
        private final int param;
        private final List<Snippet> snippets;

        Definition(String name, int id, @Nullable String description, boolean enabled,
                   @Nullable TextColor color, int speed, int param,
                   @Nullable List<Snippet> snippets) {
            this.name = name;
            this.id = id;
            this.description = description != null ? description : "";
            this.enabled = enabled;
            this.color = color != null ? color : DEFAULT_BASE_COLOR;
            this.speed = Math.max(1, Math.min(7, speed));
            this.param = Math.max(0, Math.min(7, param));
            this.snippets = snippets != null ? List.copyOf(snippets) : List.of();
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the effect's configured color. This color is baked into the shader.
         */
        @NotNull
        public TextColor getColor() {
            return color;
        }

        /**
         * Gets the effect's configured speed (1-7).
         */
        public int getSpeed() {
            return speed;
        }

        /**
         * Gets the effect's configured param/amplitude (0-7).
         */
        public int getParam() {
            return param;
        }

        public List<Snippet> getSnippets() {
            return snippets;
        }

        @Nullable
        public Snippet resolveSnippet(int packFormat, @NotNull MinecraftVersion version) {
            for (Snippet snippet : snippets) {
                if (snippet.matches(packFormat, version)) {
                    return snippet;
                }
            }
            return null;
        }
    }

    /**
     * GLSL snippet for a text effect, optionally scoped to a target pack format/version.
     */
    public record Snippet(@Nullable String name, @Nullable SnippetTarget target,
                          @Nullable String vertex, @Nullable String fragment,
                          @Nullable String vertexPrelude, @Nullable String fragmentPrelude) {

        public boolean matches(int packFormat, @NotNull MinecraftVersion version) {
            return target == null || target.matches(packFormat, version);
        }

        public boolean hasVertex() {
            return vertex != null && !vertex.isBlank();
        }

        public boolean hasFragment() {
            return fragment != null && !fragment.isBlank();
        }

        public boolean hasVertexPrelude() {
            return vertexPrelude != null && !vertexPrelude.isBlank();
        }

        public boolean hasFragmentPrelude() {
            return fragmentPrelude != null && !fragmentPrelude.isBlank();
        }
    }

    /**
     * Target constraints for selecting a snippet.
     */
    public record SnippetTarget(@Nullable Integer minPackFormat, @Nullable Integer maxPackFormat,
                                @Nullable MinecraftVersion minVersion, @Nullable MinecraftVersion maxVersion) {
        public boolean matches(int packFormat, @NotNull MinecraftVersion version) {
            if (minPackFormat != null && packFormat < minPackFormat) {
                return false;
            }
            if (maxPackFormat != null && packFormat > maxPackFormat) {
                return false;
            }
            if (minVersion != null && version.compareTo(minVersion) < 0) {
                return false;
            }
            if (maxVersion != null && version.compareTo(maxVersion) > 0) {
                return false;
            }
            return true;
        }
    }

    // Static configuration loaded from settings.yml + text_effects.yml
    private static final Map<String, Definition> effectsByName = new LinkedHashMap<>();
    private static final Map<Integer, Definition> effectsById = new LinkedHashMap<>();
    private static final List<Definition> effectDefinitions = new ArrayList<>();
    private static String sharedVertexPrelude = "";
    private static String sharedFragmentPrelude = "";
    private static boolean globalEnabled = true;
    private static TextEffectEncoding encoding = new EffectFontEncoding();
    private static ShaderTemplate shaderTemplate = ShaderTemplate.AUTO;

    /**
     * Loads text effect configuration from settings.yml and text_effects.yml.
     *
     * @param settingsSection The TextEffects configuration section in settings.yml
     * @param effectsRoot     The root section of text_effects.yml
     */
    public static void loadConfig(@Nullable ConfigurationSection settingsSection,
                                  @Nullable ConfigurationSection effectsRoot) {
        if (settingsSection == null) {
            globalEnabled = false;
            encoding = new EffectFontEncoding();
            shaderTemplate = ShaderTemplate.AUTO;
        } else {
            globalEnabled = settingsSection.getBoolean("enabled", true);
            loadShaderConfig(settingsSection.getConfigurationSection("shader"));
        }

        loadEffectDefinitions(effectsRoot);
        applyEffectOverrides(settingsSection != null ? settingsSection.getConfigurationSection("effects") : null);
    }

    private static void loadEffectDefinitions(@Nullable ConfigurationSection effectsRoot) {
        effectsByName.clear();
        effectsById.clear();
        effectDefinitions.clear();
        sharedVertexPrelude = "";
        sharedFragmentPrelude = "";

        if (effectsRoot == null) {
            Logs.logWarning("text_effects.yml is missing; no text effects will be available.");
            return;
        }

        ConfigurationSection sharedSection = effectsRoot.getConfigurationSection("shared");
        if (sharedSection != null) {
            sharedVertexPrelude = sharedSection.getString("vertex_prelude", "");
            sharedFragmentPrelude = sharedSection.getString("fragment_prelude", "");
        }

        ConfigurationSection effectsSection = effectsRoot.getConfigurationSection("effects");
        if (effectsSection == null) {
            Logs.logWarning("text_effects.yml has no 'effects' section; no text effects will be available.");
            return;
        }

        int maxEffectId = encoding.shaderEncoding().dataMask();
        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(key);
            if (effectSection == null) {
                continue;
            }

            String name = key.trim();
            if (name.isEmpty()) {
                continue;
            }

            int id = effectSection.getInt("id", -1);
            if (id < 0) {
                Logs.logWarning("Text effect '" + name + "' is missing an id, skipping.");
                continue;
            }
            if (id > maxEffectId) {
                Logs.logWarning("Text effect '" + name + "' uses id " + id
                        + " but encoding only supports 0-" + maxEffectId + "; skipping.");
                continue;
            }

            String description = effectSection.getString("description", "");
            boolean enabled = effectSection.getBoolean("enabled", true);
            TextColor color = parseColor(effectSection.getString("color", null));
            int speed = effectSection.getInt("speed", DEFAULT_SPEED);
            int param = effectSection.getInt("param", DEFAULT_PARAM);
            List<Snippet> snippets = parseSnippets(effectSection);

            String normalized = normalizeName(name);
            if (effectsByName.containsKey(normalized)) {
                Logs.logWarning("Duplicate text effect name '" + name + "', skipping.");
                continue;
            }
            if (effectsById.containsKey(id)) {
                Logs.logWarning("Duplicate text effect id '" + id + "' for '" + name + "', skipping.");
                continue;
            }

            Definition definition = new Definition(name, id, description, enabled, color, speed, param, snippets);
            effectsByName.put(normalized, definition);
            effectsById.put(id, definition);
            effectDefinitions.add(definition);
        }
    }

    private static void applyEffectOverrides(@Nullable ConfigurationSection overrides) {
        if (overrides == null) {
            return;
        }

        for (String key : overrides.getKeys(false)) {
            Definition definition = getEffect(key);
            if (definition == null) {
                Logs.logWarning("Unknown text effect override '" + key + "' in settings.yml.");
                continue;
            }
            ConfigurationSection effectSection = overrides.getConfigurationSection(key);
            if (effectSection != null) {
                definition.setEnabled(effectSection.getBoolean("enabled", definition.isEnabled()));
            }
        }
    }

    private static List<Snippet> parseSnippets(@NotNull ConfigurationSection effectSection) {
        List<Snippet> snippets = new ArrayList<>();
        List<Map<?, ?>> snippetMaps = effectSection.getMapList("snippets");
        if (!snippetMaps.isEmpty()) {
            int index = 0;
            for (Map<?, ?> snippetMap : snippetMaps) {
                if (snippetMap == null) {
                    index++;
                    continue;
                }
                String name = asString(snippetMap.get("name"), "snippet-" + index);
                SnippetTarget target = parseTarget(snippetMap.get("targets"));
                String vertex = asString(snippetMap.get("vertex"), "");
                String fragment = asString(snippetMap.get("fragment"), "");
                String vertexPrelude = asString(snippetMap.get("vertex_prelude"), "");
                String fragmentPrelude = asString(snippetMap.get("fragment_prelude"), "");

                snippets.add(new Snippet(name, target, vertex, fragment, vertexPrelude, fragmentPrelude));
                index++;
            }
        } else {
            String vertex = effectSection.getString("vertex", "");
            String fragment = effectSection.getString("fragment", "");
            String vertexPrelude = effectSection.getString("vertex_prelude", "");
            String fragmentPrelude = effectSection.getString("fragment_prelude", "");
            if (!vertex.isBlank() || !fragment.isBlank() || !vertexPrelude.isBlank() || !fragmentPrelude.isBlank()) {
                snippets.add(new Snippet("default", null, vertex, fragment, vertexPrelude, fragmentPrelude));
            }
        }

        return snippets;
    }

    @Nullable
    private static SnippetTarget parseTarget(@Nullable Object targetObj) {
        if (!(targetObj instanceof Map<?, ?> targetMap)) {
            return null;
        }

        Integer minPack = asInteger(targetMap.get("min_pack_format"));
        Integer maxPack = asInteger(targetMap.get("max_pack_format"));
        MinecraftVersion minVersion = asVersion(targetMap.get("min_version"));
        MinecraftVersion maxVersion = asVersion(targetMap.get("max_version"));
        return new SnippetTarget(minPack, maxPack, minVersion, maxVersion);
    }

    @Nullable
    private static MinecraftVersion asVersion(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return new MinecraftVersion(text);
        } catch (Exception ex) {
            Logs.logWarning("Invalid minecraft version '" + text + "' in text_effects.yml.");
            return null;
        }
    }

    @Nullable
    private static Integer asInteger(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @NotNull
    private static String asString(@Nullable Object value, @NotNull String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString();
        return text != null ? text : defaultValue;
    }

    @Nullable
    private static TextColor parseColor(@Nullable String colorStr) {
        if (colorStr == null || colorStr.isBlank()) {
            return null;
        }
        String trimmed = colorStr.trim();
        // Support hex colors like "#FF5500" or "FF5500"
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        try {
            int rgb = Integer.parseInt(trimmed, 16);
            return TextColor.color(rgb);
        } catch (NumberFormatException ex) {
            Logs.logWarning("Invalid color '" + colorStr + "' in text_effects.yml, using default.");
            return null;
        }
    }

    @NotNull
    private static String normalizeName(@NotNull String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static void loadShaderConfig(@Nullable ConfigurationSection shaderSection) {
        ShaderTemplate parsedTemplate = ShaderTemplate.AUTO;
        TextEffectEncoding parsedEncoding = new EffectFontEncoding();

        if (shaderSection != null) {
            String templateName = shaderSection.getString("template", ShaderTemplate.AUTO.getName());
            ShaderTemplate template = ShaderTemplate.fromName(templateName);
            if (template == null) {
                Logs.logWarning("Unknown TextEffects.shader.template '" + templateName + "', using 'auto'.");
            } else {
                parsedTemplate = template;
            }

            // Effect font encoding is now the only supported encoding
            // The encoding config option is deprecated but we still read it for logging
            String encodingName = shaderSection.getString("encoding", null);
            if (encodingName != null && !encodingName.isEmpty()) {
                String normalized = encodingName.trim().toLowerCase(Locale.ROOT);
                if (!normalized.equals("effect_font") && !normalized.equals("effect-font")) {
                    Logs.logWarning("TextEffects.shader.encoding '" + encodingName + "' is deprecated. Using 'effect_font'.");
                }
            }
        }

        shaderTemplate = parsedTemplate;
        encoding = parsedEncoding;
    }

    /**
     * Checks if text effects are globally enabled.
     */
    public static boolean isEnabled() {
        return globalEnabled;
    }

    /**
     * Returns the configured shader template selection.
     */
    public static ShaderTemplate getShaderTemplate() {
        return shaderTemplate;
    }

    /**
     * Returns the configured encoding strategy.
     */
    public static TextEffectEncoding getEncoding() {
        return encoding;
    }

    /**
     * Returns all configured text effects.
     */
    @NotNull
    public static List<Definition> getEffects() {
        return Collections.unmodifiableList(effectDefinitions);
    }

    /**
     * Returns all enabled text effects.
     */
    @NotNull
    public static List<Definition> getEnabledEffects() {
        if (!globalEnabled) {
            return List.of();
        }
        List<Definition> enabled = new ArrayList<>();
        for (Definition definition : effectDefinitions) {
            if (definition.isEnabled()) {
                enabled.add(definition);
            }
        }
        return enabled;
    }

    /**
     * Checks if a specific effect is enabled.
     */
    public static boolean isEffectEnabled(@NotNull Definition definition) {
        return globalEnabled && definition.isEnabled();
    }

    /**
     * Checks if a specific effect is enabled by name.
     */
    public static boolean isEffectEnabled(@NotNull String name) {
        Definition definition = getEffect(name);
        return definition != null && isEffectEnabled(definition);
    }

    /**
     * Checks if a specific effect type is enabled.
     *
     * @deprecated Prefer {@link #isEffectEnabled(String)}.
     */
    @Deprecated
    public static boolean isEffectEnabled(Type type) {
        Definition definition = getEffect(type.getName());
        return definition != null && isEffectEnabled(definition);
    }

    /**
     * Returns true if at least one effect is enabled.
     */
    public static boolean hasAnyEffectEnabled() {
        return !getEnabledEffects().isEmpty();
    }

    /**
     * Gets a text effect definition by name.
     */
    @Nullable
    public static Definition getEffect(@NotNull String name) {
        return effectsByName.get(normalizeName(name));
    }

    /**
     * Gets a text effect definition by id.
     */
    @Nullable
    public static Definition getEffect(int id) {
        return effectsById.get(id);
    }

    /**
     * Returns the shared vertex prelude snippet.
     */
    @NotNull
    public static String getSharedVertexPrelude() {
        return sharedVertexPrelude != null ? sharedVertexPrelude : "";
    }

    /**
     * Returns the shared fragment prelude snippet.
     */
    @NotNull
    public static String getSharedFragmentPrelude() {
        return sharedFragmentPrelude != null ? sharedFragmentPrelude : "";
    }

    /**
     * Sets whether a specific effect is enabled.
     */
    public static void setEffectEnabled(@NotNull Definition definition, boolean enabled) {
        definition.setEnabled(enabled);
    }

    /**
     * Sets whether a specific effect is enabled by name.
     */
    public static void setEffectEnabled(@NotNull String name, boolean enabled) {
        Definition definition = getEffect(name);
        if (definition != null) {
            definition.setEnabled(enabled);
        }
    }

    /**
     * Sets whether a specific effect type is enabled.
     *
     * @deprecated Prefer {@link #setEffectEnabled(String, boolean)}.
     */
    @Deprecated
    public static void setEffectEnabled(Type type, boolean enabled) {
        Definition definition = getEffect(type.getName());
        if (definition != null) {
            definition.setEnabled(enabled);
        }
    }

    /**
     * Sets whether text effects are globally enabled.
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }

    /**
     * Gets the encoded color for a text effect character, using the default base color.
     *
     * @param definition The effect definition
     * @param speed      Speed of the effect (1-7)
     * @param charIndex  Character index for phase offset (0-7, wraps for longer text);
     *                   some encodings derive this in the shader and may ignore it.
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Encoded color carrying the effect parameters
     */
    @NotNull
    public static TextColor getMagicColor(@NotNull Definition definition, int speed, int charIndex, int param) {
        return getMagicColor(DEFAULT_BASE_COLOR, definition, speed, charIndex, param);
    }

    /**
     * Gets the encoded color for a text effect character, preserving the base color.
     *
     * @param baseColor  Base color to preserve
     * @param definition The effect definition
     * @param speed      Speed of the effect (1-7)
     * @param charIndex  Character index for phase offset (0-7, wraps for longer text)
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Encoded color carrying the effect parameters
     */
    @NotNull
    public static TextColor getMagicColor(@NotNull TextColor baseColor, @NotNull Definition definition,
                                          int speed, int charIndex, int param) {
        return encoding.encode(baseColor, definition.getId(), speed, param, charIndex);
    }

    /**
     * Gets the encoded color for a text effect character by name.
     */
    @NotNull
    public static TextColor getMagicColor(@NotNull String name, int speed, int charIndex, int param) {
        Definition definition = getEffect(name);
        if (definition == null) {
            return DEFAULT_BASE_COLOR;
        }
        return getMagicColor(definition, speed, charIndex, param);
    }

    /**
     * Gets the encoded color for a text effect character, using the default base color.
     *
     * @deprecated Prefer {@link #getMagicColor(Definition, int, int, int)}.
     */
    @Deprecated
    @NotNull
    public static TextColor getMagicColor(Type type, int speed, int charIndex, int param) {
        Definition definition = getEffect(type.getName());
        if (definition == null) {
            return DEFAULT_BASE_COLOR;
        }
        return getMagicColor(definition, speed, charIndex, param);
    }

    /**
     * Gets the encoded color for a text effect character, preserving the base color.
     *
     * @deprecated Prefer {@link #getMagicColor(TextColor, Definition, int, int, int)}.
     */
    @Deprecated
    @NotNull
    public static TextColor getMagicColor(@NotNull TextColor baseColor, Type type, int speed, int charIndex, int param) {
        Definition definition = getEffect(type.getName());
        if (definition == null) {
            return baseColor;
        }
        return getMagicColor(baseColor, definition, speed, charIndex, param);
    }

    /**
     * Applies a text effect to a string, returning a Component with encoded colors.
     *
     * @param text   The text to apply the effect to
     * @param definition   The effect definition
     * @param speed  Speed of the effect (1-7)
     * @param param  Additional parameter (amplitude, intensity, etc.) (0-7)
     * @return Component with per-character encoded colors
     */
    @NotNull
    public static Component apply(String text, Definition definition, int speed, int param) {
        return apply(text, definition, speed, param, null);
    }

    /**
     * Applies a text effect to a string using effect-specific fonts.
     * <p>
     * With effect fonts, the color is used entirely for encoding effect parameters.
     * The original base color is NOT preserved - effect fonts trade color for effect.
     *
     * @param text       The text to apply the effect to
     * @param definition The effect definition
     * @param speed      Speed of the effect (1-7)
     * @param param      Additional parameter (amplitude, intensity, etc.) (0-7)
     * @param baseColor  Ignored - effect fonts don't preserve base color
     * @return Component with per-character encoded colors and effect font
     */
    @NotNull
    public static Component apply(String text, Definition definition, int speed, int param, @Nullable TextColor baseColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (definition == null || !isEffectEnabled(definition)) {
            TextColor effectiveBase = baseColor != null ? baseColor : DEFAULT_BASE_COLOR;
            return Component.text(text).color(effectiveBase);
        }

        // Get the effect-specific font
        Key effectFont = EffectFontProvider.getFontKey(definition.getId());

        Component result = Component.empty();
        int idx = 0;

        for (int i = 0; i < text.length(); ) {
            int codepoint = text.codePointAt(i);

            // Encode effect parameters in color (baseColor is ignored for effect fonts)
            TextColor encodedColor = encoding.encode(null, definition.getId(), speed, param, idx);

            result = result.append(
                    Component.text(Character.toString(codepoint))
                            .font(effectFont)  // Use effect-specific font
                            .color(encodedColor)
            );

            i += Character.charCount(codepoint);
            idx++;
        }

        return result;
    }

    /**
     * Applies a text effect using the definition's configured speed and param.
     * This is the primary method for applying effects.
     */
    @NotNull
    public static Component apply(String text, Definition definition) {
        if (definition == null) {
            return Component.text(text != null ? text : "");
        }
        return apply(text, definition, definition.getSpeed(), definition.getParam());
    }

    /**
     * Applies a text effect by name.
     */
    @NotNull
    public static Component apply(String text, String effectName, int speed, int param) {
        Definition definition = getEffect(effectName);
        return apply(text, definition, speed, param, null);
    }

    /**
     * Applies a text effect by name using the definition's configured parameters.
     */
    @NotNull
    public static Component apply(String text, String effectName) {
        Definition definition = getEffect(effectName);
        return apply(text, definition);
    }

    /**
     * Applies a text effect with default parameters.
     *
     * @deprecated Prefer {@link #apply(String, Definition)}.
     */
    @Deprecated
    @NotNull
    public static Component apply(String text, Type type) {
        return apply(text, type, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies a text effect to a string, returning a Component with encoded colors.
     *
     * @deprecated Prefer {@link #apply(String, Definition, int, int)}.
     */
    @Deprecated
    @NotNull
    public static Component apply(String text, Type type, int speed, int param) {
        return apply(text, type, speed, param, null);
    }

    /**
     * Applies a text effect to a string while preserving the provided base color.
     *
     * @deprecated Prefer {@link #apply(String, Definition, int, int, TextColor)}.
     */
    @Deprecated
    @NotNull
    public static Component apply(String text, Type type, int speed, int param, @Nullable TextColor baseColor) {
        Definition definition = getEffect(type.getName());
        return apply(text, definition, speed, param, baseColor);
    }

    // Convenience methods for each effect type (by name)

    /**
     * Applies rainbow effect - cycles through hues over time.
     *
     * @param text  The text to colorize
     * @param speed How fast the rainbow cycles (1-7)
     * @return Component with rainbow effect colors
     */
    @NotNull
    public static Component rainbow(String text, int speed) {
        return apply(text, "rainbow", speed, 0);
    }

    /**
     * Applies rainbow effect with default speed.
     */
    @NotNull
    public static Component rainbow(String text) {
        return rainbow(text, DEFAULT_SPEED);
    }

    /**
     * Applies wave effect - vertical sine wave motion.
     *
     * @param text      The text to animate
     * @param speed     How fast the wave moves (1-7)
     * @param amplitude Wave amplitude (1-7)
     * @return Component with wave effect colors
     */
    @NotNull
    public static Component wave(String text, int speed, int amplitude) {
        return apply(text, "wave", speed, amplitude);
    }

    /**
     * Applies wave effect with default parameters.
     */
    @NotNull
    public static Component wave(String text) {
        return wave(text, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies shake effect - random jitter.
     *
     * @param text      The text to animate
     * @param speed     How fast the shake updates (1-7)
     * @param intensity Shake intensity (1-7)
     * @return Component with shake effect colors
     */
    @NotNull
    public static Component shake(String text, int speed, int intensity) {
        return apply(text, "shake", speed, intensity);
    }

    /**
     * Applies shake effect with default parameters.
     */
    @NotNull
    public static Component shake(String text) {
        return shake(text, DEFAULT_SPEED, DEFAULT_PARAM);
    }

    /**
     * Applies pulse effect - opacity fades in/out.
     *
     * @param text  The text to animate
     * @param speed How fast the pulse cycles (1-7)
     * @return Component with pulse effect colors
     */
    @NotNull
    public static Component pulse(String text, int speed) {
        return apply(text, "pulse", speed, 0);
    }

    /**
     * Applies pulse effect with default speed.
     */
    @NotNull
    public static Component pulse(String text) {
        return pulse(text, DEFAULT_SPEED);
    }

    /**
     * Applies gradient effect - static color gradient.
     *
     * @param text The text to colorize
     * @return Component with gradient effect colors
     */
    @NotNull
    public static Component gradient(String text) {
        return apply(text, "gradient", 0, 0);
    }

    /**
     * Applies typewriter effect - characters appear sequentially.
     *
     * @param text  The text to animate
     * @param speed How fast characters appear (1-7)
     * @return Component with typewriter effect colors
     */
    @NotNull
    public static Component typewriter(String text, int speed) {
        return apply(text, "typewriter", speed, 0);
    }

    /**
     * Applies typewriter effect with default speed.
     */
    @NotNull
    public static Component typewriter(String text) {
        return typewriter(text, DEFAULT_SPEED);
    }

    /**
     * Checks if a color value represents a text effect.
     *
     * @param color The color value to check
     * @return true if this is a text effect magic color
     */
    public static boolean isTextEffectColor(int color) {
        return encoding.matches(color);
    }

    /**
     * Extracts text effect parameters from a magic color.
     *
     * @param color The color value
     * @return int array of [effectType, speed, charIndex, param], or null if not a text effect.
     *         Some encodings derive charIndex in the shader; in that case charIndex may be 0 here.
     */
    @Nullable
    public static int[] extractParams(int color) {
        TextEffectEncoding.Decoded decoded = encoding.decode(color);
        if (decoded == null) return null;
        return new int[]{decoded.effectType(), decoded.speed(), decoded.charIndex(), decoded.param()};
    }
}
