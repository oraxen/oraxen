package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.glyphs.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;

public class GlyphCommand {

    public OraxenCommand getGlyphCommand() {
        // Accept both the plural and singular permission: 1.218's chained withPermission calls
        // overwrote each other, making oraxen.command.emoji the effective permission, so users
        // granted only the singular form keep access.
        return new OraxenCommand("emojis")
                .withPermission("oraxen.command.emojis;oraxen.command.emoji")
                .executesPlayer((player, args) -> {
                    List<Glyph> emojiList = OraxenPlugin.get().getFontManager().getEmojis().stream().toList();
                    boolean onlyShowPermissable = Settings.SHOW_PERMISSION_EMOJIS.toBool();

                    List<Glyph> emojis = !onlyShowPermissable ? emojiList
                            : emojiList.stream().filter(glyph -> glyph.hasPermission(player)).toList();
                    Component pages = Component.empty();
                    int s;

                    if (emojis.isEmpty()) {
                        Message.NO_EMOJIS.send(player);
                        return;
                    }

                    pageLoop:
                    for (int p = 0; p < 50; p++) {
                        for (int i = 0; i < 256; i++) {
                            s = p * 256 + i + 1;
                            if (emojis.size() < s) break pageLoop;
                            Glyph emoji = (emojis.get(p * 256 + i));
                            String[] placeholders = emoji.getPlaceholders();
                            String finalString = "";
                            Component permissionMessage = Component.empty();
                            for (String placeholder : placeholders) {
                                if (Arrays.toString(placeholders).replace("]", "").endsWith(placeholder)) {
                                    finalString += placeholder;
                                } else {
                                    finalString += (placeholder + "\n");
                                }

                                if (!onlyShowPermissable) {
                                    permissionMessage = permissionMessage.append(emoji.hasPermission(player)
                                            ? Component.text("\nPermitted.", NamedTextColor.GREEN)
                                            : Component.text("\nNo Permission.", NamedTextColor.RED));
                                }
                            }

                            pages = pages.append(AdventureUtils.MINI_MESSAGE.deserialize("<glyph:" + emoji.getName() + ">")
                                    .clickEvent(ClickEvent.copyToClipboard(emoji.getCharacters()))
                                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.text(finalString).append(permissionMessage))));
                        }
                    }

                    Book book = Book.book(Component.text("Glyph Book"), Component.text("Oraxen"), List.of(pages));
                    AdventureUtils.openBook(player, book);
                });
    }
}
