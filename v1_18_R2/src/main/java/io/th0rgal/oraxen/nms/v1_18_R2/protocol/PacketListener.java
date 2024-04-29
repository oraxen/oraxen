package io.th0rgal.oraxen.nms.v1_18_R2.protocol;

import net.minecraft.server.level.ServerPlayer;

public interface PacketListener {
    boolean listen(ServerPlayer player, Object packet);
}
