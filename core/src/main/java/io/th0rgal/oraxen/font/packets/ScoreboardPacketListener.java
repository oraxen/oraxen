package io.th0rgal.oraxen.font.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import io.th0rgal.oraxen.OraxenPlugin;

public class ScoreboardPacketListener extends PacketAdapter {

    public ScoreboardPacketListener() {
        super(OraxenPlugin.get(), ListenerPriority.MONITOR, PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        try {
            if (packet.getIntegers().read(0) == 1) return;
            packet.getModifier().write(3, NumberFormat.blank());
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

}
