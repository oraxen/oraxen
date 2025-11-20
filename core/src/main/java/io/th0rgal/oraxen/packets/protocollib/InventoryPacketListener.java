package io.th0rgal.oraxen.packets.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.utils.PacketHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;

public class InventoryPacketListener extends PacketAdapter {

    public InventoryPacketListener() {
        super(OraxenPlugin.get(), ListenerPriority.MONITOR, PacketType.Play.Server.OPEN_WINDOW);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (VersionUtil.atOrAbove("1.21.4")) {
            return;
        }
        PacketContainer packet = event.getPacket();
        try {
            String chat = PacketHelpers.readJson(packet.getChatComponents().read(0).getJson());
            packet.getChatComponents().write(0, WrappedChatComponent.fromJson(PacketHelpers.toJson(chat)));
        } catch (Throwable ignored) {

        }
    }

}
