package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.th0rgal.oraxen.config.Settings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


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
            tabIconTexture = chatSection.getString("tabIconTexture", "ewogICJ0aW1lc3RhbXAiIDogMTY0ODI4NzUzMTA4NywKICAicHJvZmlsZUlkIiA6ICJhYzM2YmVkZGQxNGQ0YjVmYmQyYzc5OThlMWMwOTg3ZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYWlzYWthIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzU3ZGRlMGExN2MwNDY4MzI0MGIyNzJlYjJkODk1NWY4NzRiM2ExMTIyNTkxNjMwMGZlM2U4ODhkZjI4YjI4ZDciCiAgICB9CiAgfQp9");
            tabIconSignature = chatSection.getString("tabIconSignature", "ScFX2rWxyhSyIn3YIke0ecNezt0bx+K8fRLQVPAzNli+X/dmod9ohujwwmDyog0exnlDAdEtXJY2XEUELpNLBHYZFyMsUChL4MObACzxGXExRBbyE8NzemmcRePKmglJfIHBxATlvG3VXeNn1dNA9g+GgLAB4KdqlDaYO5qAtdET7rckzLhSVeFz3yct3LuqZwzutdkJIbnR2Bu9kM4IpyowDbBEoASUp2ogNq4bQ+9O7cU7PtayknPGJaustHQR32jVcLYNqGweZKjZZUgER+6XrGAwyuWENQ00UpEWanHa2ahBugxzg1atcgc3spx3FxWLN0bVsUj4oXulPhxD4/44jALhHl7898qXwoRhqGaAuombJRH/bMoyoTZUDgcxmTbWFZcos9Ugg6eBlQo7ip35mW7fyd/8rk6RCGyf/wmqyDUnFNUHeQgJHHED+yr2oN/Y4jzCmG5Ikk1RExpi2Mbi7ouZx3bKzkTsEafGdnx8sMxenRCYFtcoQCcV4woQsk7xqqJyBVFiU4wXzHzMnbOhiRPJHlcqzJljFw2LuI97f8vlQGpW5KriFUWLSgKs1Zw4QOIdl5cv/oSZOvEAoi0s5EOB4rzVVE1XXLatYWOaRXy6Nbl8c9SH8UbkhcK5t+J54UIsvvuqq8AoDOX6yyw/wDORVlTaqy7pVXKZSQk=");
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
