package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

public class PapiAliases {

    private PapiAliases() {}

    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public static GsonComponentSerializer gson = GsonComponentSerializer.gson();
    public static String readJson(String text) {
        // Serialize initial string from json to component, then parse to handle tags and serialize again to json string
        return Utils.MINI_MESSAGE.serialize(gson.deserialize(text)).replace("\\", "");
    }
}
