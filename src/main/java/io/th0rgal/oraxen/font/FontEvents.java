package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;

public class FontEvents implements Listener {

    private final FontManager manager;

    public FontEvents(FontManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        for (Character character : manager.getReverseMap().keySet()) {
            if (!message.contains(String.valueOf(character)))
                continue;
            Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
            if (!glyph.hasPermission(event.getPlayer())) {
                Message.NO_PERMISSION.send(event.getPlayer(), Template.template("permission", glyph.getPermission()));
                event.setCancelled(true);
            }
        }
        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet())
            if (entry.getValue().hasPermission(event.getPlayer()))
                message = (!manager.useLuckPermsChatColor)
                        ? message.replace(entry.getKey(),
                        String.valueOf(entry.getValue().getCharacter()))
                        : message.replace(entry.getKey(),
                        ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter()))
                        + PapiAliases.setPlaceholders(event.getPlayer(), "%luckperms_meta_chatcolor%");

        event.setMessage(message);
    }

}
