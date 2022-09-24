package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PrintGlyphCommand {
    public CommandAPICommand getPrintGlyphCommand() {
        List<String> glyphnames = new ArrayList<>();
        glyphnames.add("all");
        OraxenPlugin.get().getFontManager().getGlyphs().forEach(glyph -> glyphnames.add(glyph.getName()));
        return new CommandAPICommand("printglyph")
                .withPermission("oraxen.command.printglyph")
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
        Component component = Component.empty();
        Audience audience = OraxenPlugin.get().getAudience().sender(sender);
        if (glyphName.equals("all")) {
            int i = 0;
            for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
                component = component.append(printClickableMsg("<reset>[<green>" + glyph.getName() + "<reset>] ", String.valueOf(glyph.getCharacter()), String.valueOf(glyph.getCharacter())));
                if (i % 3 == 0) {
                    audience.sendMessage(component);
                    component = Component.empty();
                }
                i++;
            }
        } else {
            Glyph g = OraxenPlugin.get().getFontManager().getGlyphs().stream().filter(glyph -> glyph.getName().equals(glyphName)).findAny().orElse(null);
            if (g == null) return;
            component = printClickableMsg("<white>" + g.getName(), String.valueOf(g.getCharacter()), "<reset>" + g.getCharacter());
        }
        audience.sendMessage(component);
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
                utf = new String(Hex.decodeHex(code.toCharArray()), StandardCharsets.UTF_16BE).toCharArray()[0];
            }
            Component component = Component.empty();
            for (int i = 0; i < range; i++) {
                component = component.append(printClickableMsg("<white>[<aqua>U+" + Integer.toHexString(utf).toUpperCase() + "," + ((int) utf) + "(dec)<white>] ",
                        String.valueOf(utf), "<white>" + utf));
                if (i == 2) {
                    OraxenPlugin.get().getAudience().sender(sender).sendMessage(component);
                    component = Component.empty();
                }
                utf++;
            }
            OraxenPlugin.get().getAudience().sender(sender).sendMessage(component);

        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    private void printHelpTitle(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Click one of the glyph-ids below to copy the unicode!");
    }

    private Component printClickableMsg(String text, String unicodeChar, String hoverText) {
        return Component.text(text)
                .append(Component.empty().clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, unicodeChar)))
                .append(Component.empty().hoverEvent(HoverEvent.showText(Component.text(hoverText))));
    }
}
