package io.th0rgal.oraxen.protocol.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class NMSHandler extends ChannelDuplexHandler {

    private final ProtocolInjector protocolInjector;

    public NMSHandler(ProtocolInjector protocolInjector) {
        this.protocolInjector = protocolInjector;
    }

    private boolean callHandlers(Channel channel, PacketFlow flow, Object packet) {
        try {
            int id = protocolInjector.getNMSPacketID(flow, packet);
            if (id == -1) {
                return true;
            }

            Set<NMSPacketHandler> handlers = protocolInjector.getNMSHandlers(flow, id);
            if (handlers == null) {
                return true;
            }

            Player player = channel.attr(ProtocolInjector.PLAYER_ATTRIBUTE).get();
            for (NMSPacketHandler handler : handlers) {
                if (!handler.handle(player, flow, id, packet)) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }

        return true;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (callHandlers(ctx.channel(), PacketFlow.SERVERBOUND, msg)) {
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (callHandlers(ctx.channel(), PacketFlow.CLIENTBOUND, msg)) {
            super.write(ctx, msg, promise);
        }
    }
}
