package io.th0rgal.oraxen.nms;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.font.Shift;
import io.th0rgal.oraxen.font.ShiftTag;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlyphHandlers {

    @NotNull public static final NamespacedKey GLYPH_HANDLER_KEY = new NamespacedKey(OraxenPlugin.get(), "glyph_handler");

    public enum GlyphHandler {
        NMS, VANILLA
    }

    public static boolean isNms() {
        return Settings.GLYPH_HANDLER.toEnumOrGet(GlyphHandler.class, (handler) -> {
            Logs.logError("Invalid glyph handler: " + handler + ", defaulting to VANILLA", true);
            Logs.logError("Valid options are: NMS, VANILLA", true);
            return GlyphHandler.VANILLA;
        }) == GlyphHandler.NMS;
    }

    private static final Key randomKey = Key.key("random");
    private static final Pattern colorableRegex = Pattern.compile("<glyph:.*:(c|colorable)>");

    private static Component escapeGlyphs(Component component, @NotNull Player player) {
        component = GlobalTranslator.render(component, player.locale());
        String serialized = AdventureUtils.MINI_MESSAGE.serialize(component);

        // Replace raw unicode usage of non-permitted Glyphs with random font
        // This will always show a white square
        for (Glyph glyph : OraxenPlugin.get().fontManager().glyphs()) {
            if (glyph.hasPermission(player)) continue;

            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(glyph.character())
                            .replacement(glyph.glyphComponent().font(randomKey))
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

    public static Component unescapeGlyphs(@NotNull Component component) {
        String serialized = AdventureUtils.MINI_MESSAGE.serialize(component);

        for (Glyph glyph : OraxenPlugin.get().fontManager().glyphs()) {
            Matcher matcher = glyph.escapedRegex.matcher(serialized);
            while (matcher.find()) {
                component = component.replaceText(
                        TextReplacementConfig.builder().once()
                                .matchLiteral(matcher.group())
                                .replacement(AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(StringUtils.removeStart(matcher.group(), "\\")))
                                .build()
                );
            }
        }

        return component;
    }

    public static Component transformGlyphs(Component component) {
        for (Glyph glyph : OraxenPlugin.get().fontManager().glyphs()) {
            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .match(glyph.baseRegex.pattern())
                            .replacement(glyph.glyphComponent())
                            .build());
        }
        component = component.replaceText(ShiftTag.REPLACEMENT_CONFIG);
        return component;
    }
}
