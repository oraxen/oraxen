package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Glyph {

    public static final Character WHITESPACE_GLYPH = '\ue000';

    private boolean fileChanged = false;

    private final String name;
    private final boolean isEmoji;
    private final boolean tabcomplete;
    private final List<String> unicodeRows;
    private final GlyphGrid grid;
    private final GlyphAppearance appearance;
    private String texture;
    private final int ascent;
    private final int height;
    private final String permission;
    private final String[] placeholders;
    private final BitMapEntry bitmapEntry;

    public final Pattern baseRegex;
    public final Pattern escapedRegex;

    /**
     * Creates a Glyph with multi-character support.
     *
     * @param glyphName    The glyph identifier
     * @param glyphSection The configuration section
     * @param unicodeRows  List of unicode strings (one per row)
     * @param grid         The grid configuration
     */
    public Glyph(final String glyphName, final ConfigurationSection glyphSection,
                 @NotNull List<String> unicodeRows, @NotNull GlyphGrid grid) {
        name = glyphName;
        this.unicodeRows = new ArrayList<>(unicodeRows);
        this.grid = grid;

        isEmoji = glyphSection.getBoolean("is_emoji", false);

        final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
        placeholders = chatSection != null ? chatSection.getStringList("placeholders").toArray(new String[0]) : new String[0];
        permission = chatSection != null ? chatSection.getString("permission", "") : "";
        tabcomplete = chatSection != null && chatSection.getBoolean("tabcomplete", false);

        String placeholderRegex = String.join("|", Arrays.stream(placeholders).map(Pattern::quote).toArray(String[]::new));
        String baseRegex = "((<(glyph|g):" + name + ")(:(c|colorable|s|shadow)(:[^>]*)?)*>" + (placeholders.length > 0 ? "|" + placeholderRegex : "") + ")";
        this.baseRegex = Pattern.compile("(?<!\\\\)" + baseRegex);
        escapedRegex = Pattern.compile("\\\\" + baseRegex);

        // Parse appearance configuration
        appearance = GlyphAppearance.fromConfig(glyphSection.getConfigurationSection("appearance"));

        ConfigurationSection bitmapSection = glyphSection.getConfigurationSection("bitmap");
        bitmapEntry = bitmapSection != null ? new BitMapEntry(bitmapSection.getString("id"), bitmapSection.getInt("row"), bitmapSection.getInt("column")) : null;
        ascent = getBitMap() != null ? getBitMap().ascent() : glyphSection.getInt("ascent", 8);
        height = getBitMap() != null ? getBitMap().height() : glyphSection.getInt("height", 8);
        texture = getBitMap() != null ? getBitMap().texture() : glyphSection.getString("texture", "required/exit_icon.png");
        if (!texture.endsWith(".png")) texture += ".png";
    }

    /**
     * Legacy constructor for backward compatibility.
     * Creates a single-character glyph.
     *
     * @param glyphName    The glyph identifier
     * @param glyphSection The configuration section
     * @param character    The unicode character
     */
    public Glyph(final String glyphName, final ConfigurationSection glyphSection, char character) {
        this(glyphName, glyphSection, List.of(String.valueOf(character)), GlyphGrid.SINGLE);

        // Handle legacy config migration
        if (glyphSection.contains("code")) {
            if (glyphSection.isInt("code")) glyphSection.set("char", (char) glyphSection.getInt("code"));
            glyphSection.set("code", null);
            fileChanged = true;
        }

        if (!glyphSection.contains("char") && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
            glyphSection.set("char", character);
            fileChanged = true;
        }
    }

    public record BitMapEntry(String id, int row, int column) {
    }

    public BitMapEntry getBitmapEntry() {
        return bitmapEntry;
    }

    public String getBitmapId() {
        return bitmapEntry != null ? bitmapEntry.id : null;
    }

    public boolean hasBitmap() {
        return getBitmapId() != null;
    }

    public boolean isBitMap() {
        return FontManager.getGlyphBitMap(getBitmapId()) != null;
    }

    public FontManager.GlyphBitMap getBitMap() {
        return FontManager.getGlyphBitMap(getBitmapId());
    }

    public boolean isFileChanged() {
        return fileChanged;
    }

    public void setFileChanged(boolean changed) {
        this.fileChanged = changed;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the first character for backward compatibility.
     *
     * @return The first character of the first row, or empty string if empty
     */
    public String getCharacter() {
        if (unicodeRows.isEmpty() || unicodeRows.get(0).isEmpty()) return "";
        return String.valueOf(unicodeRows.get(0).charAt(0));
    }

    /**
     * Returns all unicode rows for multi-character glyphs.
     *
     * @return Unmodifiable list of unicode strings (one per row)
     */
    @NotNull
    public List<String> getUnicodeRows() {
        return Collections.unmodifiableList(unicodeRows);
    }

    /**
     * Flattens all rows into a single character array.
     *
     * @return All characters from all rows
     */
    public char[] getAllChars() {
        StringBuilder sb = new StringBuilder();
        for (String row : unicodeRows) {
            sb.append(row);
        }
        return sb.toString().toCharArray();
    }

    /**
     * Joins all rows with newlines for display purposes.
     *
     * @return Formatted unicode string with newlines between rows
     */
    public String getFormattedUnicodes() {
        return String.join("\n", unicodeRows);
    }

    /**
     * Returns the grid configuration for this glyph.
     *
     * @return The GlyphGrid configuration
     */
    @NotNull
    public GlyphGrid getGrid() {
        return grid;
    }

    /**
     * Returns the appearance configuration for this glyph.
     *
     * @return The GlyphAppearance configuration
     */
    @NotNull
    public GlyphAppearance getAppearance() {
        return appearance;
    }

    /**
     * Returns the font key for this glyph.
     *
     * @return The Adventure Key for the font
     */
    @NotNull
    public Key getFont() {
        return appearance.font();
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = (texture.endsWith(".png")) ? texture : texture + ".png";
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

    public boolean isEmoji() {
        return isEmoji;
    }

    public boolean hasTabCompletion() {
        return tabcomplete;
    }

    /**
     * Converts this glyph to JSON for resource pack generation.
     * Supports multi-character glyphs with multiple rows in the "chars" array.
     *
     * @return JsonObject for the font provider
     */
    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray chars = new JsonArray();

        // Add each row as a separate entry in the chars array
        for (String row : unicodeRows) {
            chars.add(row);
        }

        output.add("chars", chars);
        output.addProperty("file", texture);
        output.addProperty("ascent", ascent);
        output.addProperty("height", height);
        output.addProperty("type", "bitmap");
        return output;
    }

    public boolean hasPermission(Player player) {
        return player == null || permission.isEmpty() || player.hasPermission(permission);
    }

    private static final Set<String> MATERIAL_NAMES = Arrays.stream(Material.values())
            .map(Material::name)
            .collect(Collectors.toSet());

    /**
     * Verifies the glyph configuration and texture validity.
     *
     * @param glyphs List of other glyphs to check for conflicts
     */
    public void verifyGlyph(List<Glyph> glyphs) {
        Path packFolder = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath()).resolve("pack");
        if (!packFolder.toFile().exists()) return;

        VerificationContext ctx = buildVerificationContext(packFolder, glyphs);
        if (ctx == null) return;

        runValidations(ctx);
    }

    /**
     * Context object containing all data needed for glyph verification.
     */
    private record VerificationContext(
            File textureFile,
            String texturePath,
            BufferedImage image,
            boolean isVanillaTexture,
            boolean hasUpperCase,
            int maxWidth,
            int maxHeight,
            Set<String> conflictingGlyphs
    ) {}

    /**
     * Builds the verification context by resolving texture paths and loading images.
     */
    @Nullable
    private VerificationContext buildVerificationContext(Path packFolder, List<Glyph> glyphs) {
        String texturePath = resolveTexturePath();
        File textureFile = resolveTextureFile(packFolder, texturePath);

        boolean isMinecraftNamespace = !texture.contains(":") || texture.split(":")[0].equals("minecraft");
        String textureName = textureFile.getName().split("\\.")[0].toUpperCase();
        boolean isVanillaTexture = isMinecraftNamespace && MATERIAL_NAMES.stream().anyMatch(textureName::contains);
        boolean hasUpperCase = texturePath.chars().anyMatch(Character::isUpperCase);

        BufferedImage image = loadImage(textureFile);

        int maxWidth = grid.columns() * 256;
        int maxHeight = grid.rows() * 256;

        Set<String> conflictingGlyphs = findConflictingGlyphs(glyphs);

        return new VerificationContext(
                textureFile, texturePath, image,
                isVanillaTexture, hasUpperCase,
                maxWidth, maxHeight, conflictingGlyphs
        );
    }

    private String resolveTexturePath() {
        String basePath = texture.contains(":")
                ? "assets/" + StringUtils.substringBefore(texture, ":") + "/textures/"
                : "textures/";
        String textureName = texture.contains(":") ? texture.split(":")[1] : texture;
        return basePath + textureName;
    }

    private File resolveTextureFile(Path packFolder, String texturePath) {
        boolean isMinecraft = StringUtils.substringBefore(texture, ":").equals("minecraft");
        if (!isMinecraft || packFolder.resolve(texturePath).toFile().exists()) {
            File file = packFolder.resolve(texturePath).toFile();
            if (!file.exists()) {
                file = packFolder.resolve("assets/minecraft/" + texturePath).toFile();
            }
            return file;
        }
        return packFolder.resolve(texturePath.replace("assets/minecraft/", "")).toFile();
    }

    @Nullable
    private BufferedImage loadImage(File textureFile) {
        try {
            return ImageIO.read(textureFile);
        } catch (IOException e) {
            return null;
        }
    }

    private Set<String> findConflictingGlyphs(List<Glyph> glyphs) {
        char firstChar = getCharacter().isEmpty() ? Character.MIN_VALUE : getCharacter().charAt(0);
        return glyphs.stream()
                .filter(g -> !g.name.equals(name) && !g.getCharacter().isBlank() && g.getCharacter().charAt(0) == firstChar)
                .map(Glyph::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Runs all validation checks and sets placeholder texture if any fail.
     */
    private void runValidations(VerificationContext ctx) {
        String error = findValidationError(ctx);
        if (error != null) {
            setTexture("required/exit_icon");
            Logs.logError(error);
            Logs.logWarning("It has been temporarily set to a placeholder image. You should edit this in the glyph config.");
        }
    }

    @Nullable
    private String findValidationError(VerificationContext ctx) {
        if (height < ascent) {
            return "The ascent is bigger than the height for " + name + ". This will break all your glyphs.";
        }
        if (!ctx.isVanillaTexture && (!ctx.textureFile.exists() || ctx.image == null)) {
            return "The texture specified for " + name + " does not exist. This will break all your glyphs.";
        }
        if (ctx.hasUpperCase) {
            Logs.logWarning("You should edit this in the glyph config and your textures filename.");
            return "The filename specified for " + name + " contains capital letters. This will break all your glyphs.";
        }
        if (ctx.texturePath.contains(" ")) {
            Logs.logWarning("You should replace spaces with _ in your filename and glyph config.");
            return "The filename specified for " + name + " contains spaces. This will break all your glyphs.";
        }
        if (ctx.texturePath.contains("//")) {
            Logs.logWarning("You should make sure that the texture-path you have specified is correct.");
            return "The filename specified for " + name + " contains double slashes. This will break all your glyphs.";
        }
        if (!ctx.isVanillaTexture && !isBitMap() && ctx.image != null
                && (ctx.image.getHeight() > ctx.maxHeight || ctx.image.getWidth() > ctx.maxWidth)) {
            Logs.logWarning("The maximum image size is " + ctx.maxWidth + "x" + ctx.maxHeight + ".");
            return "The texture specified for " + name + " is larger than the supported size. This will break all your glyphs.";
        }
        if (Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool() && !ctx.conflictingGlyphs.isEmpty()) {
            Logs.logWarning("You should edit the code of all these glyphs to be unique.");
            return name + " code is the same as " + String.join(", ", ctx.conflictingGlyphs) + ". This will break all your glyphs.";
        }
        return null;
    }

    /**
     * Useful to easily get the MiniMessage-tag for a glyph
     */
    public String getGlyphTag() {
        return '<' + "glyph;" + name + '>';
    }


    public String getShortGlyphTag() {
        return "<g:" + name + '>';
    }

    public Component getGlyphComponent() {
        return Component.textOfChildren(Component.text(getCharacter(), NamedTextColor.WHITE).font(getFont()).hoverEvent(getGlyphHoverText()));
    }

    @Nullable
    public HoverEventSource getGlyphHoverText() {
        String hoverText = Settings.GLYPH_HOVER_TEXT.toString();
        TagResolver hoverResolver = TagResolver.builder().tag("glyph_placeholder", Tag.selfClosingInserting(Component.text(Arrays.stream(getPlaceholders()).findFirst().orElse("")))).build();
        Component hoverComponent = AdventureUtils.MINI_MESSAGE.deserialize(hoverText, hoverResolver);
        if (hoverText.isEmpty() || hoverComponent == Component.empty()) return null;
        return HoverEvent.showText(hoverComponent);
    }
}
