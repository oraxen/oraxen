package io.th0rgal.oraxen.font;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.utils.AdventureUtils.*;

public class FontEvents implements Listener {

    private final FontManager manager;
    @Nullable PaperChatHandler paperChatHandler;
    @Nullable LegacyPaperChatHandler legacyPaperChatHandler;
    @Nullable SpigotChatHandler spigotChatHandler;

    enum ChatHandler {
        LEGACY,
        MODERN;

        public static boolean isLegacy() {
            return get() == LEGACY;
        }

        public static boolean isModern() {
            return get() == MODERN;
        }

        public static ChatHandler get() {
            try {
                return valueOf(Settings.CHAT_HANDLER.toString());
            } catch (IllegalArgumentException e) {
                ChatHandler chatHandler = VersionUtil.isPaperServer() ? MODERN : LEGACY;
                Logs.logError("Invalid chat-handler defined in settings.yml, defaulting to " + chatHandler, true);
                Logs.logError("Valid options are: " + Arrays.toString(values()), true);
                return chatHandler;
            }
        }
    }

    public FontEvents(FontManager manager) {
        this.manager = manager;
        if (VersionUtil.isPaperServer()) {
            if (VersionUtil.atOrAbove("1.19.1"))
                paperChatHandler = new PaperChatHandler();
            legacyPaperChatHandler = new LegacyPaperChatHandler();
        }
        spigotChatHandler = new SpigotChatHandler();
    }

    public void registerChatHandlers() {
        if (paperChatHandler != null)
            Bukkit.getPluginManager().registerEvents(paperChatHandler, OraxenPlugin.get());
        if (legacyPaperChatHandler != null)
            Bukkit.getPluginManager().registerEvents(legacyPaperChatHandler, OraxenPlugin.get());
        if (spigotChatHandler != null)
            Bukkit.getPluginManager().registerEvents(spigotChatHandler, OraxenPlugin.get());
    }

    public void unregisterChatHandlers() {
        if (paperChatHandler != null)
            HandlerList.unregisterAll(paperChatHandler);
        if (legacyPaperChatHandler != null)
            HandlerList.unregisterAll(legacyPaperChatHandler);
        if (spigotChatHandler != null)
            HandlerList.unregisterAll(spigotChatHandler);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookGlyph(final PlayerEditBookEvent event) {
        if (!Settings.FORMAT_BOOKS.toBool() || manager.useNmsGlyphs()) return;

        BookMeta meta = event.getNewBookMeta();
        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;
            if (containsUnpermittedGlyph(event.getPlayer(), page))
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

        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = entry.getValue().getCharacters();
                if (entry.getValue().hasPermission(player))
                    page = (manager.permsChatcolor == null)
                            ? page.replace(entry.getKey(), ChatColor.WHITE + unicode + ChatColor.BLACK)
                            .replace(unicode, ChatColor.WHITE + unicode + ChatColor.BLACK)
                            : page.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(player, manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode + ChatColor.BLACK);
                meta.setPage(i, AdventureUtils.parseLegacy(page));
            }
        }

        Book book = Book.builder()
                .title(MINI_MESSAGE.deserialize(meta.getTitle() != null ? meta.getTitle() : ""))
                .author(MINI_MESSAGE.deserialize(meta.getAuthor() != null ? meta.getAuthor() : ""))
                .pages(meta.getPages().stream().map(p -> MINI_MESSAGE_EMPTY.deserialize(p, GlyphTag.getResolverForPlayer(player))).toList())
                .build();

        // Open fake book and deny opening of original book to avoid needing to format the original book
        event.setUseItemInHand(Event.Result.DENY);
        OraxenPlugin.get().getAudience().player(player).openBook(book);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignGlyph(final SignChangeEvent event) {
        if (!Settings.FORMAT_SIGNS.toBool() || manager.useNmsGlyphs()) return;

        Player player = event.getPlayer();
        String[] lines = event.getLines();
        for (int i = 0; i < lines.length; i++) {
            String line = AdventureUtils.parseLegacyThroughMiniMessage(lines[i]);
            if (containsUnpermittedGlyph(player, line))
                event.setCancelled(true);

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = entry.getValue().getCharacters();
                if (entry.getValue().hasPermission(player))
                    line = (manager.permsChatcolor == null)
                            ? line.replace(entry.getKey(), ChatColor.WHITE + unicode + ChatColor.BLACK)
                            .replace(unicode, ChatColor.WHITE + unicode + ChatColor.BLACK)
                            : line.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(player, manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode + ChatColor.BLACK);
            }
            event.setLine(i, line);
        }
    }

    @EventHandler
    public void onPlayerRename(final InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory clickedInv)) return;
        if (!Settings.FORMAT_ANVIL.toBool() || manager.useNmsGlyphs() || event.getSlot() != 2) return;
        if (VersionUtil.atOrAbove("1.20.5")) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack inputItem = clickedInv.getItem(0);
        ItemStack resultItem = clickedInv.getItem(2);
        if (resultItem == null || !OraxenItems.exists(inputItem)) return;

        String displayName = processRenameDisplayName(player, clickedInv.getRenameText(), inputItem);

        String finalDisplayName = displayName;
        ItemUtils.editItemMeta(resultItem, meta -> {
            if (finalDisplayName == null) meta.setDisplayName(null);
            else if (VersionUtil.isPaperServer()) meta.displayName(MINI_MESSAGE.deserialize(finalDisplayName));
            else meta.setDisplayName(finalDisplayName);
        });
    }

    private String processRenameDisplayName(Player player, String displayName, ItemStack inputItem) {
        if (displayName != null) {
            displayName = AdventureUtils.parseLegacyThroughMiniMessage(displayName);
            displayName = replaceUnpermittedGlyphs(player, displayName);
            displayName = replaceGlyphPlaceholders(player, displayName);
        }

        // If displayName is unchanged from input or empty, restore original name
        String strippedInput = MINI_MESSAGE.stripTags(AdventureUtils.parseLegacy(inputItem.getItemMeta().getDisplayName()));
        if (((displayName == null || displayName.isEmpty()) && OraxenItems.exists(inputItem))
                || strippedInput.equals(displayName)) {
            return inputItem.getItemMeta().getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
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
        for (Glyph glyph : manager.getGlyphs().stream()
                .sorted(Comparator.comparingInt((Glyph glyph) -> glyph.getCharacters().length()).reversed())
                .toList()) {
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
                    : ChatColor.WHITE + entry.getValue().getCharacters()
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
        manager.clearGlyphTabCompletions(event.getPlayer());
    }

    public class SpigotChatHandler implements Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isLegacy() || manager.useNmsGlyphs()) return;

            String format = format(event.getFormat(), null);
            String message = format(event.getMessage(), event.getPlayer());
            if (format == null || message == null) event.setCancelled(true);
            else {
                event.setFormat(format);
                event.setMessage(message);
            }
        }

        /**
         * Formats a string with glyphs and placeholders
         *
         * @param string The string to format
         * @param player The player to check permissions for, if null it parses the string without checking permissions
         * @return The formatted string, or null if the player doesn't have permission for a glyph
         */
        private String format(String string, @Nullable Player player) {
            TextComponent component = (TextComponent) AdventureUtils.MINI_MESSAGE_PLAYER(player).deserialize(string);
            if (player != null) for (String character : manager.getReverseMap().keySet()) {
                if (!component.content().contains(character)) continue;
                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(player)) {
                    Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.getPermission()));
                    return null;
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String placeholder = entry.getKey();
                Glyph glyph = entry.getValue();

                if (player == null || glyph.hasPermission(player)) {
                    component = (TextComponent) component.replaceText(TextReplacementConfig.builder()
                            .matchLiteral(placeholder).replacement(glyph.getGlyphComponent()).build());
                }
            }

            // Process animated glyphs
            for (AnimatedGlyph animGlyph : manager.getAnimatedGlyphs()) {
                if (player == null || animGlyph.hasPermission(player)) {
                    for (String placeholder : animGlyph.getPlaceholders()) {
                        component = (TextComponent) component.replaceText(TextReplacementConfig.builder()
                                .matchLiteral(placeholder).replacement(animGlyph.getGlyphComponent()).build());
                    }
                }
            }

            return LEGACY_SERIALIZER.serialize(component);
        }
    }


    @SuppressWarnings("UnstableApiUsage")
    public class PaperChatHandler implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatDecorateEvent event) {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isModern() || manager.useNmsGlyphs()) return;
            event.result(format(event.result(), event.player()));
        }

    }

    public class LegacyPaperChatHandler implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatEvent event) {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isModern() || manager.useNmsGlyphs()) return;
            // AsyncChatDecorateEvent has formatted the component if server is 1.19.1+
            Component message = VersionUtil.atOrAbove("1.19.1") ? event.message() : format(event.message(), event.getPlayer());
            message = message != null ? message : Component.empty();
            if (!message.equals(Component.empty())) return;

            event.viewers().clear();
            event.setCancelled(true);
        }

    }

    private Component format(Component message, Player player) {
        Key randomKey = Key.key("random");
        String serialized = MINI_MESSAGE.serialize(message);
        for (Glyph glyph : manager.getGlyphs().stream()
                .sorted(Comparator.comparingInt((Glyph glyph) -> glyph.getCharacters().length()).reversed())
                .toList()) {
            String characters = glyph.getCharacters();
            if (!serialized.contains(characters)) continue;
            if (!glyph.hasPermission(player)) message = message.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(characters)
                            .replacement(glyph.getGlyphComponent().font(randomKey))
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
