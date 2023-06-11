package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class PrintGlyphCommand {
    public CommandAPICommand getPrintGlyphCommand() {
        final List<String> glyphnames = new ArrayList<>();
        glyphnames.add("all");
        glyphnames.addAll(OraxenPlugin.get().getFontManager().getGlyphs().stream().map(Glyph::getName).toList());
        return new CommandAPICommand("printglyph")
                .withPermission("oraxen.command.printglyph")
                .withArguments(new TextArgument("glyphname").replaceSuggestions(ArgumentSuggestions.strings(glyphnames.toArray(new String[0]))))
                .executes(((commandSender, args) -> {
                    FontManager fontManager = OraxenPlugin.get().getFontManager();
                    Audience audience = OraxenPlugin.get().getAudience().sender(commandSender);
                    audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red><b>Click one of the glyph-ids below to copy the unicode!"));
                    String glyphname = (String) args.get("glyphname");
                    if (fontManager.getGlyphFromName(glyphname) != null || glyphname.equals("all")) {
                        printGlyph(fontManager, audience, glyphname);
                    } else printUnicode(audience, glyphname);
                }));
    }

    private void printGlyph(FontManager fontManager, Audience audience, String glyphName) {
        Component component = Component.text("");
        if (glyphName.equals("all")) {
            int i = 0;
            for (Glyph glyph : fontManager.getGlyphs()) {
                component = component.append(printClickableMsg("<reset>[<green>" + glyph.getName() + "<reset>] ", String.valueOf(glyph.getCharacter()), String.valueOf(glyph.getCharacter())));
                if (i % 3 == 0) {
                    audience.sendMessage(component);
                    component = Component.empty();
                }
                i++;
            }
        } else if (glyphName.contains("shift_")) {
            try { // Real ugly but its mainly formatting output msg
                int shift = Integer.parseInt(glyphName.split("shift_")[1]);
                if (glyphName.startsWith("neg_shift") && shift > 0) shift = -shift;
                String s = fontManager.getShift(shift);
                String name = shift < 0 ? "neg_shift_" + -shift : "shift_" + shift;
                component = printClickableMsg("<white>" + name, s, s);
            } catch (NumberFormatException e) {
                audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_red><b>Invalid shift number!"));
            }
        } else {
            Glyph g = fontManager.getGlyphs().stream().filter(glyph -> glyph.getName().equals(glyphName)).findFirst().orElse(null);
            if (g == null) return;
            component = printClickableMsg("<white>" + g.getName(), String.valueOf(g.getCharacter()), "<reset>" + g.getCharacter());
        }
        audience.sendMessage(component);
    }

    /**
     * Parses code input to print a unicode list with decimal version of UTF-16
     * the format is {hex unicode id}+{range to display} like this: "E000+10"
     * @param audience audience to send the message to
     * @param code unicode symbol with formatted range
     */
    private void printUnicode(Audience audience, String code) {
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
            Component component = Component.text("");
            for (int i = 0; i < range; i++) {
                component = component.append(printClickableMsg("<white>[<aqua>U+" + Integer.toHexString(utf).toUpperCase() + "," + ((int) utf) + "(dec)<white>] ",
                        String.valueOf(utf), "<white>" + utf));
                if (i == 2) {
                    audience.sendMessage(component);
                    component = Component.empty();
                }
                utf++;
            }
            audience.sendMessage(component);

        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    private Component printClickableMsg(String text, String unicode, String hoverText) {
        return AdventureUtils.MINI_MESSAGE.deserialize(text)
                .hoverEvent(HoverEvent.showText(AdventureUtils.MINI_MESSAGE.deserialize(hoverText)))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, unicode));

    }
}
