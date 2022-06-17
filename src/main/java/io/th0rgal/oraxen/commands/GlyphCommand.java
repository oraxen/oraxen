package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

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
                    List<String> list = new ArrayList<>();

                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();
                    if (meta == null || emojis.isEmpty()) return;

                    meta.setTitle("Glyph Book");
                    meta.setAuthor("Oraxen");
                    meta.setGeneration(ORIGINAL);
                    int s = 0;
                    for (int p = 0; p < 50; p++) {
                        for (int i = 0; i < 256; i++) {
                            s = p * 256 + i + 1;
                            if (emojis.size() < s) break;
                            list.add(String.valueOf(emojis.get(p * 256 + i).getCharacter()));
                        }
                        meta.addPage(ChatColor.WHITE + list.toString().replace("[", "").replace("]", "").replace(",", ""));
                        list.clear();
                        if (emojis.size() < s) break;
                    }
                    book.setItemMeta(meta);
                    player.openBook(book);
                });
    }
}
