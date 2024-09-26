package io.th0rgal.oraxen.font;

import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
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
import java.util.Map;

import static io.th0rgal.oraxen.items.ItemBuilder.ORIGINAL_NAME_KEY;
import static io.th0rgal.oraxen.utils.AdventureUtils.*;

public class FontEvents implements Listener {

    private final FontManager manager;
    @Nullable PaperChatHandler paperChatHandler;
    @Nullable LegacyPaperChatHandler legacyPaperChatHandler;
    @Nullable SpigotChatHandler spigotChatHandler;

    public enum ChatHandler {
        LEGACY,
        MODERN;

        public static boolean isLegacy() {
            return get() == LEGACY;
        }

        public static boolean isModern() {
            return get() == MODERN;
        }

        public static ChatHandler get() {
            return Settings.CHAT_HANDLER.toEnumOrGet(ChatHandler.class, () -> {
                ChatHandler chatHandler = VersionUtil.isPaperServer() ? MODERN : LEGACY;
                Logs.logError("Invalid chat-handler defined in settings.yml, defaulting to " + chatHandler, true);
                Logs.logError("Valid options are: " + Arrays.toString(values()), true);
                return chatHandler;
            });
        }
    }

    public FontEvents(FontManager manager) {
        this.manager = manager;
        if (VersionUtil.isPaperServer()) paperChatHandler = new PaperChatHandler();
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
        if (!Settings.FORMAT_BOOKS.toBool()) return;

        BookMeta meta = event.getNewBookMeta();
        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;
            for (Character character : manager.getReverseMap().keySet()) {
                if (!page.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), AdventureUtils.tagResolver("permission", glyph.permission()));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookGlyph(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (!Settings.FORMAT_BOOKS.toBool()) return;

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (item == null || !(item.getItemMeta() instanceof BookMeta meta) || item.getType() != Material.WRITTEN_BOOK) return;
        if (event.useInteractedBlock() == Event.Result.ALLOW) return;

        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().character());
                if (entry.getValue().hasPermission(player))
                    page = page.replace(entry.getKey(), ChatColor.WHITE + unicode + ChatColor.BLACK)
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
        OraxenPlugin.get().audience().player(player).openBook(book);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignGlyph(final SignChangeEvent event) {
        if (!Settings.FORMAT_SIGNS.toBool()) return;

        Player player = event.getPlayer();
        for (String line : event.getLines()) {
            line = AdventureUtils.parseLegacyThroughMiniMessage(line);
            int i = Arrays.stream(event.getLines()).toList().indexOf(line);
            if (i == -1) continue;
            for (Character character : manager.getReverseMap().keySet()) {
                if (!line.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(player)) {
                    Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.permission()));
                    event.setCancelled(true);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().character());
                if (entry.getValue().hasPermission(player))
                    line = line.replace(entry.getKey(), ChatColor.WHITE + unicode + ChatColor.BLACK)
                            .replace(unicode, ChatColor.WHITE + unicode + ChatColor.BLACK);
            }
            event.setLine(i, AdventureUtils.parseLegacy(line));
        }
    }

    @EventHandler
    public void onPlayerRename(final InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory clickedInv)) return;
        if (!Settings.FORMAT_ANVIL.toBool() || event.getSlot() != 2) return;

        Player player = (Player) event.getWhoClicked();
        String displayName = clickedInv.getRenameText();
        ItemStack inputItem = clickedInv.getItem(0);
        ItemStack resultItem = clickedInv.getItem(2);
        if (resultItem == null || !OraxenItems.exists(inputItem)) return;

        if (displayName != null) {
            displayName = AdventureUtils.parseLegacyThroughMiniMessage(displayName);
            for (Character character : manager.getReverseMap().keySet()) {
                if (!displayName.contains(String.valueOf(character))) continue;
                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(player)) {
                    Glyph required = manager.getGlyphFromName("required");
                    String replacement = required.hasPermission(player) ? String.valueOf(required.character()) : "";
                    Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.permission()));
                    displayName = displayName.replace(String.valueOf(character), replacement);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                if (entry.getValue().hasPermission(player))
                    displayName = displayName.replace(entry.getKey(),
                            String.valueOf(entry.getValue().character()));
            }
        }

        // Since getRenameText is in PlainText, check if the displayName is the same as the rename text with all tags stripped
        // If so retain the displayName of inputItem. This also fixes enchantments breaking names
        // If the displayName is null, reset it to the "original" name
        String strippedDownInputDisplay = MINI_MESSAGE.stripTags(AdventureUtils.parseLegacy(inputItem.getItemMeta().getDisplayName()));
        if (((displayName == null || displayName.isEmpty()) && OraxenItems.exists(inputItem)) || strippedDownInputDisplay.equals(displayName)) {
            displayName = inputItem.getItemMeta().getPersistentDataContainer().get(ORIGINAL_NAME_KEY, PersistentDataType.STRING);
        }

        ItemUtils.displayName(resultItem, displayName != null ? MINI_MESSAGE.deserialize(displayName) : null);
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
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isLegacy()) return;

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
            if (player != null) for (Character character : manager.getReverseMap().keySet()) {
                if (!component.content().contains(String.valueOf(character))) continue;
                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(player)) {
                    Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.permission()));
                    return null;
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String placeholder = entry.getKey();
                Glyph glyph = entry.getValue();

                if (player == null || glyph.hasPermission(player)) {
                    component = (TextComponent) component.replaceText(TextReplacementConfig.builder()
                            .matchLiteral(placeholder).replacement(glyph.glyphComponent()).build());
                }
            }

            return LEGACY_SERIALIZER.serialize(component);
        }
    }


    @SuppressWarnings("UnstableApiUsage")
    public class PaperChatHandler implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatDecorateEvent event) {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isModern()) return;
            event.result(format(event.result(), event.player()));
        }

    }

    public class LegacyPaperChatHandler implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerChat(AsyncChatEvent event) {
            if (!Settings.FORMAT_CHAT.toBool() || !ChatHandler.isModern()) return;
            Component message = event.message();
            if (!message.equals(Component.empty())) return;

            event.viewers().clear();
            event.setCancelled(true);
        }

    }

    private Component format(Component message, Player player) {
        Key randomKey = Key.key("random");
        String serialized = MINI_MESSAGE.serialize(message);
        for (Character character : manager.getReverseMap().keySet()) {
            if (!serialized.contains(character.toString())) continue;

            Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
            if (!glyph.hasPermission(player)) message.replaceText(
                    TextReplacementConfig.builder()
                            .matchLiteral(character.toString())
                            .replacement(glyph.glyphComponent().font(randomKey))
                            .build()
            );
        }

        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet())
            if (entry.getValue().hasPermission(player)) {
                message = message.replaceText(
                        TextReplacementConfig.builder()
                                .matchLiteral(entry.getKey())
                                .replacement(entry.getValue().glyphComponent()).build()
                );
            }

        return message;
    }

}
