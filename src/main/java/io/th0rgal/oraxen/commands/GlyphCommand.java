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

import java.util.ArrayList;
import java.util.List;

public class GlyphCommand {

    private static final int EMOJIS_PER_PAGE = 144;
    private static final int MAX_PAGES = 50;

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

                    if (emojis.isEmpty()) {
                        Message.NO_EMOJIS.send(player);
                        return;
                    }

                    List<Component> pages = new ArrayList<>();
                    int shown = Math.min(emojis.size(), EMOJIS_PER_PAGE * MAX_PAGES);
                    for (int start = 0; start < shown; start += EMOJIS_PER_PAGE) {
                        Component page = Component.empty();
                        for (Glyph emoji : emojis.subList(start, Math.min(start + EMOJIS_PER_PAGE, shown))) {
                            Component hover = Component.text(String.join("\n", emoji.getPlaceholders()));
                            if (!onlyShowPermissable) {
                                hover = hover.append(emoji.hasPermission(player)
                                        ? Component.text("\nPermitted.", NamedTextColor.GREEN)
                                        : Component.text("\nNo Permission.", NamedTextColor.RED));
                            }

                            page = page.append(AdventureUtils.MINI_MESSAGE.deserialize("<glyph:" + emoji.getName() + ">")
                                    .clickEvent(ClickEvent.copyToClipboard(emoji.getCharacters()))
                                    .hoverEvent(HoverEvent.showText(hover)));
                        }
                        pages.add(page);
                    }

                    Book book = Book.book(Component.text("Glyph Book"), Component.text("Oraxen"), pages);
                    AdventureUtils.openBook(player, book);
                });
    }
}
