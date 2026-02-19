package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.TextEffect;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Generates text shaders (animation + text effects) and scoreboard shaders.
 * Extracted from ResourcePack to reduce class size.
 *
 * <p>All shader output is written via {@link ResourcePack#writeStringToVirtual},
 * which is a public static method that remains in ResourcePack.
 */
class TextShaderGenerator {

    // --- Records ---

    record TextShaderFeatures(boolean animatedGlyphs, boolean textEffects) {
        boolean anyEnabled() {
            return animatedGlyphs || textEffects;
        }
    }

    private record TextEffectSnippets(String vertexPrelude, String fragmentPrelude,
                                      String vertexEffects, String fragmentEffects) {
    }

    private record VertexShaderConfig(
            String fogDistanceInit,
            String fogDistanceRecalc,
            String vertexColorInit,
            String vertexColorAnimated,
            String moduloExpr
    ) {}

    // --- State ---

    private boolean textShadersGenerated = false;
    private boolean shaderOverlaysGenerated = false;
    private TextShaderFeatures textShaderFeatures = null;
    private TextEffectSnippets textEffectSnippets = null;
    private TextShaderTarget textEffectSnippetsTarget = null;

    /** Directory name for 1.21.6+ shader overlay */
    private static final String OVERLAY_1_21_6_PLUS = "overlay_1_21_6_plus";
    /** Directory name for 1.21.4/1.21.5 shader overlay */
    private static final String OVERLAY_1_21_4_5 = "overlay_1_21_4_5";

    // Common uniform definitions for shader JSON
    private static final String UNIFORM_MATRIX = """
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] }""";
    private static final String UNIFORM_FOG = """
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                        { "name": "FogShape", "type": "int", "count": 1, "values": [ 0 ] }""";
    private static final String UNIFORM_GAMETIME = """
                        { "name": "GameTime", "type": "float", "count": 1, "values": [ 0.0 ] }""";
    private static final String UNIFORM_SCREENSIZE = """
                        { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] }""";

    // --- Public API ---

    void reset() {
        textShadersGenerated = false;
        shaderOverlaysGenerated = false;
        textShaderFeatures = null;
        textEffectSnippets = null;
        textEffectSnippetsTarget = null;
    }

    boolean wereTextShadersGenerated() {
        return textShadersGenerated;
    }

    boolean wereShaderOverlaysGenerated() {
        return shaderOverlaysGenerated;
    }

    TextShaderFeatures getTextShaderFeatures() {
        return textShaderFeatures;
    }

    String getOverlay1216PlusName() {
        return OVERLAY_1_21_6_PLUS;
    }

    String getOverlay12145Name() {
        return OVERLAY_1_21_4_5;
    }

    void maybeGenerateTextShaders(boolean hasAnimatedGlyphs) {
        if (textShadersGenerated) return;

        TextShaderFeatures features = resolveTextShaderFeatures(hasAnimatedGlyphs);
        if (!features.anyEnabled()) return;

        TextShaderTarget target = TextShaderTarget.current();
        generateTextShaders(target, features);
        textShaderFeatures = features;
        textShadersGenerated = true;
    }

    void hideScoreboardNumbers() {
        if (OraxenPlugin.get().getPacketAdapter().isEnabled() && VersionUtil.isPaperServer()
                && VersionUtil.atOrAbove("1.20.3")) {
            OraxenPlugin.get().getPacketAdapter().registerScoreboardListener();
        } else { // Pre 1.20.3 rely on shaders
            // Check if text shaders were already generated - need to combine them
            if (textShadersGenerated) {
                // Use combined shaders that support both text features and scoreboard hiding
                TextShaderTarget target = TextShaderTarget.current();
                boolean hasAnimatedGlyphs = !OraxenPlugin.get().getFontManager().getAnimatedGlyphs().isEmpty();
                TextShaderFeatures features = textShaderFeatures != null
                        ? textShaderFeatures
                        : resolveTextShaderFeatures(hasAnimatedGlyphs);
                ResourcePack.writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh",
                        getCombinedVertexShader(target, features));
                ResourcePack.writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json",
                        getCombinedShaderJson(target));
                // Fragment shader stays the same (text shader uses vertex shader for scoreboard hiding)
                Logs.logInfo("Using combined text + scoreboard hiding shaders");
            } else {
                ResourcePack.writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.json", getScoreboardJson());
                ResourcePack.writeStringToVirtual("assets/minecraft/shaders/core/", "rendertype_text.vsh", getScoreboardVsh());
            }
        }
    }

    void hideScoreboardOrTablistBackgrounds() {
        String fileName = VersionUtil.atOrAbove("1.20.1") ? "rendertype_gui.vsh" : "position_color.fsh";
        String scoreTabBackground = "";
        if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool() || Settings.HIDE_TABLIST_BACKGROUND.toBool())
            scoreTabBackground = getScoreboardBackground();
        if (Settings.HIDE_SCOREBOARD_BACKGROUND.toBool())
            scoreTabBackground = scoreTabBackground.replaceFirst("//SCOREBOARD.a", "vertexColor.a");
        if (Settings.HIDE_TABLIST_BACKGROUND.toBool() && VersionUtil.atOrAbove("1.21"))
            scoreTabBackground = scoreTabBackground.replace("//TABLIST.a", "vertexColor.a");

        if (!scoreTabBackground.isEmpty())
            ResourcePack.writeStringToVirtual("assets/minecraft/shaders/core/", fileName, scoreTabBackground);
    }

    // --- Internal methods ---

    TextShaderFeatures resolveTextShaderFeatures(boolean hasAnimatedGlyphs) {
        boolean textEffectsEnabled = TextEffect.isEnabled() && TextEffect.hasAnyEffectEnabled();
        TextEffect.ShaderTemplate template = TextEffect.getShaderTemplate();

        boolean includeAnimated;
        boolean includeEffects;

        switch (template) {
            case ANIMATED_GLYPHS -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = false;
            }
            case TEXT_EFFECTS -> {
                includeAnimated = false;
                includeEffects = textEffectsEnabled;
            }
            case AUTO -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = textEffectsEnabled;
            }
            default -> {
                includeAnimated = hasAnimatedGlyphs;
                includeEffects = textEffectsEnabled;
            }
        }

        if (hasAnimatedGlyphs && !includeAnimated) {
            Logs.logWarning("Animated glyphs detected but TextEffects.shader.template disables them.");
        }
        if (textEffectsEnabled && !includeEffects) {
            Logs.logWarning("Text effects are enabled but TextEffects.shader.template disables them.");
        }

        return new TextShaderFeatures(includeAnimated, includeEffects);
    }

    private void generateTextShaders(TextShaderTarget target, TextShaderFeatures features) {
        // Generate base shaders for the current server version
        generateTextShadersForTarget(target, features, "");

        // Generate overlay shaders for cross-version compatibility
        // If server is 1.21.4/1.21.5, also generate 1.21.6+ shaders in overlay directory
        if (target.isAtLeast("1.21.4") && !target.isAtLeast("1.21.6")) {
            TextShaderTarget overlayTarget = TextShaderTarget.forVersion("1.21.6");
            generateTextShadersForTarget(overlayTarget, features, OVERLAY_1_21_6_PLUS + "/");
            shaderOverlaysGenerated = true;
            Logs.logSuccess("Generated shader overlay for 1.21.6+ clients");
        }
        // If server is 1.21.6+, also generate 1.21.4/1.21.5 shaders in overlay directory
        else if (target.isAtLeast("1.21.6")) {
            TextShaderTarget overlayTarget = TextShaderTarget.forVersion("1.21.4");
            generateTextShadersForTarget(overlayTarget, features, OVERLAY_1_21_4_5 + "/");
            shaderOverlaysGenerated = true;
            Logs.logSuccess("Generated shader overlay for 1.21.4/1.21.5 clients");
        }
    }

    private void generateTextShadersForTarget(TextShaderTarget target, TextShaderFeatures features, String pathPrefix) {
        // Generate shaders (see-through uses a different vertex format on 1.21.6+)
        String vshContent = getAnimationVertexShader(target, features, false);
        String fshContent = getAnimationFragmentShader(target, false);
        String jsonContent = getAnimationShaderJson(target, false);

        String vshSeeThrough = getAnimationVertexShader(target, features, true);
        String fshSeeThrough = getAnimationFragmentShader(target, true);
        String jsonSeeThrough = getAnimationShaderJson(target, true);

        String vshIntensity = getAnimationVertexShader(target, features, false);
        String fshIntensity = getAnimationFragmentShader(target, false, true);
        String jsonIntensity = getAnimationShaderJson(target, "rendertype_text_intensity", false);

        String vshIntensitySeeThrough = getAnimationVertexShader(target, features, true);
        String fshIntensitySeeThrough = getAnimationFragmentShader(target, true, true);
        String jsonIntensitySeeThrough = getAnimationShaderJson(target, "rendertype_text_intensity_see_through", true);

        String shaderPath = pathPrefix + "assets/minecraft/shaders/core";

        // Write shaders for both rendertype_text and rendertype_text_see_through
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text.vsh", vshContent);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text.fsh", fshContent);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text.json", jsonContent);

        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_see_through.vsh", vshSeeThrough);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_see_through.fsh", fshSeeThrough);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_see_through.json", jsonSeeThrough);

        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity.vsh", vshIntensity);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity.fsh", fshIntensity);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity.json", jsonIntensity);

        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.vsh", vshIntensitySeeThrough);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.fsh", fshIntensitySeeThrough);
        ResourcePack.writeStringToVirtual(shaderPath, "rendertype_text_intensity_see_through.json", jsonIntensitySeeThrough);

        Logs.logSuccess("Generated text shaders for " + target.displayName()
                + " (shader " + getShaderVersion(target) + ")" + (pathPrefix.isEmpty() ? "" : " [overlay]"));
    }

    private String getShaderVersion(TextShaderTarget target) {
        if (target.isAtLeast("1.21.6")) {
            return "1.21.6";
        } else if (target.isAtLeast("1.21.4")) {
            return "1.21.4";
        } else if (target.isAtLeast("1.21")) {
            return "1.21";
        } else {
            return "1.20";
        }
    }

    private String getTextShaderConstants(TextShaderTarget target, TextShaderFeatures features) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, """
                const bool ORAXEN_ANIMATED_GLYPHS = %s;
                const bool ORAXEN_TEXT_EFFECTS = %s;
                """,
                features.animatedGlyphs() ? "true" : "false",
                features.textEffects() ? "true" : "false"));

        // Generate exact trigger colors only for effects that have valid snippets for this target
        List<TextEffect.Definition> enabledEffects = TextEffect.getEnabledEffects().stream()
                .filter(def -> def.resolveSnippet(target.packFormat(), target.minecraftVersion()) != null)
                .toList();
        int effectCount = enabledEffects.size();
        sb.append(String.format(Locale.ROOT, "const int ORAXEN_EFFECT_COUNT = %d;\n", effectCount));

        // Always define arrays (GLSL requires all identifiers to exist even in unreachable branches)
        int arraySize = Math.max(1, effectCount);
        sb.append("const ivec3 ORAXEN_EFFECT_TRIGGERS[").append(arraySize).append("] = ivec3[](\n");
        if (enabledEffects.isEmpty()) {
            sb.append("    ivec3(0, 0, 0)\n");
        } else {
            for (int i = 0; i < enabledEffects.size(); i++) {
                TextEffect.Definition def = enabledEffects.get(i);
                int rgb = def.getTriggerColor().value();
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                sb.append(String.format(Locale.ROOT, "    ivec3(%d, %d, %d)", r, g, b));
                if (i < enabledEffects.size() - 1) {
                    sb.append(",");
                }
                sb.append(" // ").append(def.getName()).append(" (id=").append(def.getId()).append(")\n");
            }
        }
        sb.append(");\n");

        sb.append("const int ORAXEN_EFFECT_IDS[").append(arraySize).append("] = int[](\n");
        if (enabledEffects.isEmpty()) {
            sb.append("    0\n");
        } else {
            for (int i = 0; i < enabledEffects.size(); i++) {
                TextEffect.Definition def = enabledEffects.get(i);
                sb.append(String.format(Locale.ROOT, "    %d", def.getId()));
                if (i < enabledEffects.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        sb.append(");\n");

        return sb.toString();
    }

    private TextEffectSnippets getTextEffectSnippets(TextShaderTarget target) {
        if (textEffectSnippets != null && target.equals(textEffectSnippetsTarget)) {
            return textEffectSnippets;
        }
        textEffectSnippetsTarget = target;
        textEffectSnippets = buildTextEffectSnippets(target);
        return textEffectSnippets;
    }

    private TextEffectSnippets buildTextEffectSnippets(TextShaderTarget target) {
        if (!TextEffect.isEnabled() || !TextEffect.hasAnyEffectEnabled()) {
            return new TextEffectSnippets("", "", "", "");
        }

        StringBuilder vertexPrelude = new StringBuilder();
        StringBuilder fragmentPrelude = new StringBuilder();
        StringBuilder vertexEffects = new StringBuilder();
        StringBuilder fragmentEffects = new StringBuilder();

        appendPrelude(vertexPrelude, TextEffect.getSharedVertexPrelude());
        appendPrelude(fragmentPrelude, TextEffect.getSharedFragmentPrelude());

        boolean firstVertex = true;
        boolean firstFragment = true;

        for (TextEffect.Definition definition : TextEffect.getEnabledEffects()) {
            TextEffect.Snippet snippet = definition.resolveSnippet(target.packFormat(), target.minecraftVersion());
            if (snippet == null) {
                Logs.logWarning("No shader snippet for text effect '" + definition.getName()
                        + "' on target " + target.displayName());
                continue;
            }

            if (snippet.hasVertexPrelude()) {
                appendPrelude(vertexPrelude, snippet.vertexPrelude());
            }
            if (snippet.hasFragmentPrelude()) {
                appendPrelude(fragmentPrelude, snippet.fragmentPrelude());
            }

            if (snippet.hasVertex()) {
                appendEffectBlock(vertexEffects, definition, snippet.vertex(), firstVertex);
                firstVertex = false;
            }
            if (snippet.hasFragment()) {
                appendEffectBlock(fragmentEffects, definition, snippet.fragment(), firstFragment);
                firstFragment = false;
            }
        }

        return new TextEffectSnippets(vertexPrelude.toString(), fragmentPrelude.toString(),
                vertexEffects.toString(), fragmentEffects.toString());
    }

    private void appendPrelude(StringBuilder builder, @Nullable String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(snippet.stripTrailing());
    }

    private void appendEffectBlock(StringBuilder builder, TextEffect.Definition definition,
                                   String snippet, boolean first) {
        String effectIndent = "                            ";
        String codeIndent = effectIndent + "    ";

        int effectId = definition.getId();

        builder.append(effectIndent)
                .append("// ")
                .append(definition.getName())
                .append(" (id=")
                .append(effectId)
                .append(")\n");
        builder.append(effectIndent)
                .append(first ? "if" : "else if")
                .append(" (effectType == ")
                .append(effectId)
                .append(") {\n");
        builder.append(indentSnippet(snippet, codeIndent));
        builder.append("\n")
                .append(effectIndent)
                .append("}\n");
    }

    private String indentSnippet(String snippet, String indent) {
        String trimmed = snippet.stripTrailing();
        if (trimmed.isEmpty()) {
            return "";
        }
        String[] lines = trimmed.split("\\R", -1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                out.append(indent).append(line);
            }
            if (i < lines.length - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }

    private String getVertexShaderMainBody(VertexShaderConfig config, String vertexEffects, boolean includeScoreboardHiding) {
        String scoreboardHiding = includeScoreboardHiding ? """

                        // Scoreboard number hiding
                        if (Position.z == 0.0 &&
                                gl_Position.x >= 0.95 && gl_Position.y >= -0.35 &&
                                vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 &&
                                gl_VertexID <= 4) {
                            gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0);
                        }""" : "";

        return """
                    void main() {
                        vec3 pos = Position;
                        gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
%s                        texCoord0 = UV0;
                        vertexColor = %s;
                        effectData = vec4(-1.0, 0.0, 0.0, 0.0); // -1 means no effect

                        int rInt = int(Color.r * 255.0 + 0.5);
                        int gRaw = int(Color.g * 255.0 + 0.5);
                        int bRaw = int(Color.b * 255.0 + 0.5);

                        // Check for animation color: R=254 for primary, R≈63 for shadow
                        bool isPrimaryAnim = (rInt == 254);
                        bool isShadowAnim = (rInt >= 62 && rInt <= 64) && (gRaw >= 1) && (bRaw <= 64);

                        if (ORAXEN_ANIMATED_GLYPHS && (isPrimaryAnim || isShadowAnim)) {
                            int gInt = isPrimaryAnim ? gRaw : min(255, gRaw * 4);
                            int bInt = isPrimaryAnim ? bRaw : min(255, bRaw * 4);

                            bool loop = (gInt < 128);
                            float fps = max(1.0, float(gInt & 0x7F));
                            int frameIndex = bInt & 0x0F;
                            int totalFrames = ((bInt >> 4) & 0x0F) + 1;

                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
                            int rawFrame = int(floor(timeSeconds * fps));
                            int currentFrame = loop ? %s : min(rawFrame, totalFrames - 1);

                            float visible = (frameIndex == currentFrame && isPrimaryAnim) ? 1.0 : 0.0;

                            if (isPrimaryAnim) {
                                vertexColor = %s;
                            } else {
                                vertexColor = vec4(0.0);
                            }
                        }

                        // Text effects: exact trigger color matching
                        if (ORAXEN_TEXT_EFFECTS && ORAXEN_EFFECT_COUNT > 0 && (!ORAXEN_ANIMATED_GLYPHS || (!isPrimaryAnim && !isShadowAnim))) {
                            // Check for exact trigger color match
                            ivec3 colorInt = ivec3(rInt, gRaw, bRaw);
                            int effectType = -1;
                            for (int i = 0; i < ORAXEN_EFFECT_COUNT; i++) {
                                if (colorInt == ORAXEN_EFFECT_TRIGGERS[i]) {
                                    effectType = ORAXEN_EFFECT_IDS[i];
                                    break;
                                }
                            }

                            if (effectType >= 0) {
                                float speed = 3.0; // Default speed (configured in shader snippets)
                                float param = 3.0; // Default param (configured in shader snippets)
                                float charIndex = float(gl_VertexID >> 2);

                                float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s

                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
%s
                                // Pass effect data to fragment shader
                                effectData = vec4(float(effectType), speed, charIndex, param);
                            }
                        }%s
                    }
                """.formatted(
                config.fogDistanceInit.isEmpty() ? "" : "                        " + config.fogDistanceInit + "\n",
                config.vertexColorInit,
                config.moduloExpr,
                config.vertexColorAnimated,
                vertexEffects,
                config.fogDistanceRecalc.isEmpty() ? "" : "                            " + config.fogDistanceRecalc + "\n",
                scoreboardHiding
        );
    }

    private String getAnimationVertexShader(TextShaderTarget target, TextShaderFeatures features, boolean seeThrough) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String textShaderConstants = getTextShaderConstants(target, features);
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String vertexPrelude = snippets.vertexPrelude();
        String vertexEffects = snippets.vertexEffects();

        VertexShaderConfig config = createVertexShaderConfig(is1_21_6Plus, seeThrough);
        String mainBody = getVertexShaderMainBody(config, vertexEffects, false);

        if (is1_21_6Plus) {
            String header = seeThrough ? """
                #version 330

                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;

                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""" : """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;

                out float sphericalVertexDistance;
                out float cylindricalVertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(textShaderConstants, vertexPrelude) + mainBody;
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = "#moj_import <fog.glsl>";
            String header = seeThrough ? """
                #version 150

                %s

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;

                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform int FogShape;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""" : """
                #version 150

                %s

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform int FogShape;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(imports, textShaderConstants, vertexPrelude) + mainBody;
        }
    }

    private VertexShaderConfig createVertexShaderConfig(boolean is1_21_6Plus, boolean seeThrough) {
        if (is1_21_6Plus) {
            if (seeThrough) {
                return new VertexShaderConfig(
                        "",
                        "",
                        "Color",
                        "vec4(1.0, 1.0, 1.0, visible)",
                        "(rawFrame % totalFrames)"
                );
            } else {
                return new VertexShaderConfig(
                        "sphericalVertexDistance = fog_spherical_distance(pos);\n                        cylindricalVertexDistance = fog_cylindrical_distance(pos);",
                        "sphericalVertexDistance = fog_spherical_distance(pos);\n                            cylindricalVertexDistance = fog_cylindrical_distance(pos);",
                        "Color * texelFetch(Sampler2, UV2 / 16, 0)",
                        "vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0)",
                        "(rawFrame % totalFrames)"
                );
            }
        } else {
            if (seeThrough) {
                return new VertexShaderConfig(
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "Color",
                        "vec4(1.0, 1.0, 1.0, visible)",
                        "int(mod(float(rawFrame), float(totalFrames)))"
                );
            } else {
                return new VertexShaderConfig(
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "vertexDistance = fog_distance(pos, FogShape);",
                        "Color * texelFetch(Sampler2, UV2 / 16, 0)",
                        "vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0)",
                        "int(mod(float(rawFrame), float(totalFrames)))"
                );
            }
        }
    }

    private String getAnimationFragmentShader(TextShaderTarget target, boolean seeThrough) {
        return getAnimationFragmentShader(target, seeThrough, false);
    }

    private String getAnimationFragmentShader(TextShaderTarget target, boolean seeThrough, boolean intensity) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String sampleExpr = intensity ? "texture(Sampler0, texCoord0).rrrr" : "texture(Sampler0, texCoord0)";
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String fragmentPrelude = snippets.fragmentPrelude();
        String fragmentEffects = snippets.fragmentEffects();

        if (is1_21_6Plus) {
            if (seeThrough) {
                return """
                    #version 330

                    #moj_import <minecraft:dynamictransforms.glsl>
                    #moj_import <minecraft:globals.glsl>

                    uniform sampler2D Sampler0;

                    in vec4 vertexColor;
                    in vec2 texCoord0;
                    in vec4 effectData;

                    out vec4 fragColor;

                    %s

                    void main() {
                        vec4 color = %s * vertexColor * ColorModulator;
                        vec4 texColor = color;

                        // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                        if (effectData.x >= 0.0 && effectData.y > 0.5) {
                            int effectType = int(effectData.x + 0.5);
                            float speed = effectData.y;
                            float charIndex = effectData.z;
                            float param = effectData.w;
                            float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                        }

                        color = texColor;

                        if (color.a < 0.1) {
                            discard;
                        }
                        fragColor = color;
                    }
                    """.formatted(fragmentPrelude, sampleExpr, fragmentEffects);
            }

            return """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:globals.glsl>

                uniform sampler2D Sampler0;

                in float sphericalVertexDistance;
                in float cylindricalVertexDistance;
                in vec4 vertexColor;
                in vec2 texCoord0;
                in vec4 effectData;

                out vec4 fragColor;

                %s

                void main() {
                    vec4 color = %s * vertexColor * ColorModulator;
                    vec4 texColor = color;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                    }

                    color = texColor;

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
                }
                """.formatted(fragmentPrelude, sampleExpr, fragmentEffects);
        } else {
            // Pre-1.21.6: use traditional uniform declarations
            String imports = "#moj_import <fog.glsl>";

            return """
                #version 150

                %s

                uniform sampler2D Sampler0;
                uniform vec4 ColorModulator;
                uniform float FogStart;
                uniform float FogEnd;
                uniform vec4 FogColor;
                uniform float GameTime;

                in float vertexDistance;
                in vec4 vertexColor;
                in vec2 texCoord0;
                in vec4 effectData;

                out vec4 fragColor;

                %s

                void main() {
                    vec4 color = %s * vertexColor * ColorModulator;
                    vec4 texColor = color;

                    // Apply text effects if effectData.x >= 0 (effectType, 0 is rainbow)
                    if (effectData.x >= 0.0 && effectData.y > 0.5) {
                        int effectType = int(effectData.x + 0.5);
                        float speed = effectData.y;
                        float charIndex = effectData.z;
                        float param = effectData.w;
                        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);

%s
                    }

                    color = texColor;

                    if (color.a < 0.1) {
                        discard;
                    }
                    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
                }
                """.formatted(imports, fragmentPrelude, sampleExpr, fragmentEffects);
        }
    }

    private String build1_21ShaderJson(String shaderName, boolean seeThrough, boolean fullyQualifiedCoreRefs,
                                       boolean includeFog, boolean includeGameTime, boolean includeScreenSize) {
        String shaderRef = fullyQualifiedCoreRefs ? "minecraft:core/" + shaderName : shaderName;
        String samplers = seeThrough
                ? "{ \"name\": \"Sampler0\" }"
                : "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }";

        StringBuilder uniforms = new StringBuilder(UNIFORM_MATRIX);
        if (includeFog) uniforms.append(",\n").append(UNIFORM_FOG);
        if (includeGameTime) uniforms.append(",\n").append(UNIFORM_GAMETIME);
        if (includeScreenSize) uniforms.append(",\n").append(UNIFORM_SCREENSIZE);

        return """
            {
                "vertex": "%s",
                "fragment": "%s",
                "samplers": [ %s ],
                "uniforms": [
%s
                ]
            }
            """.formatted(shaderRef, shaderRef, samplers, uniforms.toString());
    }

    private String buildLegacyShaderJson(String shaderName, boolean hasUV2, boolean hasSampler2,
            boolean hasFog, boolean hasGameTime, boolean hasScreenSize) {
        String attributes = hasUV2
                ? "\"Position\", \"Color\", \"UV0\", \"UV2\""
                : "\"Position\", \"Color\", \"UV0\"";
        String samplers = hasSampler2
                ? "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }"
                : "{ \"name\": \"Sampler0\" }";

        StringBuilder uniforms = new StringBuilder(UNIFORM_MATRIX);
        if (hasFog) uniforms.append(",\n").append(UNIFORM_FOG);
        if (hasGameTime) uniforms.append(",\n").append(UNIFORM_GAMETIME);
        if (hasScreenSize) uniforms.append(",\n").append(UNIFORM_SCREENSIZE);

        return """
            {
                "blend": {
                    "func": "add",
                    "srcrgb": "srcalpha",
                    "dstrgb": "1-srcalpha"
                },
                "vertex": "%s",
                "fragment": "%s",
                "attributes": [ %s ],
                "samplers": [ %s ],
                "uniforms": [
%s
                ]
            }
            """.formatted(shaderName, shaderName, attributes, samplers, uniforms.toString());
    }

    private String getAnimationShaderJson(TextShaderTarget target, boolean seeThrough) {
        String shaderName = seeThrough ? "rendertype_text_see_through" : "rendertype_text";
        return getAnimationShaderJson(target, shaderName, seeThrough);
    }

    private String getAnimationShaderJson(TextShaderTarget target, String shaderName, boolean seeThrough) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        boolean is1_21_4Plus = target.isAtLeast("1.21.4");

        if (is1_21_6Plus) {
            String samplers = seeThrough
                    ? "{ \"name\": \"Sampler0\" }"
                    : "{ \"name\": \"Sampler0\" }, { \"name\": \"Sampler2\" }";
            return """
                {
                    "vertex": "minecraft:core/%s",
                    "fragment": "minecraft:core/%s",
                    "samplers": [ %s ]
                }
                """.formatted(shaderName, shaderName, samplers);
        } else if (is1_21_4Plus) {
            boolean includeFog = !seeThrough;
            return build1_21ShaderJson(shaderName, seeThrough, true, includeFog, true, false);
        } else {
            return buildLegacyShaderJson(shaderName, !seeThrough, !seeThrough, true, true, false);
        }
    }

    private String getCombinedVertexShader(TextShaderTarget target, TextShaderFeatures features) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");
        String textShaderConstants = getTextShaderConstants(target, features);
        TextEffectSnippets snippets = getTextEffectSnippets(target);
        String vertexPrelude = snippets.vertexPrelude();
        String vertexEffects = snippets.vertexEffects();

        // Combined shader always uses non-seeThrough config (has Sampler2, fog) with scoreboard hiding
        VertexShaderConfig config = createVertexShaderConfig(is1_21_6Plus, false);
        String mainBody = getVertexShaderMainBody(config, vertexEffects, true);

        if (is1_21_6Plus) {
            String header = """
                #version 330

                #moj_import <minecraft:fog.glsl>
                #moj_import <minecraft:dynamictransforms.glsl>
                #moj_import <minecraft:projection.glsl>
                #moj_import <minecraft:globals.glsl>

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform vec2 ScreenSize;

                out float sphericalVertexDistance;
                out float cylindricalVertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(textShaderConstants, vertexPrelude) + mainBody;
        } else {
            String imports = "#moj_import <fog.glsl>";
            String header = """
                #version 150

                %s

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;
                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;
                uniform int FogShape;
                uniform vec2 ScreenSize;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;
                out vec4 effectData;
                %s
                %s

""";
            return header.formatted(imports, textShaderConstants, vertexPrelude) + mainBody;
        }
    }

    private String getCombinedShaderJson(TextShaderTarget target) {
        boolean is1_21_6Plus = target.isAtLeast("1.21.6");

        if (is1_21_6Plus) {
            return """
                {
                    "vertex": "minecraft:core/rendertype_text",
                    "fragment": "minecraft:core/rendertype_text",
                    "samplers": [ { "name": "Sampler0" }, { "name": "Sampler2" } ],
                    "uniforms": [ { "name": "ScreenSize", "type": "float", "count": 2, "values": [ 1.0, 1.0 ] } ]
                }
                """;
        } else {
            return buildLegacyShaderJson("rendertype_text", true, true, true, true, true);
        }
    }

    private String getScoreboardVsh() {
        return """
                #version 150

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler2;

                uniform mat4 ModelViewMat;
                uniform mat4 ProjMat;

                uniform vec2 ScreenSize;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;

                void main() {
                    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                    vertexDistance = length((ModelViewMat * vec4(Position, 1.0)).xyz);
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                    texCoord0 = UV0;

                	// delete sidebar numbers
                	if(	Position.z == 0.0 && // check if the depth is correct (0 for gui texts)
                			gl_Position.x >= 0.95 && gl_Position.y >= -0.35 && // check if the position matches the sidebar
                			vertexColor.g == 84.0/255.0 && vertexColor.g == 84.0/255.0 && vertexColor.r == 252.0/255.0 && // check if the color is the sidebar red color
                			gl_VertexID <= 4 // check if it's the first character of a string
                		) gl_Position = ProjMat * ModelViewMat * vec4(ScreenSize + 100.0, 0.0, 0.0); // move the vertices offscreen, idk if this is a good solution for that but vec4(0.0) doesnt do the trick for everyone
                }
                """;
    }

    private String getScoreboardJson() {
        return """
                {
                    "blend": {
                        "func": "add",
                        "srcrgb": "srcalpha",
                        "dstrgb": "1-srcalpha"
                    },
                    "vertex": "rendertype_text",
                    "fragment": "rendertype_text",
                    "attributes": [
                        "Position",
                        "Color",
                        "UV0",
                        "UV2"
                    ],
                    "samplers": [
                        { "name": "Sampler0" },
                        { "name": "Sampler2" }
                    ],
                    "uniforms": [
                        { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },
                        { "name": "ColorModulator", "type": "float", "count": 4, "values": [ 1.0, 1.0, 1.0, 1.0 ] },
                        { "name": "FogStart", "type": "float", "count": 1, "values": [ 0.0 ] },
                        { "name": "FogEnd", "type": "float", "count": 1, "values": [ 1.0 ] },
                        { "name": "FogColor", "type": "float", "count": 4, "values": [ 0.0, 0.0, 0.0, 0.0 ] },
                		{ "name": "ScreenSize", "type": "float", "count": 2,  "values": [ 1.0, 1.0 ] }
                    ]
                }
                """;
    }

    private String getScoreboardBackground() {
        if (VersionUtil.atOrAbove("1.21"))
            return """
                    #version 150

                     in vec3 Position;
                     in vec4 Color;

                     uniform mat4 ModelViewMat;
                     uniform mat4 ProjMat;

                     out vec4 vertexColor;

                     void main() {
                     	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

                     	vertexColor = Color;

                     	//Isolating Scoreboard Display
                     	// Mojang Changed the Z position in 1.21, idk exact value but its huge
                     	if(gl_Position.y > -0.5 && gl_Position.y < 0.85 && gl_Position.x > 0.0 && gl_Position.x <= 1.0 && Position.z > 1000.0 && Position.z < 2750.0) {
                     		//vertexColor = vec4(vec3(0.0,0.0,1.0),1.0); // Debugger
                     		//SCOREBOARD.a = 0.0;
                     	}
                     	else {
                         	//vertexColor = vec4(vec3(1.0,0.0,0.0),1.0);
                     	}

                     	// Uncomment this if you want to make LIST invisible
                     	if(Position.z > 2750.0 && Position.z < 3000.0) {
                     		//TABLIST.a = 0.0;
                     	}
                     }

                    """;
        else
            return """
                    #version 150

                    in vec4 vertexColor;

                    uniform vec4 ColorModulator;

                    out vec4 fragColor;

                    bool isgray(vec4 a) {
                        return a.r == 0 && a.g == 0 && a.b == 0 && a.a < 0.3 && a.a > 0.29;
                    }

                    bool isdarkgray(vec4 a) {
                    	return a.r == 0 && a.g == 0 && a.b == 0 && a.a == 0.4;
                    }

                    void main() {

                        vec4 color = vertexColor;

                        if (color.a == 0.0) {
                            discard;
                        }

                        fragColor = color * ColorModulator;

                    	if(isgray(fragColor)){
                    		discard;
                    	}
                    	if(isdarkgray(fragColor)){
                    		discard;
                    	}
                    }
                    // Made by Reytz#9806 for minecraft 1.18.2""";
    }
}
