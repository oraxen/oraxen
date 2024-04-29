package io.th0rgal.oraxen.nms.v1_20_R2.protocol;

import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.NotNull;

public interface PacketHandler<T extends Packet<?>> {
    boolean handle(@NotNull T packet);
}
