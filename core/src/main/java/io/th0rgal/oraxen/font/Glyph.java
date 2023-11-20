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
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Glyph {

    public static final Character WHITESPACE_GLYPH = '\ue000';

    private boolean fileChanged = false;

    private final String name;
    private final Key font = Key.key("default");
    private final boolean isEmoji;
    private final boolean tabcomplete;
    private final Character character;
    private String texture;
    private final int ascent;
    private final int height;
    private final String permission;
    private final String[] placeholders;
    private final BitMapEntry bitmapEntry;

    public final Pattern baseRegex;
    public final Pattern escapedRegex;

    public Glyph(final String glyphName, final ConfigurationSection glyphSection, char newChars) {
        name = glyphName;

        isEmoji = glyphSection.getBoolean("is_emoji", false);

        final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
        placeholders = chatSection != null ? chatSection.getStringList("placeholders").toArray(new String[0]) : new String[0];
        permission = chatSection != null ? chatSection.getString("permission", "") : "";
        tabcomplete = chatSection != null && chatSection.getBoolean("tabcomplete", false);

        String placeholderRegex = String.join("|", Arrays.stream(placeholders).map(Pattern::quote).toArray(String[]::new));
        String baseRegex = "((<(glyph|g):" + name + ")(:(c|colorable))*>" + (placeholders.length > 0 ?  "|" + placeholderRegex : "") + ")";
        this.baseRegex = Pattern.compile("(?<!\\\\)" + baseRegex);
        escapedRegex = Pattern.compile("\\\\" + baseRegex);

        if (glyphSection.contains("code")) {
            if (glyphSection.isInt("code")) glyphSection.set("char", (char) glyphSection.getInt("code"));
            glyphSection.set("code", null);
            fileChanged = true;
        }

        if (!glyphSection.contains("char") && !Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool()) {
            glyphSection.set("char", newChars);
            fileChanged = true;
        }

        character = glyphSection.get("char") != null ? glyphSection.getString("char", "").charAt(0) : null;


        ConfigurationSection bitmapSection = glyphSection.getConfigurationSection("bitmap");
        bitmapEntry = bitmapSection != null ? new BitMapEntry(bitmapSection.getString("id"), bitmapSection.getInt("row"), bitmapSection.getInt("column")) : null;
        ascent = getBitMap() != null ? getBitMap().ascent() : glyphSection.getInt("ascent", 8);
        height = getBitMap() != null ? getBitMap().height() : glyphSection.getInt("height", 8);
        texture = getBitMap() != null ? getBitMap().texture() : glyphSection.getString("texture", "required/exit_icon.png");
        if (!texture.endsWith(".png")) texture += ".png";
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

    public String getName() {
        return name;
    }

    public String getCharacter() {
        return character != null ? character.toString() : "";
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

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray chars = new JsonArray();
        chars.add(getCharacter());
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

    private final Set<String> materialNames = Arrays.stream(Material.values()).map(Material::name).collect(Collectors.toSet());

    public void verifyGlyph(List<Glyph> glyphs) {
        // Return on first run as files aren't generated yet
        Path packFolder = Path.of(OraxenPlugin.get().getDataFolder().getAbsolutePath()).resolve("pack");
        if (!packFolder.toFile().exists()) return;

        String texturePath = texture.contains(":") ? "assets/" + StringUtils.substringBefore(texture, ":") + "/textures/" : "textures/";
        texturePath = texturePath + (texture.contains(":") ? texture.split(":")[1] : texture);
        File textureFile;
        // If using minecraft as a namespace, make sure it is in assets or root pack-dir
        if (!StringUtils.substringBefore(texture, ":").equals("minecraft") || packFolder.resolve(texturePath).toFile().exists()) {
            textureFile = packFolder.resolve(texturePath).toFile();
            if (!textureFile.exists())
                textureFile = packFolder.resolve("assets/minecraft/" + texturePath).toFile();
        } else textureFile = packFolder.resolve(texturePath.replace("assets/minecraft/", "")).toFile();

        Map<Glyph, Boolean> sameCharMap = glyphs.stream().filter(g -> !g.name.equals(name) && !g.getCharacter().isBlank() && g.character.equals(character)).collect(Collectors.toMap(g -> g, g -> true));
        // Check if the texture is a vanilla item texture and therefore not in oraxen, but the vanilla pack
        boolean isMinecraftNamespace = !texture.contains(":") || texture.split(":")[0].equals("minecraft");
        String textureName = textureFile.getName().split("\\.")[0].toUpperCase();
        boolean isVanillaTexture = isMinecraftNamespace && materialNames.stream().anyMatch(textureName::contains);
        boolean hasUpperCase = false;
        BufferedImage image = null;
        for (char c : texturePath.toCharArray()) if (Character.isUpperCase(c)) hasUpperCase = true;
        try {
            image = ImageIO.read(textureFile);
        } catch (IOException ignored) {
        }

        if (height < ascent) {
            this.setTexture("required/exit_icon");
            Logs.logError("The ascent is bigger than the height for " + name + ". This will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder image. You should edit this in the glyph config.");
        } else if (!isVanillaTexture && (!textureFile.exists() || image == null)) {
            this.setTexture("required/exit_icon");
            Logs.logError("The texture specified for " + name + " does not exist. This will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder image. You should edit this in the glyph config.");
        } else if (hasUpperCase) {
            this.setTexture("required/exit_icon");
            Logs.logError("The filename specified for " + name + " contains capital letters.");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should edit this in the glyph config and your textures filename.");
        } else if (texturePath.contains(" ")) {
            this.setTexture("required/exit_icon");
            Logs.logError("The filename specified for " + name + " contains spaces.");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should replace spaces with _ in your filename and glyph config.");
        } else if (texturePath.contains("//")) {
            this.setTexture("required/exit_icon");
            Logs.logError("The filename specified for " + name + " contains double slashes.");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should make sure that the texture-path you have specified is correct.");
        } else if (!isVanillaTexture && !isBitMap() && (image.getHeight() > 256 || image.getWidth() > 256)) {
            this.setTexture("required/exit_icon");
            Logs.logError("The texture specified for " + name + " is larger than the supported size.");
            Logs.logWarning("The maximum image size is 256x256. Anything bigger will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder-image. You should edit this in the glyph config.");
        } else if (Settings.DISABLE_AUTOMATIC_GLYPH_CODE.toBool() && !sameCharMap.isEmpty()) {
            this.setTexture("required/exit_icon");
            Logs.logError(name + " code is the same as " + sameCharMap.keySet().stream().map(Glyph::getName).collect(Collectors.joining(", ")) + ".");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should edit the code of all these glyphs to be unique.");
        }
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
        return Component.textOfChildren(Component.text(getCharacter(), NamedTextColor.WHITE).font(font).hoverEvent(getGlyphHoverText()));
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
