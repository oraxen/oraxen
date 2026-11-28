package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class PacketHelpers {

    public static final String BACKSLASH_REMOVAL_REGEX = "\\\\(?!u)(?!n)(?!\")";

    public static String readJson(String text) {
        return AdventureUtils.parseMiniMessageThroughLegacy(AdventureUtils.GSON_SERIALIZER.deserialize(text));
    }

    public static String toJson(String text) {
        return AdventureUtils.GSON_SERIALIZER.serialize(AdventureUtils.MINI_MESSAGE.deserialize(text));
    }

    public static String translateJson(String json) {
        if (json == null) return null;
        try {
            return AdventureUtils.GSON_SERIALIZER.serialize(translateTitle(AdventureUtils.GSON_SERIALIZER.deserialize(json)));
        } catch (RuntimeException exception) {
            if (Settings.DEBUG.toBool()) {
                Logs.logWarning("PacketHelpers.translateJson failed, returning original payload: " + exception.getMessage());
            }
            return json;
        }
    }

    public static Component translateTitle(Component component) {
        if (component instanceof TextComponent text && text.children().isEmpty()) {
            String content = text.content();
            if (content.indexOf('\\') >= 0 && content.indexOf('&') < 0 && content.indexOf('\u00a7') < 0) {
                if (content.indexOf('<') < 0) return text;
                // Only parse raw MiniMessage when there is no existing style to carry over.
                if (!text.hasStyling()) return AdventureUtils.MINI_MESSAGE.deserialize(content);
            }
        }
        return AdventureUtils.MINI_MESSAGE.deserialize(
            AdventureUtils.removeBackslashes(AdventureUtils.parseMiniMessageThroughLegacy(component))
        );
    }
}
