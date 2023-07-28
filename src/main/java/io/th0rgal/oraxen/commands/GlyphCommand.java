package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GlyphCommand {

    CommandAPICommand getGlyphCommand(ConfigurationSection commandsSection) {
        ConfigurationSection emojiSection = commandsSection.getConfigurationSection("emoji_list");
        List<Glyph> emojiList = OraxenPlugin.get().getFontManager().getEmojis().stream().toList();

        return new CommandAPICommand("emojis")
                .withPermission("oraxen.command.emojis").withPermission("oraxen.command.emoji")
                .executes((sender, args) -> {
                    Player player = ((Player) sender);
                    if (emojiSection == null) return;
                    boolean onlyShowPermissable = emojiSection.getBoolean("only_show_emojis_with_permission");

                    List<Glyph> emojis = !onlyShowPermissable
                            ? emojiList : emojiList.stream().filter(glyph -> glyph.hasPermission(player)).toList();
                    Collection<Component> list = new ArrayList<>();
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
                            String permissionMessage = "";
                            for (String placeholder : placeholders) {
                                if (Arrays.toString(placeholders).replace("]", "").endsWith(placeholder)) {
                                    finalString += placeholder;
                                } else {
                                    finalString += (placeholder + "\n");
                                }

                                if (!onlyShowPermissable) {
                                    permissionMessage += emoji.hasPermission(player) ?
                                            ("\n" + ChatColor.GREEN + "Permitted") : ("\n" + ChatColor.RED + "No Permission");
                                }
                            }

                            pages = pages.append(AdventureUtils.MINI_MESSAGE.deserialize("<white>" + emoji.getCharacter())
                                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.valueOf(emoji.getCharacter())))
                                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text(finalString + permissionMessage))));
                        }
                    }

                    Book book = Book.book(Component.text("Glyph Book"), Component.text("Oraxen"), pages);
                    OraxenPlugin.get().getAudience().player(player).openBook(book);
                });
    }
}
