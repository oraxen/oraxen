package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.config.Message;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class FontEvents implements Listener {

    private final FontManager manager;

    public FontEvents(FontManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        for (String word : message.split(" ")) {
            Glyph glyph = manager.getGlyphFromPlaceholder(word);
            if (glyph != null && glyph.hasPermission(event.getPlayer()))
                message = message.replace(word, String.valueOf(glyph.character()));
        }
        for (Character character : manager.getReverseMap().keySet()) {
            if (!message.contains(String.valueOf(character)))
                continue;

            Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
            if (!glyph.hasPermission(event.getPlayer())) {
                Message.NO_PERMISSION.send(event.getPlayer(), "permission", glyph.permission());
                event.setCancelled(true);
            }

        }
        event.setMessage(message);
    }

}
