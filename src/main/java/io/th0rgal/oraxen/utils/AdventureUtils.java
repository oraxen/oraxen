package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.font.GlyphTag;
import io.th0rgal.oraxen.font.ShiftTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class AdventureUtils {

    private AdventureUtils() {
    }

    public static final TagResolver OraxenTagResolver =
            TagResolver.resolver(TagResolver.standard(), GlyphTag.RESOLVER, GlyphTag.RESOLVER_SHORT, ShiftTag.RESOLVER, ShiftTag.RESOLVER_SHORT);

    public static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public static final LegacyComponentSerializer LEGACY_AMPERSAND =
            LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(OraxenTagResolver).build();

    public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    public static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    /**
     * @param message The string to parse
     * @return The original string, serialized and deserialized through MiniMessage
     */
    public static String parseMiniMessage(String message) {
        return MINI_MESSAGE.serialize(MINI_MESSAGE.deserialize(message));
    }

    public static String parseMiniMessage(String message, TagResolver tagResolver) {
        return MINI_MESSAGE.serialize(MINI_MESSAGE.deserialize(message, tagResolver));
    }

    /**
     * @param message The component to parse
     * @return The original component, serialized and deserialized through MiniMessage
     */
    public static Component parseMiniMessage(Component message) {
        return MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(message));
    }

    public static Component parseMiniMessage(Component message, TagResolver tagResolver) {
        return MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(message), tagResolver);
    }

    /**
     * Parses the string by deserializing it to a legacy component, then serializing it to a string via MiniMessage
     * @param message The string to parse
     * @return The parsed string
     */
    public static String parseLegacy(String message) {
        return MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!\")", "");
    }

    public static Component parseLegacy(Component message) {
        return MINI_MESSAGE.deserialize(LEGACY_SERIALIZER.serialize(message));
    }

    public static String parseLegacyToString(Component message) {
        return MINI_MESSAGE.serialize(parseLegacy(message));
    }

    /**
     * Parses a string through both legacy and minimessage serializers.
     * This is useful for parsing strings that may contain legacy formatting codes and modern adventure-tags.
     * @param message The component to parse
     * @return The parsed string
     */
    public static String parseLegacyThroughMiniMessage(String message) {
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!\")", "")));
    }

    public static String parseLegacyThroughMiniMessage(Component message) {
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(LEGACY_SERIALIZER.serialize(message).replaceAll("\\\\(?!u)(?!\")", "")));
    }

    public static String parseMiniMessageThroughLegacy(Component message) {
        return MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(MINI_MESSAGE.serialize(message).replace('&', '§'))).replaceAll("\\\\(?!u)(?!\")", "");
    }

    /**
     * @param message The string to parse
     * @return The original string, parsed with GsonComponentSerializer
     */
    public static String parseJson(String message) {
        return GSON_SERIALIZER.serialize(GSON_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!\")", "");
    }

    /**
     * @param message The component to parse
     * @return The original component, parsed with GsonSerializer
     */
    public static Component parseJson(Component message) {
        return GSON_SERIALIZER.deserialize(GSON_SERIALIZER.serialize(message).replaceAll("\\\\(?!u)(?!\")", ""));
    }

    /**
     * @param message The string to parse
     * @return The original string, parsed with PlainTextComponentSerializer
     */
    public static String parsePlainText(String message) {
        return PLAIN_TEXT.serialize(PLAIN_TEXT.deserialize(message));
    }

    /**
     * @param message The component to parse
     * @return The original component, parsed with PlainTextComponentSerializer
     */
    public static Component parsePlainText(Component message) {
        return PLAIN_TEXT.deserialize(PLAIN_TEXT.serialize(message));
    }


    public static TagResolver tagResolver(String tag, String string) {
        return TagResolver.resolver(tag, Tag.selfClosingInserting(AdventureUtils.MINI_MESSAGE.deserialize(string)));
    }
}
