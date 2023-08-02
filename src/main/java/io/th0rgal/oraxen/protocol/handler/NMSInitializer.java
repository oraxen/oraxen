package io.th0rgal.oraxen.protocol.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.jetbrains.annotations.NotNull;

public class NMSInitializer extends ChannelInboundHandlerAdapter  {

    private static final AttributeKey<?> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");

    private final ProtocolInjector protocolInjector;

    public NMSInitializer(ProtocolInjector protocolInjector) {
        this.protocolInjector = protocolInjector;
    }

    public void runInitializers(PacketFlow flow) {
        protocolInjector.getAllNMSHandlers(flow).forEach((i, handlers) -> handlers.forEach(NMSPacketHandler::initialize));
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        Logs.logInfo("Initializing NMS protocol...");
        Object connectionProtocol = ctx.channel().attr(ATTRIBUTE_PROTOCOL).get();
        protocolInjector.initializeProtocol(connectionProtocol.getClass());
        runInitializers(PacketFlow.CLIENTBOUND);
        runInitializers(PacketFlow.SERVERBOUND);
        ctx.pipeline().remove(this);
        super.channelRead(ctx, msg);
    }
}
