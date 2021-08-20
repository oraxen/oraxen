package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


public class Glyph {

    private static int lastCode = -1;
    private boolean fileChanged = false;

    private final String name;
    private final char character;
    private String texture;
    private final int ascent;
    private final int height;
    private String permission = null;
    private String[] placeholders;

    public Glyph(final String glyphName, final ConfigurationSection glyphSection) {
        name = glyphName;
        placeholders = new String[0];
        if (glyphSection.isConfigurationSection("chat")) {
            final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
            placeholders = chatSection.getStringList("placeholders").toArray(new String[0]);
            if (chatSection.isString("permission"))
                permission = chatSection.getString("permission");
        }
        texture = glyphSection.getString("texture");
        if (!texture.endsWith(".png"))
            texture += ".png";
        int code;
        if (glyphSection.isInt("code"))
            code = glyphSection.getInt("code");
        else {
            code = lastCode != -1 ? ++lastCode : 3000;
            if (Settings.AUTOMATICALLY_SET_GLYPH_CODE.toBool()) {
                glyphSection.set("code", code);
                fileChanged = true;
            }
        }
        lastCode = code;
        character = (char) code;
        ascent = glyphSection.getInt("ascent");
        height = glyphSection.getInt("height");
    }

    public boolean isFileChanged() {
        return fileChanged;
    }

    public String getName() {
        return name;
    }

    public char getCharacter() {
        return character;
    }

    public String getTexture() {
        return texture;
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

    public JsonObject toJson() {
        final JsonObject output = new JsonObject();
        final JsonArray chars = new JsonArray();
        chars.add(getHexcode());
        output.add("chars", chars);
        output.addProperty("file", texture);
        output.addProperty("ascent", ascent);
        output.addProperty("height", height);
        output.addProperty("type", "bitmap");
        return output;
    }

    public String getHexcode() { // unfortunately we don't have the choice, this is a windows bug
        final String hexCode = Integer.toHexString(character);
        return "\\u" + ("0000" + hexCode).substring(hexCode.length());
    }

    public boolean hasPermission(final Player player) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

}
