package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

import static net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
import static org.bukkit.inventory.meta.BookMeta.Generation.ORIGINAL;

public class GlyphCommand {

    public CommandAPICommand getGlyphCommand() {
        return new CommandAPICommand("emoji")
                .withPermission("oraxen.command.emoji")
                .withArguments(new TextArgument("type").replaceSuggestions(
                        ArgumentSuggestions.strings("list")))
                .executes((sender, args) -> {
                    Player player = ((Player) sender);
                    List<Glyph> emojis = OraxenPlugin.get().getFontManager().getEmojis().stream().filter(glyph -> glyph.hasPermission(player)).toList();
                    List<BaseComponent[]> list = new ArrayList<>();
                    BaseComponent[] page;
                    ComponentBuilder page2 = new ComponentBuilder("");
                    int s;

                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();
                    if (meta == null || emojis.isEmpty()) return;

                    meta.setTitle("Glyph Book");
                    meta.setAuthor("Oraxen");
                    meta.setGeneration(ORIGINAL);
                    pageLoop:
                    for (int p = 0; p < 50; p++) {
                        for (int i = 0; i < 256; i++) {
                            s = p * 256 + i + 1;
                            if (emojis.size() < s) break pageLoop;
                            Glyph emoji = (emojis.get(p * 256 + i));
                             page = new ComponentBuilder(ChatColor.WHITE + String.valueOf(emoji.getCharacter()))
                                    .event(new ClickEvent(SUGGEST_COMMAND, String.valueOf(emoji.getCharacter())))
                                    .event(new HoverEvent(SHOW_TEXT, new ComponentBuilder(":"+emoji.getName()+":").create()))
                                    .create();
                             list.add(page);
                        }
                    }
                    for (BaseComponent[] b : list)
                        page2.append(b);
                    meta.spigot().setPages(page2.create());
                    book.setItemMeta(meta);
                    player.openBook(book);
                });
    }
}
