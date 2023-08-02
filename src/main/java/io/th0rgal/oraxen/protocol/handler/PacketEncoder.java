package io.th0rgal.oraxen.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolUtil;
import io.th0rgal.oraxen.protocol.packet.Packet;

@ChannelHandler.Sharable
public class PacketEncoder extends MessageToByteEncoder<Packet> {

    private final MinecraftVersion version;

    public PacketEncoder(MinecraftVersion version) {
        this.version = version;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
        ProtocolUtil.writeVarInt(out, packet.getID(version));
        packet.encode(out, version);
    }
}