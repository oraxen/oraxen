package io.th0rgal.oraxen.nms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlyphHandlers {

    private enum GlyphHandler {
        NMS, VANILLA;

        public static GlyphHandler get() {
            try {
                return GlyphHandler.valueOf(Settings.GLYPH_HANDLER.toString());
            } catch (IllegalArgumentException e) {
                Logs.logError("Invalid glyph handler: " + Settings.GLYPH_HANDLER + ", defaulting to VANILLA", true);
                Logs.logError("Valid options are: NMS, VANILLA", true);
                return GlyphHandler.VANILLA;
            }
        }
    }

    public static boolean isNms() {
        return GlyphHandler.get() == GlyphHandler.NMS;
    }

    public static Component transform(Component component, @Nullable Player player, boolean isUtf) {
        if (player != null) return escapeGlyphs(component, player);
        else return transformGlyphs(component, isUtf);
    }
    private static final Key randomKey = Key.key("random");

    private static Component escapeGlyphs(Component component, @NotNull Player player) {
        component = GlobalTranslator.render(component, player.locale());
        String serialized = AdventureUtils.MINI_MESSAGE.serialize(component);

        // Replace raw unicode usage of non-permissed Glyphs with random font
        // This will always show a white square
        for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            if (glyph.hasPermission(player)) continue;

            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(glyph.getCharacter())
                            .replacement(glyph.getGlyphComponent().font(randomKey))
                            .build()
            );

            // Escape all glyph-tags
            Matcher matcher = glyph.baseRegex.matcher(serialized);
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder().once()
                                .matchLiteral(matcher.group())
                                .replacement(AdventureUtils.MINI_MESSAGE.deserialize("\\" + matcher.group()))
                                .build()
                );
            }
        }

        return component;
    }

    private static final Pattern colorableRegex = Pattern.compile("<glyph:.*:(c|colorable)>");

    private static Component transformGlyphs(Component component, boolean isUtf) {
        String serialized = AdventureUtils.MINI_MESSAGE_EMPTY.serialize(component);

        for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            Matcher matcher = glyph.baseRegex.matcher(serialized);
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder().once()
                                .matchLiteral(matcher.group())
                                .replacement(glyph.getGlyphComponent())
                                .build());
            }

            if (isUtf) {
                matcher = glyph.escapedRegex.matcher(serialized);
                while (matcher.find()) {
                    component = component.replaceText(
                            TextReplacementConfig.builder().once()
                                    .matchLiteral(matcher.group())
                                    .replacement(AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(StringUtils.removeStart(matcher.group(), "\\")))
                                    .build()
                    );
                }
            }
        }

        return component;
    }

    public static String formatJsonString(@NotNull JsonObject obj, @Nullable Player player) {
        if ((obj.has("args") || obj.has("text") || obj.has("extra") || obj.has("translate"))) {
            Component component = AdventureUtils.GSON_SERIALIZER.deserialize(obj.toString());
            component = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(AdventureUtils.MINI_MESSAGE_EMPTY.serialize(component));
            component = transform(component, player, false);
            return AdventureUtils.GSON_SERIALIZER.serialize(component);
        } else return obj.toString();
    }

    public static Function<String, String> transformer(@Nullable Player player) {
        return string -> {
            try {
                JsonElement element = JsonParser.parseString(string);
                if (element.isJsonObject())
                    return GlyphHandlers.formatJsonString(element.getAsJsonObject(), player);
            } catch (Exception ignored) {
            }
            return string;
        };
    }
}
