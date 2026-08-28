package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.glyphs.GlyphTag;
import io.th0rgal.oraxen.glyphs.ShiftTag;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AdventureUtils {

    private AdventureUtils() {
    }

    public static final MiniMessage MINI_MESSAGE_EMPTY = MiniMessage.miniMessage();

    public static final TagResolver OraxenTagResolver = TagResolver.resolver(TagResolver.standard(),
            GlyphTag.RESOLVER, ShiftTag.RESOLVER,
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

    /**
     * Cosmetic style tags that are safe to resolve in player-supplied text. Interaction and
     * content tags ({@code <click>}, {@code <hover>}, {@code <insertion>}, {@code <translate>},
     * {@code <selector>}, ...) are deliberately excluded and stay literal.
     */
    private static final TagResolver SAFE_STYLE_TAGS = TagResolver.resolver(
            StandardTags.color(), StandardTags.decorations(), StandardTags.gradient(),
            StandardTags.rainbow(), StandardTags.transition(), StandardTags.font(),
            StandardTags.reset());

    /**
     * MiniMessage instance for player-supplied text (e.g. anvil renames): resolves only the
     * safe cosmetic style tags plus Oraxen's glyph/shift tags.
     */
    public static final MiniMessage SAFE_PLAYER_INPUT_MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(SAFE_STYLE_TAGS, GlyphTag.RESOLVER, ShiftTag.RESOLVER))
            .build();

    /**
     * Player-aware variant of {@link #SAFE_PLAYER_INPUT_MINI_MESSAGE}: glyph tags resolve with
     * the player's permissions (unpermitted glyphs stay literal tag text).
     */
    public static MiniMessage safePlayerInputMiniMessage(Player player) {
        return MiniMessage.builder()
                .tags(TagResolver.resolver(SAFE_STYLE_TAGS, GlyphTag.getResolverForPlayer(player), ShiftTag.RESOLVER))
                .build();
    }

    /**
     * Parses player-supplied text through the legacy serializer (converting legacy codes) and
     * the restricted {@link #SAFE_PLAYER_INPUT_MINI_MESSAGE} tag set. Unsafe tags stay literal.
     * The result is a MiniMessage string rather than a legacy string, so styled output survives
     * a later MiniMessage deserialization instead of degrading into literal section-sign codes.
     */
    public static String parseSafePlayerInput(String message) {
        String miniMessage = MINI_MESSAGE.serialize(LEGACY_SERIALIZER.deserialize(message))
                .replaceAll("\\\\(?!u)(?!n)(?!\")", "");
        return MINI_MESSAGE.serialize(SAFE_PLAYER_INPUT_MINI_MESSAGE.deserialize(miniMessage));
    }

    public static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    public static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    public static void sendMessage(CommandSender sender, Component component) {
        if (sender == null || component == null) return;
        sender.sendMessage(component);
    }

    public static void sendActionBar(Player player, Component component) {
        if (player == null || component == null) return;
        player.sendActionBar(component);
    }

    public static void showTitle(Player player, Title title) {
        if (player == null || title == null) return;
        player.showTitle(title);
    }

    public static void openBook(Player player, Book book) {
        if (player == null || book == null) return;
        player.openBook(book);
    }

    public static void playSound(Player player, Sound sound) {
        if (player == null || sound == null) return;
        player.playSound(sound);
    }

    public static void playSound(Player player, @Nullable String sound, Sound.Source source, float volume, float pitch) {
        playSound(player, sound(sound, source, volume, pitch));
    }

    public static void playSound(Player player, Location location, Sound sound) {
        if (player == null || sound == null) return;
        if (location == null) {
            playSound(player, sound);
            return;
        }

        player.playSound(sound, location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(Player player, Location location, @Nullable String sound, Sound.Source source, float volume, float pitch) {
        playSound(player, location, sound(sound, source, volume, pitch));
    }

    public static void playSound(Location location, Sound sound) {
        if (location == null || sound == null || location.getWorld() == null) return;

        location.getWorld().playSound(sound, location.getX(), location.getY(), location.getZ());
    }

    public static void playSound(Location location, @Nullable String sound, Sound.Source source, float volume, float pitch) {
        playSound(location, sound(sound, source, volume, pitch));
    }

    public static void stopSound(Player player, Sound sound) {
        if (player == null || sound == null) return;
        player.stopSound(sound);
    }

    public static void stopSound(Player player, SoundStop soundStop) {
        if (player == null || soundStop == null) return;

        player.stopSound(soundStop);
    }

    /**
     * Builds an Adventure {@link Sound} from a config-provided sound id.
     *
     * @return the sound, or null if the id is null, blank or not a valid key
     */
    @Nullable
    public static Sound sound(@Nullable String sound, Sound.Source source, float volume, float pitch) {
        if (sound == null || sound.isBlank()) return null;
        try {
            return Sound.sound(Key.key(sound.trim().toLowerCase(Locale.ROOT)), source, volume, pitch);
        } catch (InvalidKeyException e) {
            Logs.logWarning("Invalid sound key: " + sound);
            return null;
        }
    }

    public static Sound.Source toSource(@Nullable SoundCategory category) {
        if (category == null) return Sound.Source.MASTER;
        return switch (category) {
            case MUSIC -> Sound.Source.MUSIC;
            case RECORDS -> Sound.Source.RECORD;
            case WEATHER -> Sound.Source.WEATHER;
            case BLOCKS -> Sound.Source.BLOCK;
            case HOSTILE -> Sound.Source.HOSTILE;
            case NEUTRAL -> Sound.Source.NEUTRAL;
            case PLAYERS -> Sound.Source.PLAYER;
            case AMBIENT -> Sound.Source.AMBIENT;
            case VOICE -> Sound.Source.VOICE;
            default -> Sound.Source.MASTER;
        };
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
        if (player != null) component = GlobalTranslator.render(component, player.locale());
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
