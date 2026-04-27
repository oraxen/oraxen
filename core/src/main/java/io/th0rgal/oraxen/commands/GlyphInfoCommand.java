package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class GlyphInfoCommand {

    CommandAPICommand getGlyphInfoCommand() {
        return new CommandAPICommand("glyphinfo")
                .withPermission("oraxen.command.glyphinfo")
                .withArguments(new StringArgument("glyphid").replaceSuggestions(ArgumentSuggestions.strings(OraxenPlugin.get().getFontManager().getGlyphs().stream().map(Glyph::getName).toList())))
                .executes(((sender, args) -> {
                    String glyphId = (String) args.get("glyphid");
                    Glyph glyph = OraxenPlugin.get().getFontManager().getGlyphFromID(glyphId);
                    Audience audience = OraxenPlugin.get().getAudience().sender(sender);
                    if (glyph == null) {
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<prefix><red>No glyph found with glyph-id <white>" + glyphId + "<red>."));
                    } else {
                        audience.sendMessage(
                                Component.empty()
                                        .append(Component.newline())
                                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>GlyphID ⏵ <white>" + glyphId))
                                        .append(Component.newline())
                                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Texture ⏵ <white>" + glyph.getTexture()))
                                        .append(Component.newline())
                                        .append(AdventureUtils.MINI_MESSAGE.deserialize("<gray>Unicode(s) ⏵ <white>"))
                                        .append(Component.newline())
                                        .append(glyph.getGlyphComponent().color(NamedTextColor.WHITE)
                                               .hoverEvent(HoverEvent.showText(AdventureUtils.MINI_MESSAGE.deserialize("<gold>Click to copy to clipboard.")))
                                               .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.getFormattedUnicodes())))
                                        .append(Component.newline())
                        );
                    }
                })
        );
    }
}
