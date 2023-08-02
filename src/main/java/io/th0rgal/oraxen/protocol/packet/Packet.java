package io.th0rgal.oraxen.protocol.packet;

import io.netty.buffer.ByteBuf;
import io.th0rgal.oraxen.protocol.MinecraftVersion;

public interface Packet {

    void encode(ByteBuf buf, MinecraftVersion version);

    int getID(MinecraftVersion version);
}