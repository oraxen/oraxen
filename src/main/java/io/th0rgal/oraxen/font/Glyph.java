package io.th0rgal.oraxen.font;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

public record Glyph(String name, char character, String texture, int ascent,
                    int height, String permission, String... placeholders) {

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
