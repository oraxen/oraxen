package io.th0rgal.oraxen.font.packets;

import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PapiAliases;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;

import static com.comphenix.protocol.PacketType.Play.Server.*;

public class TitlePacketListener extends PacketAdapter {

    public TitlePacketListener() {
        super(OraxenPlugin.get(), SET_TITLE_TEXT, SET_SUBTITLE_TEXT, /*SET_ACTION_BAR_TEXT,*/ PLAYER_LIST_HEADER_FOOTER);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        FontManager fontManager = OraxenPlugin.get().getFontManager();
        PacketContainer packet = event.getPacket();

        if (packet.getType() == PLAYER_LIST_HEADER_FOOTER) {
            Component header;
            Component footer;
            try {
                if (packet.getChatComponents().read(0) == null)
                    header = (Component) packet.getModifier().read(2);
                else header = Utils.MINI_MESSAGE.deserialize(PapiAliases.readJson(packet.getChatComponents().read(0).getJson()));
                if (packet.getChatComponents().read(1) == null)
                    footer = (Component) packet.getModifier().read(3);
                else footer = Utils.MINI_MESSAGE.deserialize(PapiAliases.readJson(packet.getChatComponents().read(1).getJson()));

                packet.getModifier().write(2, header);
                packet.getModifier().write(3, footer);
            } catch (Exception e) {
                Logs.logWarning("Error while reading header/footer packet");
            }
        } else {
            try {
                Component title;
                if (packet.getChatComponents().read(0) == null) title = (Component) packet.getModifier().read(1);
                else title = Utils.MINI_MESSAGE.deserialize(PapiAliases.readJson(packet.getChatComponents().read(0).getJson()));

                event.getPacket().getModifier().write(1, title);
            } catch (Exception e) {
                Logs.logWarning("Error whilst reading title/subtitle packet");
                e.printStackTrace();
            }
        }
    }
}
