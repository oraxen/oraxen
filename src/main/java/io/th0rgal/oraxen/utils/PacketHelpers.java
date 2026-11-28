package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;

import java.util.regex.Pattern;

public class PacketHelpers {

    public static final String BACKSLASH_REMOVAL_REGEX = "\\\\(?!u)(?!n)(?!\")";
    private static final Pattern LEGACY_TEXT = Pattern.compile("(?is)\\A.*[&\u00a7][0-9a-fk-orx].*\\z");
    private static final Pattern LEGACY_AMPERSAND_CODE = Pattern.compile("(?i)&(?=[0-9a-fk-orx])");
    private static final Pattern MINI_MESSAGE_TEXT = Pattern.compile("(?s)\\A.*<.*\\z");

    public static String readJson(String text) {
        return AdventureUtils.MINI_MESSAGE.serialize(translateTitle(AdventureUtils.GSON_SERIALIZER.deserialize(text)));
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
        component = component.replaceText(config -> config.match(LEGACY_TEXT).replacement((match, builder) -> {
            String text = LEGACY_AMPERSAND_CODE.matcher(match.group()).replaceAll("\u00a7");
            return Component.empty().append(AdventureUtils.LEGACY_SERIALIZER.deserialize(text));
        }));
        // Keep parsed styles on the new child so they do not leak to existing children.
        return component.replaceText(config -> config.match(MINI_MESSAGE_TEXT).replacement((match, builder) ->
                Component.empty().append(AdventureUtils.MINI_MESSAGE.deserialize(match.group()))));
    }
}
