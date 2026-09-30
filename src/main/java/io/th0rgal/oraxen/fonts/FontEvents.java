package io.th0rgal.oraxen.fonts;

import io.th0rgal.oraxen.glyphs.*;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.SchedulerUtil;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.utils.AdventureUtils.*;

public class FontEvents implements Listener {

    private final FontManager manager;
    private final PaperChatHandler paperChatHandler;
    private final Map<UUID, SignEditSession> signEditSessions = new ConcurrentHashMap<>();

    public FontEvents(FontManager manager) {
        this.manager = manager;
        this.paperChatHandler = new PaperChatHandler();
    }

    public void registerChatHandlers() {
        Bukkit.getPluginManager().registerEvents(paperChatHandler, OraxenPlugin.get());
    }

    public void unregisterChatHandlers() {
        HandlerList.unregisterAll(paperChatHandler);
        signEditSessions.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignOpen(final PlayerOpenSignEvent event) {
        if (!Settings.FORMAT_SIGNS.toBool()) return;

        Player player = event.getPlayer();
        SignEditSession previous = signEditSessions.remove(player.getUniqueId());
        if (previous != null)
            player.sendBlockUpdate(previous.location(), previous.original());

        Location location = event.getSign().getLocation();
        Sign sign = (Sign) event.getSign().copy();
        Side side = event.getSide();
        List<Component> originalLines = List.copyOf(sign.getSide(side).lines());
        List<String> editorLines = new ArrayList<>(originalLines.size());
        boolean hasAnimatedGlyph = false;
        for (Component line : originalLines) {
            String originalText = PLAIN_TEXT.serialize(line);
            String editorText = animatedGlyphPlaceholders(originalText, manager.getAnimatedGlyphs());
            editorLines.add(editorText);
            hasAnimatedGlyph |= !editorText.equals(originalText);
        }
        if (!hasAnimatedGlyph) return;

        Sign preview = (Sign) sign.copy();
        for (int i = 0; i < editorLines.size(); i++)
            preview.getSide(side).line(i, Component.text(editorLines.get(i)));

        signEditSessions.put(player.getUniqueId(),
                new SignEditSession(location, side, sign, originalLines, editorLines));
        player.sendBlockUpdate(location, preview);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSignOpenComplete(final PlayerOpenSignEvent event) {
        if (!event.isCancelled()) return;
        SignEditSession session = signEditSessions.remove(event.getPlayer().getUniqueId());
        if (session != null)
            event.getPlayer().sendBlockUpdate(session.location(), session.original());
    }

    static String animatedGlyphPlaceholders(String text, Collection<AnimatedGlyph> animatedGlyphs) {
        List<AnimatedGlyph> glyphs = animatedGlyphs.stream()
                .filter(glyph -> glyph.getPlaceholders().length > 0)
                .sorted(Comparator.comparingInt((AnimatedGlyph glyph) ->
                        PlainTextComponentSerializer.plainText().serialize(glyph.getGlyphComponent()).length()).reversed())
                .toList();
        for (AnimatedGlyph glyph : glyphs) {
            String characters = PlainTextComponentSerializer.plainText().serialize(glyph.getGlyphComponent());
            if (!characters.isEmpty())
                text = text.replace(characters, glyph.getPlaceholders()[0]);
        }
        return text;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookGlyph(final PlayerEditBookEvent event) {
        if (!Settings.FORMAT_BOOKS.toBool()) return;

        BookMeta meta = event.getNewBookMeta();
        for (Component page : meta.pages()) {
            if (containsUnpermittedGlyph(event.getPlayer(), PLAIN_TEXT.serialize(page)))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookGlyph(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!Settings.FORMAT_BOOKS.toBool()) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || !(event.getItem().getItemMeta() instanceof BookMeta meta)) return;
        if (event.getItem().getType() != Material.WRITTEN_BOOK) return;
        if (event.useInteractedBlock() == Event.Result.ALLOW) return;

        Book book = Book.book(
                meta.title() != null ? meta.title() : Component.empty(),
                meta.author() != null ? meta.author() : Component.empty(),
                meta.pages().stream().map(page -> formatBookPage(page, player)).toList()
        );

        // Open fake book and deny opening of original book to avoid needing to format the original book
        event.setUseItemInHand(Event.Result.DENY);
        AdventureUtils.openBook(player, book);
    }

    /**
     * Formats a displayed book page. Glyph tags ({@code <glyph:...>}) resolve again through the
     * restricted player-aware resolver (they were lost when pages moved to literal-only
     * formatting), while the injection fix stays intact: interaction tags such as
     * {@code <click:run_command>} remain literal text instead of becoming real events.
     */
    private Component formatBookPage(Component page, Player player) {
        String serialized = MINI_MESSAGE_EMPTY.serialize(page).replaceAll("\\\\(?!u)(?!n)(?!\")", "");
        return format(AdventureUtils.safePlayerInputMiniMessage(player).deserialize(serialized), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignGlyph(final SignChangeEvent event) {
        if (!Settings.FORMAT_SIGNS.toBool()) return;

        Player player = event.getPlayer();
        SignEditSession session = matchingSignEditSession(event);
        List<Component> lines = event.lines();
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (session != null && PLAIN_TEXT.serialize(line).equals(session.editorLines().get(i))) {
                event.line(i, session.originalLines().get(i));
                continue;
            }
            if (containsUnpermittedGlyph(player, PLAIN_TEXT.serialize(line)))
                event.setCancelled(true);
            event.line(i, format(line, player));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSignGlyphComplete(final SignChangeEvent event) {
        SignEditSession session = matchingSignEditSession(event);
        if (session == null) return;
        Player player = event.getPlayer();
        signEditSessions.remove(player.getUniqueId());

        Sign restored = (Sign) session.original().copy();
        if (!event.isCancelled())
            for (int i = 0; i < event.lines().size(); i++)
                restored.getSide(session.side()).line(i, event.line(i));
        SchedulerUtil.runForEntityLater(player, 1L,
                () -> player.sendBlockUpdate(session.location(), restored));
    }

    private SignEditSession matchingSignEditSession(SignChangeEvent event) {
        SignEditSession session = signEditSessions.get(event.getPlayer().getUniqueId());
        return session != null && session.side() == event.getSide()
                && session.location().equals(event.getBlock().getLocation()) ? session : null;
    }

    private record SignEditSession(Location location, Side side, Sign original,
                                   List<Component> originalLines, List<String> editorLines) {}

    @EventHandler
    public void onPlayerRename(final InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory clickedInv)) return;
        if (!Settings.FORMAT_ANVIL.toBool() || event.getSlot() != 2) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack inputItem = clickedInv.getItem(0);
        ItemStack resultItem = clickedInv.getItem(2);
        if (resultItem == null || !OraxenItems.exists(inputItem)) return;

        String renameText = VersionUtil.atOrAbove("1.21.1")
                ? ((AnvilView) event.getView()).getRenameText()
                : clickedInv.getRenameText();
        String displayName = processRenameDisplayName(player, renameText, inputItem);

        String finalDisplayName = displayName;
        ItemUtils.editItemMeta(resultItem, meta ->
                ItemUtils.setDisplayName(meta, finalDisplayName == null ? null : MINI_MESSAGE.deserialize(finalDisplayName)));
    }

    private String processRenameDisplayName(Player player, String displayName, ItemStack inputItem) {
        if (displayName != null) {
            // Restrict player-supplied rename text to the safe cosmetic tag set (style,
            // glyph and shift tags); interaction tags such as <click>/<hover> stay literal.
            displayName = AdventureUtils.parseSafePlayerInput(displayName);
            displayName = replaceUnpermittedGlyphs(player, displayName);
            displayName = replaceGlyphPlaceholders(player, displayName);
        }

        // If displayName is unchanged from input or empty, restore original name
        ItemMeta inputMeta = inputItem.getItemMeta();
        String strippedInput = inputMeta != null && ItemUtils.hasDisplayName(inputMeta)
                ? AdventureUtils.PLAIN_TEXT.serialize(ItemUtils.getDisplayName(inputMeta))
                : "";
        if (((displayName == null || displayName.isEmpty()) && OraxenItems.exists(inputItem))
                || strippedInput.equals(displayName)) {
            return inputMeta != null ? inputMeta.getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING) : null;
        }
        return displayName;
    }

    private String replaceUnpermittedGlyphs(Player player, String displayName) {
        Glyph required = manager.getGlyphFromName("required");
        String replacement = required.hasPermission(player) ? required.getCharacters() : "";
        StringBuilder builder = new StringBuilder(displayName);
        Set<Glyph> warnedGlyphs = new HashSet<>();
        for (GlyphMatch match : findGlyphMatches(displayName).reversed()) {
            Glyph glyph = match.glyph();
            if (glyph.hasPermission(player)) continue;

            if (warnedGlyphs.add(glyph))
                Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.getPermission()));
            builder.replace(match.start(), match.end(), replacement);
        }
        return builder.toString();
    }

    private boolean containsUnpermittedGlyph(Player player, String text) {
        boolean containsUnpermittedGlyph = false;
        Set<Glyph> warnedGlyphs = new HashSet<>();
        for (GlyphMatch match : findGlyphMatches(text)) {
            Glyph glyph = match.glyph();
            if (glyph.hasPermission(player)) continue;

            if (warnedGlyphs.add(glyph))
                Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.getPermission()));
            containsUnpermittedGlyph = true;
        }
        return containsUnpermittedGlyph;
    }

    private List<GlyphMatch> findGlyphMatches(String text) {
        List<GlyphMatch> matches = new ArrayList<>();
        boolean[] occupied = new boolean[text.length()];
        for (Glyph glyph : manager.getGlyphsByCharacterLength()) {
            String characters = glyph.getCharacters();
            if (characters.isEmpty()) continue;
            int start = text.indexOf(characters);
            while (start != -1) {
                int end = start + characters.length();
                if (!isOccupied(occupied, start, end)) {
                    matches.add(new GlyphMatch(glyph, start, end));
                    for (int i = start; i < end; i++) occupied[i] = true;
                }
                start = text.indexOf(characters, start + 1);
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(GlyphMatch::start))
                .toList();
    }

    private boolean isOccupied(boolean[] occupied, int start, int end) {
        for (int i = start; i < end; i++)
            if (occupied[i]) return true;
        return false;
    }

    private record GlyphMatch(Glyph glyph, int start, int end) {}

    private String replaceGlyphPlaceholders(Player player, String displayName) {
        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
            if (!entry.getValue().hasPermission(player)) continue;
            String replacement = (manager.permsChatcolor == null)
                    ? entry.getValue().getCharacters()
                    : "<white>" + entry.getValue().getCharacters()
                            + PapiAliases.setPlaceholders(player, manager.permsChatcolor);
            displayName = displayName.replace(entry.getKey(), replacement);
        }
        return displayName;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.sendGlyphTabCompletion(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        signEditSessions.remove(event.getPlayer().getUniqueId());
        manager.clearGlyphTabCompletions(event.getPlayer());
    }

    @SuppressWarnings("UnstableApiUsage")
    public class PaperChatHandler implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatDecorateEvent event) {
            if (!Settings.FORMAT_CHAT.toBool()) return;
            event.result(format(event.result(), event.player()));
        }

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatEvent event) {
            if (!Settings.FORMAT_CHAT.toBool()) return;
            // The removed legacy chat handler blocked messages containing raw glyph unicodes
            // the sender lacks permission for; restore that behavior on the modern path
            // (containsUnpermittedGlyph also sends the NO_PERMISSION message).
            if (containsUnpermittedGlyph(event.getPlayer(), PLAIN_TEXT.serialize(event.originalMessage())))
                event.setCancelled(true);
        }

    }

    private Component format(Component message, Player player) {
        Key randomKey = Key.key("random");
        String serialized = MINI_MESSAGE.serialize(message);
        for (Glyph glyph : manager.getGlyphsByCharacterLength()) {
            String characters = glyph.getCharacters();
            if (!serialized.contains(characters)) continue;
            message = message.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(characters)
                            .replacement(glyph.hasPermission(player)
                                    ? glyph.getGlyphComponent()
                                    : glyph.getGlyphComponent().font(randomKey))
                            .build()
            );
        }

        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet())
            if (entry.getValue().hasPermission(player)) {
                message = message.replaceText(
                        TextReplacementConfig.builder()
                                .matchLiteral(entry.getKey())
                                .replacement(entry.getValue().getGlyphComponent()).build()
                );
            }

        // Process animated glyphs
        for (AnimatedGlyph animGlyph : manager.getAnimatedGlyphs())
            if (animGlyph.hasPermission(player)) {
                for (String placeholder : animGlyph.getPlaceholders()) {
                    message = message.replaceText(
                            TextReplacementConfig.builder()
                                    .matchLiteral(placeholder)
                                    .replacement(animGlyph.getGlyphComponent()).build()
                    );
                }
            }

        return message;
    }

}
