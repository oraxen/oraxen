package io.th0rgal.oraxen.nms.v1_20_R4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlyphHandler implements io.th0rgal.oraxen.nms.GlyphHandler {

    private static final Map<PacketType<?>, ProtocolInfo<?>> INFO = new HashMap<>();
    @Override
    public void setupNmsGlyphs() {
        RegistryAccess access = access();
        try {
            Field byId = Arrays.stream(IdDispatchCodec.class.getDeclaredFields()).filter(f -> List.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
            byId.setAccessible(true);
            for (ProtocolInfo<? extends PacketListener> protocolInfo : List.of(
                    GameProtocols.CLIENTBOUND.bind(b -> new RegistryFriendlyByteBuf(b, access)),
                    GameProtocols.SERVERBOUND.bind(b -> new RegistryFriendlyByteBuf(b, access)),
                    HandshakeProtocols.SERVERBOUND,
                    StatusProtocols.CLIENTBOUND,
                    StatusProtocols.SERVERBOUND,
                    LoginProtocols.CLIENTBOUND,
                    LoginProtocols.SERVERBOUND,
                    ConfigurationProtocols.CLIENTBOUND,
                    ConfigurationProtocols.SERVERBOUND
            )) {
                List<?> list = (List<?>) byId.get(protocolInfo.codec());
                for (Object object : list) {
                    Field type = object.getClass().getDeclaredFields()[1];
                    type.setAccessible(true);
                    PacketType<?> packetType = (PacketType<?>) type.get(object);
                    INFO.put(packetType, protocolInfo);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (VersionUtil.isPaperServer()) Bukkit.getPluginManager().registerEvents(new GlyphListener(), OraxenPlugin.get());
    }

    @Override
    public void inject(Player player) {
        ChannelPipeline pipeline = channel(player).pipeline();
        for (Map.Entry<String, ChannelHandler> entry : pipeline.toMap().entrySet()) {
            if (entry.getValue() instanceof Connection) pipeline.addBefore(entry.getKey(), "oraxen", new PlayerHandler(player));
        }
    }

    @Override
    public void uninject(Player player) {
        Channel channel = channel(player);
        channel.eventLoop().submit(() -> channel.pipeline().remove("oraxen"));
    }

    private static Channel channel(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel;
    }
    private static RegistryAccess access() {
        return ((CraftServer) Bukkit.getServer()).getHandle().getServer().registryAccess();
    }

    private static class GlyphBuffer extends RegistryFriendlyByteBuf {
        private final ByteBuf original;

        private GlyphBuffer(ByteBuf buf) {
            super(buf, access());
            original = buf;
        }

        @NotNull
        @Override
        public GlyphBuffer copy() {
            return new GlyphBuffer(Unpooled.copiedBuffer(original));
        }

    }
    private static class PlayerHandler extends ChannelDuplexHandler {
        private final Player player;
        private PlayerHandler(Player player) {
            this.player = player;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            Object result = msg;
            if (msg instanceof Packet<?> packet) {
                result = newPacket(packet, false);
            }
            super.channelRead(ctx, result);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            Object result = msg;
            if (msg instanceof Packet<?> packet) {
                result = newPacket(packet, true);
            }
            super.write(ctx, result, promise);
        }

        private Packet<?> newPacket(Packet<?> packet, boolean clientbound) {
            ProtocolInfo<?> info = INFO.get(packet.type());
            if (info != null) {
                StreamCodec<ByteBuf, Packet<?>> codec = (StreamCodec<ByteBuf, Packet<?>>) info.codec();
                try {
                    GlyphBuffer buf = new GlyphBuffer(Unpooled.buffer());
                    codec.encode(buf, packet);
                    return codec.decode(writeNewNbt(buf, clientbound));
                } catch (Exception ignored) {
                }
            }
            return packet;
        }

        private GlyphBuffer writeNewNbt(GlyphBuffer original, boolean clientbound) {
            GlyphBuffer newBuffer = new GlyphBuffer(Unpooled.buffer());
            int id = VarInt.read(original);
            VarInt.write(newBuffer, id);
            try {
                while (original.isReadable()) {
                    byte b = original.readByte();
                    TagType<?> type = TagTypes.getType(b);
                    if (type != CompoundTag.TYPE || !original.isReadable()) {
                        newBuffer.writeByte(b);
                    } else try (DataInputStream input = new DataInputStream(new ByteBufInputStream(original))) {
                        CompoundTag getTag = CompoundTag.TYPE.load(input, NbtAccounter.unlimitedHeap());
                        try {
                            Component newComponent = GlyphHandlers.transform(
                                    PaperAdventure.asAdventure(ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, getTag).getOrThrow().getFirst()),
                                    clientbound ? null : player,
                                    false
                            );
                            CompoundTag tag = new CompoundTag();
                            ComponentSerialization.CODEC.encode(PaperAdventure.asVanilla(newComponent), NbtOps.INSTANCE, tag);
                            getTag = tag;
                        } catch (Exception ignored) {
                        }
                        try (DataOutputStream output = new DataOutputStream(new ByteBufOutputStream(newBuffer))) {
                            getTag.write(output);
                        } catch (Exception e2) {
                            return original;
                        }
                    } catch (Exception e) {
                        newBuffer.writeByte(b);
                    }
                }
                return newBuffer;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
