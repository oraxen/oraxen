package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class Glyph {

    private boolean fileChanged = false;

    private final String name;
    private boolean tabcomplete;
    private String tabIconTexture;
    private String tabIconSignature;
    private final char character;
    private String texture;
    private final int ascent;
    private final int height;
    private String permission = null;
    private String[] placeholders;
    private int code;

    public Glyph(final String glyphName, final ConfigurationSection glyphSection, int newCode) {
        name = glyphName;
        placeholders = new String[0];
        if (glyphSection.isConfigurationSection("chat")) {
            final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
            placeholders = chatSection.getStringList("placeholders").toArray(new String[0]);
            if (chatSection.isString("permission"))
                permission = chatSection.getString("permission");
            if (chatSection.isBoolean("tabcomplete"))
                tabcomplete = chatSection.getBoolean("tabcomplete");
            else tabcomplete = false;
            if (chatSection.isString("tabIconTexture")) {
                tabIconTexture = chatSection.getString("tabIconTexture");
            } else tabIconTexture = null;
            if (chatSection.isString("tabIconSignature")) {
                tabIconSignature = chatSection.getString("tabIconSignature");
            } else tabIconSignature = null;
        }
        texture = glyphSection.getString("texture");
        if (!texture.endsWith(".png"))
            texture += ".png";

        this.code = newCode;
        if (glyphSection.getInt("code", -1) != newCode && Settings.AUTOMATICALLY_SET_GLYPH_CODE.toBool()) {
            glyphSection.set("code", code);
            fileChanged = true;
        }

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

    public boolean hasTabCompletion() { return tabcomplete; }

    public String getTabIconTexture() {
        return tabIconTexture;
    }

    public String getTabIconSignature() {
        return tabIconSignature;
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

    public boolean hasPermission(final Player player) {
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }

    protected void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
