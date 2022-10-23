package io.th0rgal.oraxen.font;

import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public class FontEvents implements Listener {

    private final FontManager manager;

    public FontEvents(FontManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onBookGlyph(final PlayerEditBookEvent event) {
        BookMeta meta = event.getNewBookMeta();
        for (String page : meta.getPages()) {
            int i = meta.getPages().indexOf(page) + 1;
            if (i == 0) continue;

            for (Character character : manager.getReverseMap().keySet()) {
                if (!page.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), AdventureUtils.tagResolver("permission", glyph.getPermission()));
                    event.setCancelled(true);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().getCharacter());
                if (entry.getValue().hasPermission(event.getPlayer()))
                    page = (manager.permsChatcolor == null)
                            ? page.replace(entry.getKey(), ChatColor.WHITE + unicode)
                            .replace(unicode, ChatColor.WHITE + unicode)
                            : page.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode);
                meta.setPage(i, AdventureUtils.parseLegacyThroughMiniMessage(page));
            }
        }
        event.setNewBookMeta(meta);
    }

    @EventHandler
    public void onSignGlyph(final SignChangeEvent event) {
        for (String line : event.getLines()) {
            int i = Arrays.stream(event.getLines()).toList().indexOf(line);
            if (i == -1) continue;
            for (Character character : manager.getReverseMap().keySet()) {
                if (!line.contains(String.valueOf(character))) continue;

                Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), AdventureUtils.tagResolver("permission", glyph.getPermission()));
                    event.setCancelled(true);
                }
            }

            for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                String unicode = String.valueOf(entry.getValue().getCharacter());
                if (entry.getValue().hasPermission(event.getPlayer()))
                    line = (manager.permsChatcolor == null)
                            ? line.replace(entry.getKey(), ChatColor.WHITE + unicode)
                            .replace(unicode, ChatColor.WHITE + unicode)
                            : line.replace(entry.getKey(), ChatColor.WHITE + unicode + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor))
                            .replace(unicode, ChatColor.WHITE + unicode);
            }
            event.setLine(i, AdventureUtils.parseLegacyThroughMiniMessage(line));
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        for (Character character : manager.getReverseMap().keySet()) {
            if (!message.contains(String.valueOf(character)))
                continue;
            Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
            if (!glyph.hasPermission(event.getPlayer())) {
                Message.NO_PERMISSION.send(event.getPlayer(), AdventureUtils.tagResolver("permission", glyph.getPermission()));
                event.setCancelled(true);
            }
        }
        for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet())
            if (entry.getValue().hasPermission(event.getPlayer()))
                message = (manager.permsChatcolor == null)
                        ? message.replace(entry.getKey(),
                        String.valueOf(entry.getValue().getCharacter()))
                        : message.replace(entry.getKey(),
                        ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter())
                                + PapiAliases.setPlaceholders(event.getPlayer(), manager.permsChatcolor));

        event.setMessage(message);
    }

    @EventHandler
    public void onPlayerRename(final InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof AnvilInventory clickedInv)) return;
        Player player = (Player) event.getWhoClicked();
        String displayName = clickedInv.getRenameText();

        switch (event.getSlot()) {
            case 0 -> { // Clicking first item
                ItemStack cursor = event.getCursor();
                ItemStack current = event.getCurrentItem();

                // Adding item to first slot
                if (cursor != null && cursor.getType() != Material.AIR) {
                    ItemMeta meta = cursor.getItemMeta();
                    if (meta == null || !meta.hasDisplayName()) return;
                    String name = meta.getDisplayName();
                    name = AdventureUtils.MINI_MESSAGE.serialize(AdventureUtils.LEGACY_SERIALIZER.deserialize(name)).replace("\\<", "<");
                    meta.setDisplayName(name);
                    cursor.setItemMeta(meta);
                }
                // Taking item from first slot
                else if (current != null && current.getType() != Material.AIR) {
                    ItemMeta meta = current.getItemMeta();
                    if (meta == null || !meta.hasDisplayName()) return;
                    String name = meta.getDisplayName();
                    meta.setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage(name));
                    current.setItemMeta(meta);
                }
            }
            case 2 -> { // Clicking result item
                ItemStack clickedItem = clickedInv.getItem(2);
                if (clickedItem == null) return;
                if (displayName == null || displayName.isBlank()) return;
                for (Character character : manager.getReverseMap().keySet()) {
                    if (!displayName.contains(String.valueOf(character))) continue;
                    Glyph glyph = manager.getGlyphFromName(manager.getReverseMap().get(character));
                    if (!glyph.hasPermission(player)) {
                        Glyph required = manager.getGlyphFromName("required");
                        String replacement = required.hasPermission(player) ? String.valueOf(required.getCharacter()) : "";
                        Message.NO_PERMISSION.send(player, AdventureUtils.tagResolver("permission", glyph.getPermission()));
                        displayName = displayName.replace(String.valueOf(character), replacement);
                    }
                }

                for (Map.Entry<String, Glyph> entry : manager.getGlyphByPlaceholderMap().entrySet()) {
                    if (entry.getValue().hasPermission(player))
                        displayName = (manager.permsChatcolor == null)
                                ? displayName.replace(entry.getKey(),
                                String.valueOf(entry.getValue().getCharacter()))
                                : displayName.replace(entry.getKey(),
                                ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter())
                                        + PapiAliases.setPlaceholders(player, manager.permsChatcolor));
                }

                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null) return;
                meta.setDisplayName(AdventureUtils.parseLegacyThroughMiniMessage(displayName));
                clickedItem.setItemMeta(meta);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.sendGlyphTabCompletion(event.getPlayer(), true);
    }
}
