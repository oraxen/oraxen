package io.th0rgal.oraxen.protocol.handler;

import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import org.bukkit.entity.Player;

public interface NMSPacketHandler {

    void initialize();

    boolean handle(Player player, PacketFlow flow, int id, Object packet);
}
