package io.th0rgal.oraxen.nms.v1_20_R1;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class GlyphHandler implements io.th0rgal.oraxen.nms.GlyphHandler {

    private final Map<Channel, ChannelHandler> encoder = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Channel, ChannelHandler> decoder = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void setupNmsGlyphs() {
        if (!GlyphHandlers.isNms()) return;
        List<Connection> networkManagers = MinecraftServer.getServer().getConnection().getConnections();
        List<ChannelFuture> channelFutures;

        try {
            Field channelFutureField = ServerConnectionListener.class.getDeclaredField("f");
            channelFutureField.setAccessible(true);

            channelFutures = (List<ChannelFuture>) channelFutureField.get(MinecraftServer.getServer().getConnection());
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
            channelFutures = new ArrayList<>();
            e1.printStackTrace();
        }

        final List<ChannelFuture> futures = channelFutures;

        // Handle connected channels
        ChannelInitializer<Channel> endInitProtocol = new ChannelInitializer<>() {
            @Override
            protected void initChannel(@NotNull Channel channel) {
                try {
                    // This can take a while, so we need to stop the main thread from interfering
                    synchronized (networkManagers) {
                        // Stop injecting channels
                        channel.eventLoop().submit(() -> inject(channel, null));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        // This is executed before Minecraft's channel handler
        ChannelInitializer<Channel> beginInitProtocol = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelHandler handler = null;
                for (Map.Entry<String, ChannelHandler> entry : channel.pipeline()) {
                    if (entry.getValue().getClass().getName().equals("com.viaversion.viaversion.bukkit.handlers.BukkitChannelInitializer")) {
                        handler = entry.getValue();
                    }
                }

                if (handler == null) {
                    channel.pipeline().addLast(endInitProtocol);
                } else {
                    Class<?> clazz = handler.getClass();
                    Method initChannel = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
                    initChannel.setAccessible(true);
                    Field original = clazz.getDeclaredField("original");
                    original.setAccessible(true);
                    ChannelInitializer<Channel> initializer = (ChannelInitializer<Channel>) original.get(handler);
                    ChannelInitializer<Channel> miniInit = new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(@NotNull Channel channel) throws Exception {
                            initChannel.invoke(initializer, channel);
                            channel.eventLoop().submit(() -> inject(channel, null));
                        }
                    };
                    original.set(handler, miniInit);
                }
            }
        };

        ChannelInboundHandlerAdapter serverChannelHandler = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, @NotNull Object msg) {
                // Prepare to initialize ths channel
                ((Channel) msg).pipeline().addFirst(beginInitProtocol);
                ctx.fireChannelRead(msg);
            }
        };

        try {
            bind(futures, serverChannelHandler);
        } catch (IllegalArgumentException ex) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    bind(futures, serverChannelHandler);
                }
            }.runTask(OraxenPlugin.get());
        }

        if (VersionUtil.isPaperServer())
            Bukkit.getPluginManager().registerEvents(new GlyphListener(), OraxenPlugin.get());
    }

    @Override
    public void inject(Player player) {
        if (player == null || !GlyphHandlers.isNms()) return;
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;

        channel.eventLoop().submit(() -> inject(channel, player));
    }

    @Override
    public void uninject(Player player) {
        if (player == null || !GlyphHandlers.isNms()) return;
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;

        uninject(channel);
    }

    private void inject(Channel channel, @javax.annotation.Nullable Player player) {
        // Replace the vanilla PacketEncoder with our own
        if (!(channel.pipeline().get("encoder") instanceof CustomPacketEncoder))
            encoder.putIfAbsent(channel, channel.pipeline().replace("encoder", "encoder", new CustomPacketEncoder(player)));

        // Replace the vanilla PacketDecoder with our own
        if (!(channel.pipeline().get("decoder") instanceof CustomPacketDecoder))
            decoder.putIfAbsent(channel, channel.pipeline().replace("decoder", "decoder", new CustomPacketDecoder(player)));
    }

    private void uninject(Channel channel) {
        if (encoder.containsKey(channel)) {
            // Replace our custom packet encoder with the default one that the player had
            ChannelHandler previousHandler = encoder.remove(channel);
            if (previousHandler instanceof PacketEncoder) {
                // PacketEncoder is not shareable, so we can't re-add it back. Instead, we'll have to create a new instance
                channel.pipeline().replace("encoder", "encoder", new PacketEncoder(PacketFlow.CLIENTBOUND));
            } else channel.pipeline().replace("encoder", "encoder", previousHandler);
        }

        if (decoder.containsKey(channel)) {
            ChannelHandler previousHandler = decoder.remove(channel);
            if (previousHandler instanceof PacketDecoder) {
                channel.pipeline().replace("decoder", "decoder", new PacketDecoder(PacketFlow.SERVERBOUND));
            } else {
                channel.pipeline().replace("decoder", "decoder", previousHandler);
            }
        }
    }

    private void bind(List<ChannelFuture> channelFutures, ChannelInboundHandlerAdapter serverChannelHandler) {
        for (ChannelFuture future : channelFutures) future.channel().pipeline().addFirst(serverChannelHandler);
        for (Player player : Bukkit.getOnlinePlayers()) inject(player);
    }

    private static class CustomDataSerializer extends FriendlyByteBuf {
        @javax.annotation.Nullable
        private final Player player;

        public CustomDataSerializer(@javax.annotation.Nullable Player player, ByteBuf bytebuf) {
            super(bytebuf);
            this.player = player;
        }

        @NotNull
        @Override
        public FriendlyByteBuf writeComponent(@NotNull Component component) {
            return super.writeComponent(GlyphHandlers.transform(component, null, false));
        }

        @NotNull
        @Override
        public net.minecraft.network.chat.Component readComponent() {
            return PaperAdventure.asVanilla((GlyphHandlers.transform(PaperAdventure.asAdventure(super.readComponent()), player, false)));
        }

        @Override
        public @NotNull FriendlyByteBuf writeUtf(@NotNull String string, int maxLength) {
            try {
                JsonElement element = JsonParser.parseString(string);
                if (element.isJsonObject())
                    return super.writeUtf(GlyphHandlers.formatJsonString(element.getAsJsonObject(), null), maxLength);
            } catch (Exception ignored) {
            }

            return super.writeUtf(string, maxLength);
        }

        @Override
        public @NotNull String readUtf(int i) {
            Component component = AdventureUtils.MINI_MESSAGE_EMPTY.deserialize(super.readUtf(i));
            return AdventureUtils.MINI_MESSAGE_EMPTY.serialize(GlyphHandlers.transform(component, player, true));
        }

        @NotNull
        @Override
        public FriendlyByteBuf writeNbt(CompoundTag tag) {
            transform(tag, GlyphHandlers.transformer(null));
            return super.writeNbt(tag);
        }

        @Override
        public @Nullable CompoundTag readNbt() {
            CompoundTag compound = super.readNbt();
            if (compound != null) transform(compound, GlyphHandlers.transformer(player));

            return compound;
        }

        private void transform(CompoundTag compound, Function<String, String> transformer) {
            for (String key : compound.getAllKeys()) {
                Tag base = compound.get(key);
                if (base instanceof CompoundTag tag) transform(tag, transformer);
                else if (base instanceof ListTag listTag) transform(listTag, transformer);
                else if (base instanceof StringTag) compound.put(key, StringTag.valueOf(transformer.apply(base.getAsString())));
            }
        }

        private void transform(ListTag list, Function<String, String> transformer) {
            List<Tag> listCopy = List.copyOf(list);
            for (Tag base : listCopy) {
                if (base instanceof CompoundTag tag) transform(tag, transformer);
                else if (base instanceof ListTag listTag) transform(listTag, transformer);
                else if (base instanceof StringTag) {
                    int index = list.indexOf(base);
                    list.set(index, StringTag.valueOf(transformer.apply(base.getAsString())));
                }
            }
        }
    }

    private static class CustomPacketEncoder extends MessageToByteEncoder<Packet<?>> {
        private final PacketFlow protocolDirection = PacketFlow.CLIENTBOUND;
        @Nullable private final Player player;

        private CustomPacketEncoder(@Nullable Player player) {
            super();
            this.player = player;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Packet<?> msg, ByteBuf out) {
            ConnectionProtocol enumProt = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
            if (enumProt == null) {
                throw new RuntimeException("ConnectionProtocol unknown: " + msg);
            }
            int integer = enumProt.getPacketId(this.protocolDirection, msg);

            FriendlyByteBuf packetDataSerializer = new CustomDataSerializer(player, out);
            packetDataSerializer.writeVarInt(integer);

            try {
                int integer2 = packetDataSerializer.writerIndex();
                msg.write(packetDataSerializer);
                int integer3 = packetDataSerializer.writerIndex() - integer2;
                if (integer3 > 8388608) {
                    throw new IllegalArgumentException("Packet too big (is " + integer3 + ", should be less than 8388608): " + msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class CustomPacketDecoder extends ByteToMessageDecoder {
        @javax.annotation.Nullable
        private final Player player;

        private CustomPacketDecoder(@Nullable Player player) {
            this.player = player;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
            final ByteBuf bufCopy = msg.copy();
            if (msg.readableBytes() == 0) return;

            CustomDataSerializer dataSerializer = new CustomDataSerializer(player, msg);
            int packetID = dataSerializer.readVarInt();
            ConnectionProtocol protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
            Packet<?> packet = protocol.createPacket(PacketFlow.SERVERBOUND, packetID, dataSerializer);

            if (packet == null) {
                throw new IOException("Bad packet id " + packetID);
            }

            if (dataSerializer.readableBytes() > 0) {
                throw new IOException("Packet " + packetID + " " + packet + " was larger than expected, found " + dataSerializer.readableBytes() + " bytes extra whiløst reading the packet " + packetID);
            } else if (packet instanceof ClientboundPlayerChatPacket) {
                FriendlyByteBuf serializer = new FriendlyByteBuf(bufCopy);
                serializer.readVarInt();
                packet = protocol.createPacket(PacketFlow.SERVERBOUND, packetID, serializer);
            }
            out.add(packet);
        }
    }
}
