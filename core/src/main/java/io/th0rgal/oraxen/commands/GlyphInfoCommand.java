package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class GlyphInfoCommand {

    CommandAPICommand getGlyphInfoCommand() {
        return new CommandAPICommand("glyphinfo")
                .withPermission("oraxen.command.glyphinfo")
                .withArguments(new StringArgument("glyphid").replaceSuggestions(ArgumentSuggestions.strings(OraxenPlugin.get().fontManager().glyphs().stream().map(Glyph::id).toList())))
                .executes(((sender, args) -> {
                    String glyphId = (String) args.get("glyphid");
                    Glyph glyph = OraxenPlugin.get().fontManager().getGlyphFromID(glyphId);
                    Audience audience = OraxenPlugin.get().audience().sender(sender);
                    if (glyph == null) {
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<red>No glyph found with glyph-id <i><dark_red>" + glyphId));
                    } else {
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>GlyphID: <aqua>" + glyphId));
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Texture: <aqua>" + glyph.texture()));
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Bitmap: <aqua>" + glyph.isBitMap()));
                        audience.sendMessage(AdventureUtils.MINI_MESSAGE.deserialize("<dark_aqua>Unicode: <white>" + glyph.character()).hoverEvent(HoverEvent.showText(AdventureUtils.MINI_MESSAGE.deserialize("<gold>Click to copy to clipboard!"))).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, glyph.character())));
                    }
                })
        );
    }
}
