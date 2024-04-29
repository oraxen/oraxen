package io.th0rgal.oraxen.nms.v1_18_R1.protocol;

import net.minecraft.server.level.ServerPlayer;

public interface PacketListener {
    boolean listen(ServerPlayer player, Object packet);
}
