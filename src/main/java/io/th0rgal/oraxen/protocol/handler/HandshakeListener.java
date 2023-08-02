package io.th0rgal.oraxen.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolUtil;
import org.jetbrains.annotations.NotNull;

public class HandshakeListener extends ChannelInboundHandlerAdapter {

    private final ProtocolInjector protocolInjector;

    public HandshakeListener(ProtocolInjector protocolInjector) {
        this.protocolInjector = protocolInjector;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf buf) {
            int ridx = buf.readerIndex();

            if (ProtocolUtil.readVarInt(buf) == 0) {
                int protocol = ProtocolUtil.readVarInt(buf);
                LoginListener loginListener = new LoginListener(protocolInjector, protocol);

                ProtocolUtil.readString(buf);
                buf.readShort();

                if (ProtocolUtil.readVarInt(buf) == 2) {
                    ctx.pipeline().addBefore("oraxen:handshake", "oraxen:login", loginListener);
                }

                ctx.pipeline().remove(this);
            }

            buf.readerIndex(ridx);
        }

        super.channelRead(ctx, msg);
    }
}
