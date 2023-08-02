package io.th0rgal.oraxen.protocol.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import org.jetbrains.annotations.NotNull;

public class DisconnectHandler extends ChannelInboundHandlerAdapter {

    private final ProtocolInjector protocolInjector;
    private final String name;

    public DisconnectHandler(ProtocolInjector protocolInjector, String name) {
        this.protocolInjector = protocolInjector;
        this.name = name;
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        protocolInjector.getPlayerChannels().remove(name);
        super.channelInactive(ctx);
    }
}