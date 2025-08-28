package io.th0rgal.oraxen.packets.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedNumberFormat;
import io.th0rgal.oraxen.OraxenPlugin;

import java.util.Optional;

public class ScoreboardPacketListener extends PacketAdapter {

    public ScoreboardPacketListener() {
        super(OraxenPlugin.get(), ListenerPriority.MONITOR, PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
    }

    private final Optional<InternalStructure> numberFormat = Optional.of(InternalStructure.getConverter().getSpecific(WrappedNumberFormat.fixed(WrappedChatComponent.fromText("test")).getHandle()));
    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        try {
            if (packet.getIntegers().read(0) == 1) return;
            packet.getOptionalStructures().write(0, numberFormat);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

}
