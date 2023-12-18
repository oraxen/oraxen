package io.th0rgal.oraxen.nms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlyphHandlers {

    public static Component transform(Component component, @Nullable Player player, boolean isUtf) {
        if (player != null) return escapeGlyphTags(component, player);
        else return transformGlyphTags(component, isUtf);
    }

    private static Component escapeGlyphTags(Component component, @NotNull Player player) {
        component = GlobalTranslator.render(component, player.locale());
        String serialized = AdventureUtils.MINI_MESSAGE_EMPTY.serialize(component);

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
            Matcher matcher = glyph.baseRegex.matcher(serialized);
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder()
                                .match(glyph.baseRegex)
                                .replacement(AdventureUtils.MINI_MESSAGE_EMPTY.deserialize("\\" + matcher.group()))
                                .build()
                );
            }
        }

        return component;
    }

    private static final Pattern colorableRegex = Pattern.compile("<glyph:.*:(c|colorable)>");

    private static Component transformGlyphTags(Component component, boolean isUtf) {
        String serialized = AdventureUtils.MINI_MESSAGE_EMPTY.serialize(component);
        for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            Matcher matcher = glyph.baseRegex.matcher(serialized);
            Component glyphComponent = Component.text(glyph.getCharacter(), NamedTextColor.WHITE).font(Key.key("default")).style(Style.empty());
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder()
                                .match(matcher.pattern())
                                .replacement(glyphComponent)
                                .build());
            }

            if (isUtf) {
                matcher = glyph.escapedRegex.matcher(serialized);
                while (matcher.find()) {
                    component = component.replaceText(
                            TextReplacementConfig.builder()
                                    .match(matcher.pattern())
                                    .replacement(AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(StringUtils.removeEnd(matcher.group(), "\\")))
                                    .build()
                    );
                }
            }
        }

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
