package io.th0rgal.oraxen.font.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.ChatColor;

import java.util.Map;

public class InventoryPacketListener extends PacketAdapter {

    public InventoryPacketListener() {
        super(OraxenPlugin.get(), PacketType.Play.Server.OPEN_WINDOW);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        PacketContainer packet = event.getPacket();
        try {
            String chat = PapiAliases.readJson(packet.getChatComponents().read(0).getJson());
            for (Character character : fontManager.getReverseMap().keySet()) {
                if (!chat.contains(String.valueOf(character)))
                    continue;
                Glyph glyph = fontManager.getGlyphFromName(fontManager.getReverseMap().get(character));
                if (!glyph.hasPermission(event.getPlayer())) {
                    Message.NO_PERMISSION.send(event.getPlayer(), Utils.tagResolver("permission", glyph.getPermission()));
                    event.setCancelled(true);
                }
            }
            for (Map.Entry<String, Glyph> entry : fontManager.getGlyphByPlaceholderMap().entrySet())
                if (entry.getValue().hasPermission(event.getPlayer()))
                    chat = (fontManager.permsChatcolor == null)
                            ? chat.replace(entry.getKey(),
                            String.valueOf(entry.getValue().getCharacter()))
                            : chat.replace(entry.getKey(),
                            ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter())
                                    + PapiAliases.setPlaceholders(event.getPlayer(), fontManager.permsChatcolor));
            packet.getChatComponents().write(0, WrappedChatComponent.fromJson(chat));
        } catch (Exception ignored) {

        }
    }

}
