package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import io.th0rgal.oraxen.utils.PacketHelpers;

public class InventoryPacketListener implements PacketListener {
    @Override public void onPacketSend(PacketSendEvent event) {
        if(event.getPacketType() != PacketType.Play.Server.OPEN_WINDOW) return;
        var wrapper = new WrapperPlayServerOpenWindow(event);
        wrapper.setTitle(PacketHelpers.translateTitle(wrapper.getTitle()));
    }
}
