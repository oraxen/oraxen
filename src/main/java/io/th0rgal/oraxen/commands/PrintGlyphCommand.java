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
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;

public class PrintGlyphCommand {
    public CommandAPICommand getPrintGlyphCommand() {
        List<String> glyphnames = new ArrayList<>();
        glyphnames.add("all");
        OraxenPlugin.get().getFontManager().getGlyphs().forEach(glyph -> glyphnames.add(glyph.getName()));
        return new CommandAPICommand("printfont")
                .withPermission("oraxen.command.printfont")
                .withArguments(new TextArgument("glyphname").replaceSuggestions(ArgumentSuggestions.strings(glyphnames.toArray(new String[glyphnames.size()]))))
                .executes(((commandSender, args) -> {
                    printHelpTitle(commandSender);
                    if (OraxenPlugin.get().getFontManager().getGlyphFromName(String.valueOf(args[0])) != null
                            ||
                            String.valueOf(args[0]).equals("all")) {

                        printGlyph(commandSender, (String) args[0]);

                    } else printUnicode(commandSender, (String) args[0]);
                }));
    }

    private void printGlyph(CommandSender sender, String glyphName) {
        if (glyphName.equals("all")) {
            int i = 0;
            ComponentBuilder cb = new ComponentBuilder();
            for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
                cb.append(printClickableMsg(ChatColor.RESET+"[" + ChatColor.GREEN + glyph.getName() + ChatColor.RESET+"] ", String.valueOf(glyph.getCharacter()), String.valueOf(glyph.getCharacter())));
                if (i % 3 == 0) {
                    sender.spigot().sendMessage(cb.create());
                    cb = new ComponentBuilder();
                }
                i++;
            }
            if (cb.getParts().size() != 0)
                sender.spigot().sendMessage(cb.create());
            return;
        }
        Glyph g = OraxenPlugin.get().getFontManager().getGlyphs().stream().filter(glyph -> glyph.getName().equals(glyphName)).findAny().orElse(null);
        sender.spigot().sendMessage(printClickableMsg(ChatColor.WHITE + g.getName(), String.valueOf(g.getCharacter()), ChatColor.RESET + String.valueOf(g.getCharacter())));
    }

    /**
     * Parses code input to print a unicode list with decimal version of UTF-16
     * the format is {hex unicode id}+{range to display} like this: "E000+10"
     * @param sender command sender
     * @param code unicode symbol with formatted range
     */
    private void printUnicode(CommandSender sender, String code) {
        try {
            char utf;
            int range = 1;
            if (code.matches("[A-Za-z0-9]{4}\\+[0-9]+")) {
                String[] splitted = code.split("\\+");
                utf = new String(Hex.decodeHex(splitted[0].toCharArray()), StandardCharsets.UTF_16BE).toCharArray()[0];
                range = Integer.parseInt(splitted[1]);
            } else {
                Bukkit.getLogger().info(code);
                utf = new String(Hex.decodeHex(code.toCharArray()), StandardCharsets.UTF_16BE).toCharArray()[0];
            }
            ComponentBuilder componentBuilder = new ComponentBuilder();
            for (int i = 0; i < range; i++) {
                componentBuilder.append(printClickableMsg(ChatColor.WHITE + "[" + ChatColor.AQUA + "U+" + Integer.toHexString(utf).toUpperCase() + "," + ((int) utf) + "(dec)" + ChatColor.WHITE + "] ",
                        String.valueOf(utf),
                        ChatColor.RESET + String.valueOf(utf)));
                if (i == 2) {
                    sender.spigot().sendMessage(componentBuilder.create());
                    componentBuilder = new ComponentBuilder();
                }
                utf++;
            }
            if (componentBuilder.getParts().size() != 0)
                sender.spigot().sendMessage(componentBuilder.create());

        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    private void printHelpTitle(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "The following text is " + ChatColor.BOLD + ChatColor.DARK_RED + "CLICKABLE and HOVERABLE" + ChatColor.RESET + ChatColor.RED + "!");
    }

    private BaseComponent[] printClickableMsg(String text, String unicodeChar, String hoverText) {
        BaseComponent[] components = new ComponentBuilder(text)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, unicodeChar))
                .event(new HoverEvent(SHOW_TEXT, new ComponentBuilder(hoverText).create()))
                .create();
        return components;
    }
}
