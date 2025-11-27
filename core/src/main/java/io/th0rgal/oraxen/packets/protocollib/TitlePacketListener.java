package io.th0rgal.oraxen.packets.protocollib;

import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.PacketHelpers;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;

import static com.comphenix.protocol.PacketType.Play.Server.*;

public class TitlePacketListener extends PacketAdapter {

    public TitlePacketListener() {
        super(OraxenPlugin.get(), ListenerPriority.MONITOR, SET_TITLE_TEXT, SET_SUBTITLE_TEXT, SET_ACTION_BAR_TEXT);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        if (packet.getType() == SET_TITLE_TEXT && Settings.FORMAT_TITLES.toBool()) {
            WrappedChatComponent title = formatTitle(packet);
            if (title != null) packet.getChatComponents().write(0, title);
        } else if (packet.getType() == SET_SUBTITLE_TEXT && Settings.FORMAT_SUBTITLES.toBool()) {
            WrappedChatComponent subtitle = formatTitle(packet);
            if (subtitle != null) packet.getChatComponents().write(0, subtitle);
        } else if (packet.getType() == SET_ACTION_BAR_TEXT) {
            if (Settings.FORMAT_ACTION_BAR.toBool()) {
                WrappedChatComponent actionbar = formatTitle(packet);
                if (actionbar != null) packet.getChatComponents().write(0, actionbar);
            }
        }
    }

    private WrappedChatComponent formatTitle(PacketContainer packet) {
        try {
            String title;
            if (packet.getChatComponents().read(0) == null) {
                if (packet.getModifier().size() > 1) {
                    title = AdventureUtils.MINI_MESSAGE.serialize((Component) packet.getModifier().read(1));
                } else {
                    return null;
                }
            } else {
                title = PacketHelpers.readJson(packet.getChatComponents().read(0).getJson());
            }
            return WrappedChatComponent.fromJson(PacketHelpers.toJson(title));
        } catch (Exception e) {
            String type;
            if (packet.getType() == SET_TITLE_TEXT) type = "title";
            else if (packet.getType() == SET_SUBTITLE_TEXT) type = "subtitle";
            else type = "actionbar";
            Logs.logWarning("Error whilst reading " + type + " packet: " + e.getMessage());
            if (Settings.DEBUG.toBool()) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
