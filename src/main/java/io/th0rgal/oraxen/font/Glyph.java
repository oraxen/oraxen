package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Glyph {

    private boolean fileChanged = false;

    private final String name;
    private final boolean isEmoji;
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
        isEmoji = glyphSection.getBoolean("is_emoji", false);
        if (glyphSection.isConfigurationSection("chat")) {
            final ConfigurationSection chatSection = glyphSection.getConfigurationSection("chat");
            placeholders = chatSection.getStringList("placeholders").toArray(new String[0]);
            if (chatSection.isString("permission"))
                permission = chatSection.getString("permission");
            if (chatSection.isBoolean("tabcomplete"))
                tabcomplete = chatSection.getBoolean("tabcomplete");
            else tabcomplete = false;
            tabIconTexture = chatSection.getString("tab_icon_texture", "ewogICJ0aW1lc3RhbXAiIDogMTY0ODI4NzUzMTA4NywKICAicHJvZmlsZUlkIiA6ICJhYzM2YmVkZGQxNGQ0YjVmYmQyYzc5OThlMWMwOTg3ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYWlzYWthIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzU3ZGRlMGExN2MwNDY4MzI0MGIyNzJlYjJkODk1NWY4NzRiM2ExMTIyNTkxNjMwMGZlM2U4ODhkZjI4YjI4ZDciCiAgICB9CiAgfQp9");
            tabIconSignature = chatSection.getString("tab_icon_signature", "ScFX2rWxyhSyIn3YIke0ecNezt0bx+K8fRLQVPAzNli+X/dmod9ohujwwmDyog0exnlDAdEtXJY2XEUELpNLBHYZFyMsUChL4MObACzxGXExRBbyE8NzemmcRePKmglJfIHBxATlvG3VXeNn1dNA9g+GgLAB4KdqlDaYO5qAtdET7rckzLhSVeFz3yct3LuqZwzutdkJIbnR2Bu9kM4IpyowDbBEoASUp2ogNq4bQ+9O7cU7PtayknPGJaustHQR32jVcLYNqGweZKjZZUgER+6XrGAwyuWENQ00UpEWanHa2ahBugxzg1atcgc3spx3FxWLN0bVsUj4oXulPhxD4/44jALhHl7898qXwoRhqGaAuombJRH/bMoyoTZUDgcxmTbWFZcos9Ugg6eBlQo7ip35mW7fyd/8rk6RCGyf/wmqyDUnFNUHeQgJHHED+yr2oN/Y4jzCmG5Ikk1RExpi2Mbi7ouZx3bKzkTsEafGdnx8sMxenRCYFtcoQCcV4woQsk7xqqJyBVFiU4wXzHzMnbOhiRPJHlcqzJljFw2LuI97f8vlQGpW5KriFUWLSgKs1Zw4QOIdl5cv/oSZOvEAoi0s5EOB4rzVVE1XXLatYWOaRXy6Nbl8c9SH8UbkhcK5t+J54UIsvvuqq8AoDOX6yyw/wDORVlTaqy7pVXKZSQk=");
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

    public void verifyGlyph() {
        final File texture = new File(OraxenPlugin.get().getDataFolder().getAbsolutePath() + "/pack/textures", getTexture());
        boolean hasUpperCase = false;
        BufferedImage image = null;
        for (char c : getTexture().toCharArray()) if (Character.isUpperCase(c)) hasUpperCase = true;
        try { image = ImageIO.read(texture); } catch (IOException ignored) {}

        if (height < ascent) {
            this.setTexture("required/exit_icon");
            Logs.logError("The ascent is bigger than the height for " + name + ". This will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder image. You should edit this in the glyph config.");
        } else if (!texture.exists()) {
            this.setTexture("required/exit_icon");
            Logs.logError("The texture specified for " + name + " does not exist. This will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder image. You should edit this in the glyph config.");
        } else if (hasUpperCase) {
            this.setTexture("required/exit_icon");
            Logs.logError("The filename specified for " + name + " contains capital letters.");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should edit this in the glyph config and your textures filename.");
        } else if (texture.getName().contains(" ")) {
            this.setTexture("required/exit_icon");
            Logs.logError("The filename specified for " + name + " contains spaces.");
            Logs.logWarning("This will break all your glyphs. It has been temporarily set to a placeholder image.");
            Logs.logWarning("You should replace spaces with _ in your filename and glyph config.");
        } else if (image.getHeight() > 256 || image.getWidth() > 256) {
            this.setTexture("required/exit_icon");
            Logs.logError("The texture specified for " + name + " is larger than the supported size.");
            Logs.logWarning("The maximum image size is 256x256. Anything bigger will break all your glyphs.");
            Logs.logWarning("It has been temporarily set to a placeholder-image. You should edit this in the glyph config.");
        }
    }
}
