package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.PacketHelpers;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.ACTION_BAR;
import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.SET_TITLE_SUBTITLE;
import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.SET_TITLE_TEXT;

public class TitlePacketListener implements PacketListener {
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if(event.getPacketType() == SET_TITLE_TEXT  && Settings.FORMAT_TITLES.toBool()) {
            var wrapper = new WrapperPlayServerSetTitleText(event);
            wrapper.setTitle(PacketHelpers.translateTitle(wrapper.getTitle()));
        } else if(event.getPacketType() == SET_TITLE_SUBTITLE && Settings.FORMAT_SUBTITLES.toBool()) {
            var wrapper = new WrapperPlayServerSetTitleSubtitle(event);
            wrapper.setSubtitle(PacketHelpers.translateTitle(wrapper.getSubtitle()));
        } else if(event.getPacketType() == ACTION_BAR && Settings.FORMAT_ACTION_BAR.toBool()) {
            var wrapper = new WrapperPlayServerActionBar(event);
            wrapper.setActionBarText(PacketHelpers.translateTitle(wrapper.getActionBarText()));
        }
    }
}
