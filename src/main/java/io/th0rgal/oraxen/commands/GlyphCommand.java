package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.Glyph;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
import static org.bukkit.inventory.meta.BookMeta.Generation.ORIGINAL;

public class GlyphCommand {

    public CommandAPICommand getGlyphCommand(ConfigurationSection commandsSection) {
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
                    List<BaseComponent[]> list = new ArrayList<>();
                    BaseComponent[] page;
                    ComponentBuilder page2 = new ComponentBuilder("");
                    int s;

                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();

                    if (meta == null || emojis.isEmpty()) {
                        Message.NO_EMOJIS.send(player);
                        return;
                    }

                    meta.setTitle("Glyph Book");
                    meta.setAuthor("Oraxen");
                    meta.setGeneration(ORIGINAL);
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

                                if (!onlyShowPermissable)
                                    permissionMessage += emoji.hasPermission(player) ?
                                            ("\n" + ChatColor.GREEN + "Permitted") : ("\n" + ChatColor.RED +"No Permission");
                            }
                            page = new ComponentBuilder(ChatColor.WHITE + String.valueOf(emoji.getCharacter()))
                                    .event(new ClickEvent(SUGGEST_COMMAND, String.valueOf(emoji.getCharacter())))
                                    .event(new HoverEvent(SHOW_TEXT, new ComponentBuilder(finalString + permissionMessage).create()))
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
