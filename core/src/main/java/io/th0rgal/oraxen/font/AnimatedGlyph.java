package io.th0rgal.oraxen.font;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An animated glyph that displays frames from a PNG sprite sheet.
 * <p>
 * Animation is achieved through shaders that detect a magic color and
 * offset the UV coordinates based on game time.
 * <p>
 * Example configuration:
 * 
 * <pre>
 * loading:
 *   texture: animations/loading      # Path to PNG sprite sheet (frames stacked vertically)
 *   animation:
 *     frames: 24                      # Number of frames in sprite sheet
 *     fps: 12                         # Frames per second (1-127, default: 10)
 *     loop: true                      # Whether to loop (default: true)
 *                                     # If false, animation plays once then stays on last frame
 *   ascent: 8
 *   height: 16
 *   chat:
 *     placeholders: [":loading:"]
 * </pre>
 * <p>
 * <b>Note:</b> Non-looping animations ({@code loop: false}) play relative to
 * the server's
 * GameTime, not relative to when the glyph first appeared. This means all
 * instances of
 * a non-looping animation will be synchronized.
 */
public class AnimatedGlyph {

    /**
     * Magic color encoding scheme for animated glyphs.
     * <p>
     * Format: 0xFFGGBB where:
     * - FF = Red channel (always 0xFF for animation marker)
     * - GG = Green channel encodes FPS directly (1-255)
     * - BB = Blue channel encodes frame count (1-255)
     * <p>
     * Shader detects R=0xFF and G in valid FPS range as animated.
     */
    public static final int MAGIC_RED = 0xFF;

    /**
     * Default frames per second for animations.
     */
    public static final int DEFAULT_FPS = 10;

    /**
     * Minimum valid FPS value.
     */
    public static final int MIN_FPS = 1;

    /**
     * Maximum valid FPS value.
     * Limited to 127 because bit 7 of the green channel is reserved for the loop
     * flag.
     * Encoding: G = (loop ? 0 : 0x80) | (fps & 0x7F)
     */
    public static final int MAX_FPS = 127;

    /**
     * Starting codepoint for animated glyph characters.
     * Uses BMP Private Use Area (U+E000 to U+F7FF) to avoid encoding issues.
     */
    private static final int BASE_CODEPOINT = 0xE000;

    /**
     * Maximum codepoint for animated glyphs.
     * Limited to U+F7FF to reserve U+F800-U+F8FF for ShiftProvider codepoints.
     * This gives 6144 codepoints for animation frames (0xF7FF - 0xE000 + 1).
     */
    private static final int MAX_CODEPOINT = 0xF7FF;

    private static int nextCodepoint = BASE_CODEPOINT;

    private final String name;
    private final String texturePath;
    private int frameCount;
    private final int fps;
    private final boolean loop;
    private final int offset;
    private final int ascent;
    private final int height;
    private final String permission;
    private final String[] placeholders;
    private final boolean tabcomplete;
    private final GlyphAppearance appearance;

    /**
     * Codepoints assigned to each frame.
     * Uses BMP PUA so all values fit in a single char (U+E000 to U+F7FF).
     */
    private final List<Integer> frameCodepoints;

    /**
     * Codepoint used for offset space provider (if offset != 0).
     * Allocated during construction to avoid collisions after codepoint counter
     * reset.
     */
    private int offsetCodepoint = -1;

    /**
     * Path to the sprite sheet PNG (user-provided or resource pack location).
     */
    private String spriteSheetPath;

    /**
     * Whether the sprite sheet has been validated and is ready for use.
     */
    private boolean processed = false;

    public final Pattern baseRegex;
    public final Pattern escapedRegex;

    /**
     * Creates an animated glyph from configuration.
     *
     * @param glyphName    The name/ID of this animated glyph
     * @param glyphSection The configuration section
     */
    public AnimatedGlyph(String glyphName, ConfigurationSection glyphSection) {
        this.name = glyphName;

        // Parse texture path (required)
        this.texturePath = glyphSection.getString("texture");
        if (texturePath == null || texturePath.isBlank()) {
            throw new IllegalArgumentException("Animated glyph '" + glyphName + "' missing 'texture' path");
        }

        // Parse animation section
        ConfigurationSection animSection = glyphSection.getConfigurationSection("animation");
        if (animSection == null) {
            throw new IllegalArgumentException("Animated glyph '" + glyphName + "' missing 'animation' section");
        }

        this.frameCount = animSection.getInt("frames", -1);
        if (frameCount <= 0) {
            throw new IllegalArgumentException(
                    "Animated glyph '" + glyphName + "' must specify 'animation.frames' > 0");
        }

        this.fps = Math.max(MIN_FPS, Math.min(MAX_FPS, animSection.getInt("fps", DEFAULT_FPS)));
        this.loop = animSection.getBoolean("loop", true);
        this.offset = animSection.getInt("offset", 0);

        this.ascent = glyphSection.getInt("ascent", 8);
        this.height = glyphSection.getInt("height", 8);

        // Parse appearance
        this.appearance = GlyphAppearance.fromConfig(glyphSection.getConfigurationSection("appearance"));

        // Parse chat section
        ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
        placeholders = chatSection != null
                ? chatSection.getStringList("placeholders").toArray(new String[0])
                : new String[0];
        permission = chatSection != null ? chatSection.getString("permission", "") : "";
        tabcomplete = chatSection != null && chatSection.getBoolean("tabcomplete", false);

        this.frameCodepoints = new ArrayList<>();

        // Allocate frame codepoints immediately since frameCount is known
        allocateFrameCodepoints();

        // Build regex patterns
        String placeholderRegex = String.join("|",
                Arrays.stream(placeholders).map(Pattern::quote).toArray(String[]::new));
        String baseRegexPattern = "((<(glyph|g):" + name + ")(:[^:>]+)*>"
                + (placeholders.length > 0 ? "|" + placeholderRegex : "") + ")";
        this.baseRegex = Pattern.compile("(?<!\\\\)" + baseRegexPattern);
        this.escapedRegex = Pattern.compile("\\\\" + baseRegexPattern);
    }

    /**
     * Checks if a configuration section defines an animated glyph.
     * An animated glyph must have both 'texture' and 'animation' section.
     *
     * @param section The configuration section to check
     * @return true if the section defines an animated glyph
     */
    public static boolean isAnimatedGlyph(@Nullable ConfigurationSection section) {
        return section != null
                && section.contains("texture")
                && section.isConfigurationSection("animation");
    }

    /**
     * Gets the full path to the sprite sheet PNG file.
     *
     * @param packFolder The pack folder path
     * @return The full file path
     */
    public File getTextureFile(Path packFolder) {
        String path = texturePath.endsWith(".png") ? texturePath : texturePath + ".png";
        // Look in textures folder
        return packFolder.resolve("textures").resolve(path).toFile();
    }

    /**
     * Allocates unique codepoints for each frame and offset (if needed).
     * Uses BMP Private Use Area (U+E000 to U+F8FF) for maximum compatibility.
     * <p>
     * All codepoints are allocated upfront during construction to ensure they
     * remain valid even after {@link #resetCodepointCounter()} is called.
     */
    private void allocateFrameCodepoints() {
        frameCodepoints.clear();
        for (int i = 0; i < frameCount; i++) {
            if (nextCodepoint > MAX_CODEPOINT) {
                Logs.logError("Animated glyph codepoint overflow! Too many animation frames defined.");
                Logs.logWarning("Consider reducing total animation frames across all animated glyphs.");
                break;
            }
            frameCodepoints.add(nextCodepoint++);
        }

        // Pre-allocate offset codepoint if offset is specified
        // This prevents collision after resetCodepointCounter() is called
        if (offset != 0 && nextCodepoint <= MAX_CODEPOINT) {
            offsetCodepoint = nextCodepoint++;
        }
    }

    /**
     * Resets the codepoint counter. Used during reload.
     */
    public static void resetCodepointCounter() {
        nextCodepoint = BASE_CODEPOINT;
    }

    /**
     * Marks this glyph as processed with the sprite sheet path.
     *
     * @param spriteSheetPath Path to the generated sprite sheet
     */
    public void setProcessed(String spriteSheetPath) {
        this.spriteSheetPath = spriteSheetPath;
        this.processed = true;
    }

    /**
     * Gets the font key for this animated glyph.
     * Animated glyphs use a separate font file.
     *
     * @return Font key in format oraxen:animations/<name>
     */
    @NotNull
    public Key getAnimationFont() {
        return Key.key("oraxen", "animations/" + name);
    }

    /**
     * Gets the appearance font (for normal glyph rendering).
     */
    @NotNull
    public Key getFont() {
        return appearance.font();
    }

    @NotNull
    public GlyphAppearance getAppearance() {
        return appearance;
    }

    public String getName() {
        return name;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getFps() {
        return fps;
    }

    public boolean isLooping() {
        return loop;
    }

    public int getOffset() {
        return offset;
    }

    public int getAscent() {
        return ascent;
    }

    public int getHeight() {
        return height;
    }

    public String getPermission() {
        return permission;
    }

    public String[] getPlaceholders() {
        return placeholders;
    }

    public boolean hasTabCompletion() {
        return tabcomplete;
    }

    public boolean isProcessed() {
        return processed;
    }

    public String getSpriteSheetPath() {
        return spriteSheetPath;
    }

    /**
     * Gets the first frame character for simple display.
     */
    public String getCharacter() {
        return frameCodepoints.isEmpty() ? "" : Character.toString(frameCodepoints.get(0));
    }

    /**
     * Gets all frame characters as a single string.
     * Properly handles supplementary codepoints using Character.toString(int).
     */
    public String getAllFrameChars() {
        StringBuilder sb = new StringBuilder();
        for (int codepoint : frameCodepoints) {
            sb.append(Character.toString(codepoint));
        }
        return sb.toString();
    }

    /**
     * Gets the codepoint for a specific frame.
     *
     * @param frame Frame index (0-based)
     * @return The frame codepoint, or -1 if out of bounds and no frames exist
     */
    public int getFrameCodepoint(int frame) {
        if (frame < 0 || frame >= frameCodepoints.size()) {
            return frameCodepoints.isEmpty() ? -1 : frameCodepoints.get(0);
        }
        return frameCodepoints.get(frame);
    }

    /**
     * Gets the string representation for a specific frame.
     *
     * @param frame Frame index (0-based)
     * @return The frame character as a string, or empty string if out of bounds
     */
    public String getFrameString(int frame) {
        int codepoint = getFrameCodepoint(frame);
        return codepoint < 0 ? "" : Character.toString(codepoint);
    }

    public boolean hasPermission(Player player) {
        return player == null || permission.isEmpty() || player.hasPermission(permission);
    }

    /**
     * Generates the font JSON for this animated glyph.
     * Creates a bitmap provider with all frames stacked vertically,
     * plus a space provider for the offset reset.
     *
     * @return JsonObject containing the font definition
     */
    public JsonObject toFontJson() {
        if (!processed || spriteSheetPath == null) {
            Logs.logWarning("Animated glyph '" + name + "' not processed, cannot generate font JSON");
            return null;
        }

        JsonObject font = new JsonObject();
        JsonArray providers = new JsonArray();

        // Bitmap provider with all frames
        JsonObject bitmap = new JsonObject();
        bitmap.addProperty("type", "bitmap");
        bitmap.addProperty("file", spriteSheetPath);
        bitmap.addProperty("ascent", ascent);
        bitmap.addProperty("height", height);

        JsonArray chars = new JsonArray();
        for (int codepoint : frameCodepoints) {
            // Use Character.toString(int) to properly handle supplementary codepoints
            chars.add(Character.toString(codepoint));
        }
        bitmap.add("chars", chars);
        providers.add(bitmap);

        // Space provider for offset reset if needed
        // Uses pre-allocated offsetCodepoint to avoid collision after counter reset
        if (offset != 0 && offsetCodepoint >= 0) {
            JsonObject space = new JsonObject();
            space.addProperty("type", "space");
            JsonObject advances = new JsonObject();
            advances.addProperty(Character.toString(offsetCodepoint), offset);
            space.add("advances", advances);
            providers.add(space);
        }

        font.add("providers", providers);
        return font;
    }

    /**
     * Generates the animation component with the first frame character and magic
     * color.
     * The shader cycles through frames via UV remapping based on game time,
     * so only the first frame character is needed.
     *
     * @return Component containing first frame character with magic color for
     *         shader detection
     */
    public Component getAnimationComponent() {
        return Component.text(getCharacter())
                .font(getAnimationFont())
                .color(getMagicColor());
    }

    /**
     * Gets a static component showing just the first frame.
     * Used as fallback for clients without shader support.
     */
    public Component getStaticComponent() {
        return Component.text(getCharacter())
                .font(getAnimationFont())
                .color(TextColor.color(0xFFFFFF));
    }

    public String getGlyphTag() {
        return "<glyph:" + name + ">";
    }

    public String getShortGlyphTag() {
        return "<g:" + name + ">";
    }

    public Component getGlyphComponent() {
        if (processed) {
            return getAnimationComponent();
        }
        return Component.text(getGlyphTag());
    }

    @Nullable
    public HoverEventSource<?> getGlyphHoverText() {
        // Show actual allocated frames (may differ from config if codepoint overflow
        // occurred)
        int actualFrames = frameCodepoints.size();
        return HoverEvent
                .showText(Component.text("Animated: " + name + " (" + actualFrames + " frames @ " + fps + " fps)"));
    }

    /**
     * Gets the magic color for this glyph encoding FPS, frame count, and loop flag.
     * <p>
     * Color format: 0xFFGGBB
     * - R (0xFF): Animation marker
     * - G: Bit 7 = loop flag (0=loop, 1=no-loop), Bits 0-6 = FPS (1-127)
     * - B: Frame count (1-255)
     *
     * @return The magic color with encoded animation parameters
     */
    public TextColor getMagicColor() {
        // Encode FPS in lower 7 bits of green channel (clamped to 1-127)
        int fpsValue = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));

        // Encode loop flag in bit 7: 0 = looping (default), 0x80 = not looping
        int loopBit = loop ? 0 : 0x80;
        int greenChannel = loopBit | fpsValue;

        // Encode actual allocated frame count in blue channel (clamped to 1-255)
        // Use frameCodepoints.size() instead of frameCount to handle codepoint overflow
        int actualFrames = frameCodepoints.size();
        int frameBlue = Math.max(1, Math.min(255, actualFrames));

        // Combine into RGB: 0xFFGGBB
        int rgb = (MAGIC_RED << 16) | (greenChannel << 8) | frameBlue;
        return TextColor.color(rgb);
    }

    /**
     * Checks if a color value represents an animated glyph.
     * Animation colors have R=0xFF and G encodes loop flag + FPS.
     * Also checks for shadow variant (colors divided by 4).
     *
     * @param color The color value to check
     * @return true if this is an animation magic color (primary or shadow)
     */
    public static boolean isAnimationColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;

        // Primary color: R=0xFF, G has FPS in lower 7 bits (1-127) and loop flag in bit
        // 7
        // G range is 1-255 (1-127 for looping, 129-255 for non-looping)
        if (r == MAGIC_RED && g >= MIN_FPS) {
            return true;
        }

        // Shadow color: Minecraft renders shadows by dividing color by 4
        // So R≈0x3F (63-64), G≈(loopBit|fps)/4
        int shadowRed = MAGIC_RED / 4;
        if (r >= shadowRed - 1 && r <= shadowRed + 1 && g >= 1) {
            return true;
        }

        return false;
    }

    /**
     * Extracts animation parameters from a magic color.
     * Handles both primary colors and shadow variants.
     *
     * @param color The color value
     * @return int array of [fps, frameCount, loop (1=true, 0=false)], or null if
     *         not an animation color
     */
    @Nullable
    public static int[] extractAnimationParams(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Primary color: R=0xFF
        if (r == MAGIC_RED && g >= MIN_FPS) {
            // Extract loop flag from bit 7, FPS from bits 0-6
            int loopFlag = (g & 0x80) == 0 ? 1 : 0; // 0x80 set means NOT looping
            int fps = g & 0x7F;
            return new int[] { Math.max(MIN_FPS, fps), b, loopFlag };
        }

        // Shadow color: multiply by 4 to recover original
        int shadowRed = MAGIC_RED / 4;
        if (r >= shadowRed - 1 && r <= shadowRed + 1 && g >= 1) {
            // Recover original green value (with loop bit) by multiplying by 4
            int originalG = Math.min(255, g * 4);
            int loopFlag = (originalG & 0x80) == 0 ? 1 : 0;
            int fps = Math.min(MAX_FPS, originalG & 0x7F);
            int frames = Math.min(255, b * 4);
            return new int[] { Math.max(MIN_FPS, fps), Math.max(1, frames), loopFlag };
        }

        return null;
    }
}
