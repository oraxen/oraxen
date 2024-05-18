package io.th0rgal.oraxen.nms.v1_20_R4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.*;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class GlyphHandler implements io.th0rgal.oraxen.nms.GlyphHandler {

    private static final Function<PacketEncoder<?>, ProtocolInfo<?>> INFO_BY_ENCODER;
    private static final Function<PacketDecoder<?>, ProtocolInfo<?>> INFO_BY_DECODER;

    static {
        Field encoder = Arrays.stream(PacketEncoder.class.getDeclaredFields()).filter(f -> ProtocolInfo.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
        Field decoder = Arrays.stream(PacketDecoder.class.getDeclaredFields()).filter(f -> ProtocolInfo.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
        encoder.setAccessible(true);
        decoder.setAccessible(true);
        INFO_BY_ENCODER = e -> {
            try {
                return (ProtocolInfo<?>) encoder.get(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        INFO_BY_DECODER = d -> {
            try {
                return (ProtocolInfo<?>) decoder.get(d);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @Override
    public void setupNmsGlyphs() {
        if (VersionUtil.isPaperServer()) Bukkit.getPluginManager().registerEvents(new GlyphListener(), OraxenPlugin.get());
    }

    @Override
    public void inject(Player player) {
        Channel channel = channel(player);
        PlayerHandler handler = new PlayerHandler(player);
        channel.eventLoop().submit(() -> {
            ChannelPipeline pipeline = channel.pipeline();
            for (Map.Entry<String, ChannelHandler> entry : pipeline) {
                if (entry.getValue() instanceof PacketEncoder<?> encoder) {
                    pipeline.replace(entry.getKey(), entry.getKey(), handler.encoder(INFO_BY_ENCODER.apply(encoder)));
                }
                if (entry.getValue() instanceof PacketDecoder<?> decoder) {
                    pipeline.replace(entry.getKey(), entry.getKey(), handler.decoder(INFO_BY_DECODER.apply(decoder)));
                }
            }
        });
    }

    @Override
    public void uninject(Player player) {
        Channel channel = channel(player);
        ChannelPipeline pipeline = channel.pipeline();
        for (Map.Entry<String, ChannelHandler> entry : pipeline) {
            if (entry.getValue() instanceof PlayerHandler.Encoder encoder) {
                pipeline.replace(entry.getKey(), entry.getKey(), new PacketEncoder<>(encoder.info));
            }
            if (entry.getValue() instanceof PlayerHandler.Decoder decoder) {
                pipeline.replace(entry.getKey(), entry.getKey(), new PacketDecoder<>(decoder.info));
            }
        }
    }

    private static Connection connection(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection;
    }
    private static Channel channel(Player player) {
        return connection(player).channel;
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

    private record PlayerHandler(Player player) {

        private static boolean hasComponent(@NotNull Class<?> clazz) {
            return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> net.minecraft.network.chat.Component.class.isAssignableFrom(f.getType()))
                    || Arrays.stream(clazz.getDeclaredClasses()).anyMatch(PlayerHandler::hasComponent);
        }

        private class Encoder extends PacketEncoder<PacketListener> {
            private final ProtocolInfo<PacketListener> info;
            private Encoder(ProtocolInfo<PacketListener> info) {
                super(info);
                this.info = info;
            }
            @Override
            protected void encode(@NotNull ChannelHandlerContext channelHandlerContext, @NotNull Packet<PacketListener> packet, @NotNull ByteBuf byteBuf) throws Exception {
                if (hasComponent(packet.getClass())) {
                    try {
                        GlyphBuffer newBuf = new GlyphBuffer(Unpooled.buffer());
                        info.codec().encode(newBuf, packet);
                        byteBuf.writeBytes(writeNewNbt(newBuf, true));
                        ProtocolSwapHandler.handleOutboundTerminalPacket(channelHandlerContext, packet);
                    } catch (Exception ignored) {
                    }
                    return;
                }
                super.encode(channelHandlerContext, packet, byteBuf);
            }
        }
        private class Decoder extends PacketDecoder<PacketListener> {
            private final ProtocolInfo<PacketListener> info;
            private Decoder(ProtocolInfo<PacketListener> info) {
                super(info);
                this.info = info;
            }
            @Override
            protected void decode(@NotNull ChannelHandlerContext channelHandlerContext, @NotNull ByteBuf byteBuf, @NotNull List<Object> list) throws Exception {
                try {
                    Packet<?> packet = info.codec().decode(writeNewNbt(new GlyphBuffer(byteBuf), false));
                    list.add(packet);
                    ProtocolSwapHandler.handleInboundTerminalPacket(channelHandlerContext, packet);
                } catch (Exception e) {
                    super.decode(channelHandlerContext, byteBuf, list);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private Decoder decoder(ProtocolInfo<?> info) {
            return new Decoder((ProtocolInfo<PacketListener>) info);
        }
        @SuppressWarnings("unchecked")
        private Encoder encoder(ProtocolInfo<?> info) {
            return new Encoder((ProtocolInfo<PacketListener>) info);
        }

        private GlyphBuffer writeNewNbt(GlyphBuffer original, boolean clientbound) {
            GlyphBuffer newBuffer = new GlyphBuffer(Unpooled.buffer());
            int id = VarInt.read(original);
            VarInt.write(newBuffer, id);

            byte[] bytes = new byte[original.readableBytes()];
            original.readBytes(bytes);
            List<Byte> list = new ArrayList<>(bytes.length);
            int index = 0;
            try {
                while (index < bytes.length) {
                    byte aByte = bytes[index++];
                    list.add(aByte);
                    if (TagTypes.getType(aByte) == CompoundTag.TYPE) {
                        int size = bytes.length - index;
                        byte[] nbtByte = new byte[size];
                        System.arraycopy(bytes, index, nbtByte, 0, nbtByte.length);
                        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(nbtByte))) {
                            CompoundTag getTag = CompoundTag.TYPE.load(input, NbtAccounter.create(FriendlyByteBuf.DEFAULT_NBT_QUOTA));
                            if (getTag.isEmpty()) continue;
                            Component oldComponent = PaperAdventure.asAdventure(ComponentSerialization.CODEC.decode(NbtOps.INSTANCE, getTag).getOrThrow().getFirst());
                            Component newComponent = GlyphHandlers.transform(
                                    oldComponent,
                                    clientbound ? null : player,
                                    false
                            );
                            CompoundTag newTag = (CompoundTag) ComponentSerialization.CODEC.encode(PaperAdventure.asVanilla(newComponent), NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            try (DataOutputStream output = new DataOutputStream(stream)) {
                                newTag.write(output);
                                index += nbtByte.length - input.available();
                                for (byte b : stream.toByteArray()) {
                                    list.add(b);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                byte[] newByte = new byte[list.size()];
                int i = 0;
                for (Byte b : list) {
                    newByte[i++] = b;
                }
                newBuffer.writeBytes(newByte);
                return newBuffer;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
