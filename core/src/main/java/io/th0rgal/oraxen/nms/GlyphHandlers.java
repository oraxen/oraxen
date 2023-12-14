package io.th0rgal.oraxen.nms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;

public class GlyphHandlers implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDecorate(AsyncChatDecorateEvent event) {
        event.result(GlyphHandlers.transform(event.result(), event.player(), true));
    }

    public static Component transform(Component component, @Nullable Player player, boolean isUtf) {
        if (player != null) return escapeGlyphTags(component, player);
        else return transformGlyphTags(component, isUtf);
    }

    private static Component escapeGlyphTags(Component component, @NotNull Player player) {
        component = GlobalTranslator.render(component, player.locale());

        // Replace raw unicode usage of non-permissed Glyphs with random font
        // This will always show a white square
        Key randomKey = Key.key("random");
        for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            if (glyph.hasPermission(player)) continue;

            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(glyph.getCharacter())
                            .replacement(Component.text(glyph.getCharacter()).font(randomKey))
                            .build()
            );

            // Escape all glyph-tags
            Matcher matcher = glyph.glyphBaseRegex.matcher(AdventureUtils.MINI_MESSAGE_EMPTY.serialize(component));
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder()
                                .match(glyph.glyphBaseRegex)
                                .replacement(AdventureUtils.MINI_MESSAGE_EMPTY.deserialize("\\" + matcher.group()))
                                .build()
                );
            }
        }

        return component;
    }

    private static Component transformGlyphTags(Component component, boolean isUtf) {

        return component;
    }

    public static String formatJsonString(@NotNull JsonObject obj) {
        return (obj.has("args") || obj.has("text") || obj.has("extra") || obj.has("translate")) ?
                Glyph.parsePlaceholders(obj).toString() : obj.toString();
    }

    public static Function<String, String> transformer() {
        return string -> {
            try {
                JsonElement element = JsonParser.parseString(string);
                if (element.isJsonObject())
                    return GlyphHandlers.formatJsonString(element.getAsJsonObject());
            } catch (Exception ignored) {
            }
            return string;
        };
    }

    public static String verifyFor(Player player, String message) {
        if (message != null && player != null) for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            String glyphTag = glyph.getGlyphTag();
            // Escape all glyphs the player does not have permission for
            if (!glyph.hasPermission(player)) message = message.replace(glyphTag, "g" + glyphTag);
        }
        return message;
    }
}
