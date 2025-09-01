package io.th0rgal.oraxen.utils;

import net.kyori.adventure.text.Component;

public class PacketHelpers {

    public static final String BACKSLASH_REMOVAL_REGEX = "\\\\(?!u)(?!n)(?!\")";

    // Serialize initial string from json to component, then parse to handle tags and serialize again to json string
    public static String readJson(String text) {
        return AdventureUtils.parseMiniMessageThroughLegacy(AdventureUtils.GSON_SERIALIZER.deserialize(text));
    }

    public static String toJson(String text) {
        return AdventureUtils.GSON_SERIALIZER.serialize(AdventureUtils.MINI_MESSAGE.deserialize(text)).replaceAll(BACKSLASH_REMOVAL_REGEX, "");
    }

    public static Component translateTitle(Component component) {
        return AdventureUtils.MINI_MESSAGE.deserialize(
            AdventureUtils.parseMiniMessageThroughLegacy(component)
                .replaceAll("\\\\(?!u)(?!n)(?!\")", "")
        );
    }
}
