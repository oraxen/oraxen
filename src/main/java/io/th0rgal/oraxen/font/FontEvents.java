package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.Arrays;
import java.util.Map;

public class FontEvents implements Listener {

    private final FontManager manager;

    public FontEvents(FontManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBookGlyph(final PlayerEditBookEvent event) {
        BookMeta meta = event.getNewBookMeta();
        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;

            for (Character character : manager.getReverseMap().keySet()) {
                if (!page.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), Template.template("permission", glyph.getPermission()));
                    event.setCancelled(true);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().getCharacter());
                if (entry.getValue().hasPermission(event.getPlayer()))
                    page = (manager.permsChatcolor == null)
                            ? page.replace(entry.getKey(), ChatColor.WHITE + unicode)
                            .replace(unicode, ChatColor.WHITE + unicode)
                            : page.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode);
                meta.setPage(i, page);
            }
        }
        event.setNewBookMeta(meta);
    }

    @EventHandler
    public void onSignGlyph(final SignChangeEvent event) {
        for (String line : event.getLines()) {
            int i = Arrays.stream(event.getLines()).toList().indexOf(line);
            if (i == -1) continue;
            for (Character character : manager.getReverseMap().keySet()) {
                if (!line.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), Template.template("permission", glyph.getPermission()));
                    event.setCancelled(true);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().getCharacter());
                if (entry.getValue().hasPermission(event.getPlayer()))
                    line = (manager.permsChatcolor == null)
                            ? line.replace(entry.getKey(), ChatColor.WHITE + unicode)
                            .replace(unicode, ChatColor.WHITE + unicode)
                            : line.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode);
            }
            event.setLine(i, line);
        }
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
                message = (manager.permsChatcolor == null)
                        ? message.replace(entry.getKey(),
                        String.valueOf(entry.getValue().getCharacter()))
                        : message.replace(entry.getKey(),
                        ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter())
                                + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor));

        event.setMessage(message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.sendGlyphTabCompletion(event.getPlayer(), true);
    }
}
