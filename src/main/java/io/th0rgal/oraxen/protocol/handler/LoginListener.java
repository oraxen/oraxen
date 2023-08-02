package io.th0rgal.oraxen.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolUtil;
import org.jetbrains.annotations.NotNull;

public class LoginListener extends ChannelInboundHandlerAdapter {

    private final ProtocolInjector protocolInjector;
    private final int protocol;

    public LoginListener(ProtocolInjector protocolInjector, int protocol) {
        this.protocolInjector = protocolInjector;
        this.protocol = protocol;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof ByteBuf buf) {
            int ridx = buf.readerIndex();

            if (ProtocolUtil.readVarInt(buf) == 0) {
                String name = ProtocolUtil.readString(buf);
                MinecraftVersion version = MinecraftVersion.fromVersionNumber(protocol);

                protocolInjector.getPlayerChannels().put(name, ctx.channel());

                ctx.pipeline().addBefore("packet_handler", "oraxen:nms_handler", new NMSHandler(protocolInjector));
                ctx.pipeline().addAfter("prepender", "oraxen:encoder", new PacketEncoder(version));
                ctx.pipeline().addFirst("oraxen:disconnect", new DisconnectHandler(protocolInjector, name));

                ctx.pipeline().remove(this);
            }

            buf.readerIndex(ridx);
        }

        super.channelRead(ctx, msg);
    }
}
