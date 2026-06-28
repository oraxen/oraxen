package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.glyphs.GlyphTag;
import io.th0rgal.oraxen.glyphs.ShiftTag;
import io.th0rgal.oraxen.glyphs.TextEffectTag;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AdventureUtils {

    private AdventureUtils() {
    }

    public static final MiniMessage MINI_MESSAGE_EMPTY = MiniMessage.miniMessage();

    public static final TagResolver OraxenTagResolver = TagResolver.resolver(TagResolver.standard(),
            GlyphTag.RESOLVER, ShiftTag.RESOLVER, TextEffectTag.RESOLVER,
            TagResolver.resolver("prefix", Tag.selfClosingInserting(MINI_MESSAGE_EMPTY.deserialize(Message.PREFIX.toString())))
    );

    public static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public static final LegacyComponentSerializer LEGACY_AMPERSAND =
            LegacyComponentSerializer.builder().character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(OraxenTagResolver).build();


    public static MiniMessage MINI_MESSAGE_PLAYER(Player player) {
        return MiniMessage.builder().tags(TagResolver.resolver(TagResolver.standard(), GlyphTag.getResolverForPlayer(player))).build();
    }

    public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    public static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static void sendMessage(CommandSender sender, Component component) {
        if (sender == null || component == null) return;

        // adventure-platform-bukkit currently does not deliver messages on Paper 26.2.
        // Use Bukkit's legacy sender path there so Oraxen logs and command feedback remain visible.
        if (VersionUtil.atOrAbove("26.2")) {
            sender.sendMessage(LEGACY_SERIALIZER.serialize(component));
            return;
        }

        try {
            OraxenPlugin.get().getAudience().sender(sender).sendMessage(component);
        } catch (Throwable ignored) {
            sender.sendMessage(LEGACY_SERIALIZER.serialize(component));
        }
    }

    public static void sendActionBar(Player player, Component component) {
        if (player == null || component == null) return;

        if (VersionUtil.atOrAbove("26.2")) {
            player.sendActionBar(component);
            return;
        }

        try {
            OraxenPlugin.get().getAudience().player(player).sendActionBar(component);
        } catch (Throwable ignored) {
            player.sendActionBar(component);
        }
    }

    public static void showTitle(Player player, Title title) {
        if (player == null || title == null) return;

        if (VersionUtil.atOrAbove("26.2")) {
            player.showTitle(title);
            return;
        }

        try {
            OraxenPlugin.get().getAudience().player(player).showTitle(title);
        } catch (Throwable ignored) {
            player.showTitle(title);
        }
    }

    public static void openBook(Player player, Book book) {
        if (player == null || book == null) return;

        if (VersionUtil.atOrAbove("26.2")) {
            player.openBook(book);
            return;
        }

        try {
            OraxenPlugin.get().getAudience().player(player).openBook(book);
        } catch (Throwable ignored) {
            player.openBook(book);
        }
    }

    public static void playSound(Player player, Sound sound) {
        if (player == null || sound == null) return;

        if (VersionUtil.atOrAbove("26.2")) {
            player.playSound(sound);
            return;
        }

        try {
            OraxenPlugin.get().getAudience().player(player).playSound(sound);
        } catch (Throwable ignored) {
            player.playSound(sound);
        }
    }

    public static void stopSound(Player player, Sound sound) {
        if (player == null || sound == null) return;

        if (VersionUtil.atOrAbove("26.2")) {
            player.stopSound(sound);
            return;
        }

        try {
            OraxenPlugin.get().getAudience().player(player).stopSound(sound);
        } catch (Throwable ignored) {
            player.stopSound(sound);
        }
    }

    /**
     * @param message The string to parse
     * @return The original string, serialized and deserialized through MiniMessage
     */
    public static String parseMiniMessage(String message) {
        return MINI_MESSAGE.serialize(MINI_MESSAGE.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    public static String parseMiniMessage(String message, @Nullable TagResolver tagResolver) {
        return MINI_MESSAGE.serialize((tagResolver != null ? MINI_MESSAGE.deserialize(message, tagResolver) : MINI_MESSAGE.deserialize(message))).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    public static String parseMiniMessage(String message, Player player) {
        return MINI_MESSAGE_EMPTY.serialize(MINI_MESSAGE_PLAYER(player).deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    /**
     * @param message The component to parse
     * @return The original component, serialized and deserialized through MiniMessage
     */
    public static Component parseMiniMessage(Component message) {
        return MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(message).replaceAll("\\\\(?!u)(?!n)(?!\")", ""));
    }

    public static Component parseMiniMessage(Component message, TagResolver tagResolver) {
        return MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(message).replaceAll("\\\\(?!u)(?!n)(?!\")", ""), tagResolver);
    }

    public static Component parseMiniMessage(Component message, Player player) {
        return MINI_MESSAGE_PLAYER(player).deserialize(MINI_MESSAGE_EMPTY.serialize(message).replaceAll("\\\\(?!u)(?!n)(?!\")", ""));
    }

    /**
     * Parses the string by deserializing it to a legacy component, then serializing it to a string via MiniMessage
     * @param message The string to parse
     * @return The parsed string
     */
    public static String parseLegacy(String message) {
        return MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
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
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "")));
    }

    public static String parseLegacyThroughMiniMessage(String message, Player player) {
        MiniMessage mm = player != null ? MINI_MESSAGE_PLAYER(player) : MINI_MESSAGE;
        return LEGACY_SERIALIZER.serialize(mm.deserialize(mm.serialize(LEGACY_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "")));
    }

    public static String parseLegacyThroughMiniMessage(Component message) {
        return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(LEGACY_SERIALIZER.serialize(message).replaceAll("\\\\(?!u)(?!n)(?!\")", "")));
    }

    public static String parseMiniMessageThroughLegacy(Component message) {
        return MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(MINI_MESSAGE.serialize(message).replace("&", "§"))).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    /**
     * @param message The string to parse
     * @return The original string, parsed with GsonComponentSerializer
     */
    public static String parseJson(String message) {
        return GSON_SERIALIZER.serialize(GSON_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
    }

    /**
     * @param message The component to parse
     * @return The original component, parsed with GsonSerializer
     */
    public static Component parseJson(Component message) {
        return GSON_SERIALIZER.deserialize(GSON_SERIALIZER.serialize(message).replaceAll("\\\\(?!u)(?!n)(?!\")", ""));
    }

    public static String parseJsonThroughMiniMessage(String message) {
        return GSON_SERIALIZER.serialize(MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(GSON_SERIALIZER.deserialize(message)).replaceAll("\\\\(?!u)(?!n)(?!\")(?!:)", ""))).replaceAll("\\\\(?!u)(?!n)(?!\")(?!:)", "");
    }

    public static String parseJsonThroughMiniMessage(String message, Player player) {
        TagResolver resolver = TagResolver.resolver(GlyphTag.getResolverForPlayer(player), ShiftTag.RESOLVER);
        Component component = GSON_SERIALIZER.deserialize(message.replaceAll("\\\\(?!u)(?!n)(?!\")", ""));
        component = MINI_MESSAGE.deserialize(MINI_MESSAGE.serialize(component).replaceAll("\\\\(?!u)(?!n)(?!\")", ""), resolver);
        if (player != null) component = GlobalTranslator.render(component, Locale.forLanguageTag(player.getLocale()));
        return GSON_SERIALIZER.serialize(component).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
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


    public static TagResolver tagResolver(String string, String tag) {
        return TagResolver.resolver(string, Tag.selfClosingInserting(AdventureUtils.MINI_MESSAGE.deserialize(tag)));
    }

    public static TagResolver tagResolver(String string, Component tag) {
        return TagResolver.resolver(string, Tag.selfClosingInserting(tag));
    }
}
