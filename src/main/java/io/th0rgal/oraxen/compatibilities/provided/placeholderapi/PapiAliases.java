package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PapiAliases {

    private PapiAliases() {}

    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
