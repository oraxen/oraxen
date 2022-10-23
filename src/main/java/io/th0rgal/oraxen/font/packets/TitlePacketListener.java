package io.th0rgal.oraxen.font.packets;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

import java.util.Map;

import static com.comphenix.protocol.PacketType.Play.Server.*;

public class TitlePacketListener extends PacketAdapter {

    public TitlePacketListener() {
        super(OraxenPlugin.get(), SET_TITLE_TEXT, SET_SUBTITLE_TEXT, SET_ACTION_BAR_TEXT, PLAYER_LIST_HEADER_FOOTER);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        PacketContainer packet = event.getPacket();

        if (packet.getType() == PLAYER_LIST_HEADER_FOOTER) {
            String header;
            String footer;
            if (packet.getChatComponents().read(0) == null)
                header = Utils.MINI_MESSAGE.serialize((Component) packet.getModifier().read(2));
            else header = PapiAliases.readJson(packet.getChatComponents().read(0).getJson());
            if (packet.getChatComponents().read(1) == null)
                footer = Utils.MINI_MESSAGE.serialize((Component) packet.getModifier().read(3));
            else footer = PapiAliases.readJson(packet.getChatComponents().read(1).getJson());

            packet.getModifier().write(2, Utils.MINI_MESSAGE.deserialize(header));
            packet.getModifier().write(3, Utils.MINI_MESSAGE.deserialize(footer));
        } else {
            try {
                String title;
                if (packet.getChatComponents().read(0) == null) title = Utils.MINI_MESSAGE.serialize((Component) packet.getModifier().read(1));
                else title = PapiAliases.readJson(packet.getChatComponents().read(0).getJson());

                for (Character character : fontManager.getReverseMap().keySet()) {
                    if (!title.contains(String.valueOf(character)))
                        continue;
                    Glyph glyph = fontManager.getGlyphFromName(fontManager.getReverseMap().get(character));
                    if (!glyph.hasPermission(event.getPlayer())) {
                        Message.NO_PERMISSION.send(event.getPlayer(), Utils.tagResolver("permission", glyph.getPermission()));
                        event.setCancelled(true);
                    }
                }

                for (Map.Entry<String, Glyph> entry : fontManager.getGlyphByPlaceholderMap().entrySet())
                    if (entry.getValue().hasPermission(event.getPlayer()))
                        title = (fontManager.permsChatcolor == null)
                                ? title.replace(entry.getKey(),
                                String.valueOf(entry.getValue().getCharacter()))
                                : title.replace(entry.getKey(),
                                ChatColor.WHITE + String.valueOf(entry.getValue().getCharacter())
                                        + PapiAliases.setPlaceholders(event.getPlayer(), fontManager.permsChatcolor));

                event.getPacket().getModifier().write(1, title);
            } catch (Exception ignored) {

            }
        }
    }
}
