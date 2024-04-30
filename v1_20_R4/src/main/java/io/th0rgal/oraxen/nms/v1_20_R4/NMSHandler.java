 package io.th0rgal.oraxen.nms.v1_20_R4;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.configuration.GlobalConfiguration;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.IFurniturePacketManager;
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.nms.PacketListenerType;
import io.th0rgal.oraxen.nms.v1_20_R4.furniture.FurniturePacketManager;
import io.th0rgal.oraxen.nms.v1_20_R4.protocol.EfficiencyPacketListener;
import io.th0rgal.oraxen.nms.v1_20_R4.protocol.InventoryPacketListener;
import io.th0rgal.oraxen.nms.v1_20_R4.protocol.PacketListener;
import io.th0rgal.oraxen.nms.v1_20_R4.protocol.TitlePacketListener;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.nbt.Tag;
import net.minecraft.network.*;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.IdDispatchCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationPacketTypes;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.handshake.HandshakePacketTypes;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginPacketTypes;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import net.minecraft.network.protocol.status.StatusPacketTypes;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    FurniturePacketManager furniturePacketManager = new FurniturePacketManager();


    private static final Map<PacketType<?>, ConnectionCodec> CODEC_MAP = new HashMap<>();
    private static final AttributeKey<ConnectionCodec> CODEC_ATTRIBUTE_KEY = AttributeKey.newInstance("oraxen.attribute.codec.key");

    private record ConnectionCodec(StreamCodec<ByteBuf, Packet<?>> codec, ProtocolInfo<?> protocol, int id) {
    }

    private static class PacketList {
        private final List<PacketType<?>> clientbounds = new ArrayList<>();
        private final List<PacketType<?>> serverbounds = new ArrayList<>();
        private PacketList(Class<?>... classes) {
            for (Class<?> aClass : classes) {
                for (Field field : aClass.getFields()) {
                    try {
                        PacketType<?> type = (PacketType<?>) field.get(null);
                        switch (type.flow()) {
                            case CLIENTBOUND -> clientbounds.add(type);
                            case SERVERBOUND -> serverbounds.add(type);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
    private static class PacketProtocol {
        private final ProtocolInfo<?> clientboundInfo;
        private final ProtocolInfo<?> serverboundInfo;

        public PacketProtocol(ProtocolInfo<?> clientbound, ProtocolInfo<?> serverbound) {
            clientboundInfo = clientbound;
            serverboundInfo = serverbound;
        }
    }

    static {
        Field getToId = Arrays.stream(IdDispatchCodec.class.getDeclaredFields()).filter(f -> Object2IntMap.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
        Field getById = Arrays.stream(IdDispatchCodec.class.getDeclaredFields()).filter(f -> List.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();
        Class<?> entryClass = IdDispatchCodec.class.getDeclaredClasses()[0];
        Field getCodec = entryClass.getDeclaredFields()[0];
        Field getType = entryClass.getDeclaredFields()[1];

        getToId.setAccessible(true);
        getById.setAccessible(true);
        getCodec.setAccessible(true);
        getType.setAccessible(true);

        PacketList list = new PacketList(
                CommonPacketTypes.class,
                ConfigurationPacketTypes.class,
                CookiePacketTypes.class,
                GamePacketTypes.class,
                HandshakePacketTypes.class,
                LoginPacketTypes.class,
                PingPacketTypes.class,
                StatusPacketTypes.class
        );

        for (PacketProtocol packetProtocol : List.of(
                new PacketProtocol(GameProtocols.CLIENTBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY)), GameProtocols.SERVERBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))),
                new PacketProtocol(StatusProtocols.CLIENTBOUND, StatusProtocols.SERVERBOUND),
                new PacketProtocol(LoginProtocols.CLIENTBOUND, LoginProtocols.SERVERBOUND),
                new PacketProtocol(ConfigurationProtocols.CLIENTBOUND, ConfigurationProtocols.SERVERBOUND),
                new PacketProtocol(null, HandshakeProtocols.SERVERBOUND)
        )) {
            for (PacketType<?> clientbound : list.clientbounds) {
                if (packetProtocol.clientboundInfo == null) break;
                IdDispatchCodec<ByteBuf, ?, ?> codec = (IdDispatchCodec<ByteBuf, ? ,?>) packetProtocol.clientboundInfo.codec();
                try {
                    Object2IntMap<?> object2IntMap = (Object2IntMap<?>) getToId.get(codec);
                    List<?> objects = (List<?>) getById.get(codec);
                    for (Object object : objects) {
                        if (getType.get(object).equals(clientbound)) {
                            StreamCodec<ByteBuf, Packet<?>> packetCodec = (StreamCodec<ByteBuf, Packet<?>>) getCodec.get(object);
                            StreamCodec<ByteBuf, Packet<?>> finalCodec = Arrays.stream(packetCodec.decode(new DummyByteBuf()).getClass().getFields()).filter(f -> StreamCodec.class.isAssignableFrom(f.getType())).findFirst().map(f -> {
                                try {
                                    return (StreamCodec<ByteBuf, Packet<?>>) f.get(null);
                                } catch (Exception e) {
                                    return null;
                                }
                            }).orElseThrow();
                            CODEC_MAP.put(clientbound, new ConnectionCodec(finalCodec, packetProtocol.clientboundInfo, object2IntMap.getInt(clientbound)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (PacketType<?> serverbound : list.serverbounds) {
                if (packetProtocol.serverboundInfo == null) break;
                IdDispatchCodec<ByteBuf, ?, ?> codec = (IdDispatchCodec<ByteBuf, ? ,?>) packetProtocol.serverboundInfo.codec();
                try {
                    Object2IntMap<?> object2IntMap = (Object2IntMap<?>) getToId.get(codec);
                    List<?> objects = (List<?>) getById.get(codec);
                    for (Object object : objects) {
                        if (getType.get(object).equals(serverbound)) {
                            StreamCodec<ByteBuf, Packet<?>> packetCodec = (StreamCodec<ByteBuf, Packet<?>>) getCodec.get(object);
                            StreamCodec<ByteBuf, Packet<?>> finalCodec = Arrays.stream(packetCodec.decode(new DummyByteBuf()).getClass().getFields()).filter(f -> StreamCodec.class.isAssignableFrom(f.getType())).findFirst().map(f -> {
                                try {
                                    return (StreamCodec<ByteBuf, Packet<?>>) f.get(null);
                                } catch (Exception e) {
                                    return null;
                                }
                            }).orElseThrow();
                            CODEC_MAP.put(serverbound, new ConnectionCodec(finalCodec, packetProtocol.serverboundInfo, object2IntMap.getInt(serverbound)));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public IFurniturePacketManager furniturePacketManager() {
        return furniturePacketManager;
    }

    @Override
    public boolean tripwireUpdatesDisabled() {
        return VersionUtil.isPaperServer() && GlobalConfiguration.get().blockUpdates.disableTripwireUpdates;
    }

    @Override
    public boolean noteblockUpdatesDisabled() {
        return VersionUtil.isPaperServer() && GlobalConfiguration.get().blockUpdates.disableNoteblockUpdates;
    }


    @Override
    public ItemStack copyItemNBTTags(@NotNull ItemStack oldItem, @NotNull ItemStack newItem) {
        net.minecraft.world.item.ItemStack oldNmsItem = CraftItemStack.asNMSCopy(oldItem);
        net.minecraft.world.item.ItemStack newNmsItem = CraftItemStack.asNMSCopy(newItem);
        BuiltInRegistries.DATA_COMPONENT_TYPE.forEach(t -> newNmsItem.set((DataComponentType<Object>) t, oldNmsItem.get(t)));
        return CraftItemStack.asBukkitCopy(newNmsItem);
    }

    @Override
    @Nullable
    public InteractionResult correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        InteractionHand hand = slot == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        BlockHitResult hitResult = getPlayerPOVHitResult(serverPlayer.level(), serverPlayer, ClipContext.Fluid.NONE);
        BlockPlaceContext placeContext = new BlockPlaceContext(new UseOnContext(serverPlayer, hand, hitResult));

        if (!(nmsStack.getItem() instanceof BlockItem blockItem)) {
            InteractionResult result = nmsStack.getItem().useOn(new UseOnContext(serverPlayer, hand, hitResult));
            return player.isSneaking() && player.getGameMode() != GameMode.CREATIVE ? result : serverPlayer.gameMode.useItem(
                    serverPlayer, serverPlayer.level(), nmsStack, hand
            );
        }

        InteractionResult result = blockItem.place(placeContext);
        if (result == InteractionResult.FAIL) return null;

        if(!player.isSneaking()) {
            World world = player.getWorld();
            BlockPos clickPos = placeContext.getClickedPos();
            Block block = world.getBlockAt(clickPos.getX(), clickPos.getY(), clickPos.getZ());
            SoundGroup sound = block.getBlockData().getSoundGroup();

            world.playSound(
                    BlockHelpers.toCenterBlockLocation(block.getLocation()), sound.getPlaceSound(),
                    SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F
            );
        }

        return result;
    }

    @Override
    public BlockHitResult getPlayerPOVHitResult(Level world, net.minecraft.world.entity.player.Player player, ClipContext.Fluid fluidHandling) {
        float f = player.getXRot();
        float g = player.getYRot();
        Vec3 vec3 = player.getEyePosition();
        float h = Mth.cos(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float i = Mth.sin(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float j = -Mth.cos(-f * ((float)Math.PI / 180F));
        float k = Mth.sin(-f * ((float)Math.PI / 180F));
        float l = i * j;
        float n = h * j;
        double d = 5.0D;
        Vec3 vec32 = vec3.add((double)l * d, (double)k * d, (double)n * d);
        return world.clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, fluidHandling, player));
    }

    @Override
    public void customBlockDefaultTools(Player player) {
        if (player instanceof CraftPlayer craftPlayer) {
            TagNetworkSerialization.NetworkPayload payload = createPayload();
            if (payload == null) return;
            ClientboundUpdateTagsPacket packet = new ClientboundUpdateTagsPacket(Map.of(Registries.BLOCK, payload));
            craftPlayer.getHandle().connection.send(packet);
        }
    }

    private TagNetworkSerialization.NetworkPayload createPayload() {
        Constructor<?> constructor = Arrays.stream(TagNetworkSerialization.NetworkPayload.class.getDeclaredConstructors()).findFirst().orElse(null);
        if (constructor == null) return null;
        constructor.setAccessible(true);
        try {
            return (TagNetworkSerialization.NetworkPayload) constructor.newInstance(tagRegistryMap);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private final Map<ResourceLocation, IntList> tagRegistryMap = createTagRegistryMap();

    private static Map<ResourceLocation, IntList> createTagRegistryMap() {
        return BuiltInRegistries.BLOCK.getTags().map(pair -> {
            IntArrayList list = new IntArrayList(pair.getSecond().size());
            if (pair.getFirst().location() == BlockTags.MINEABLE_WITH_AXE.location()) {
                pair.getSecond().stream()
                        .filter(block -> !block.value().getDescriptionId().endsWith("note_block"))
                        .forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));
            } else pair.getSecond().forEach(block -> list.add(BuiltInRegistries.BLOCK.getId(block.value())));

            return Map.of(pair.getFirst().location(), list);
        }).collect(HashMap::new, Map::putAll, Map::putAll);
    }


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
                        protected void initChannel(@NotNull Channel ch) throws Exception {
                            initChannel.invoke(initializer, ch);
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

    private void uninject(Channel channel) {
        if (encoder.containsKey(channel)) {
            // Replace our custom packet encoder with the default one that the player had
            ChannelHandler previousHandler = encoder.remove(channel);
            if (previousHandler instanceof PacketEncoder) {
                // PacketEncoder is not shareable, so we can't re-add it back. Instead, we'll have to create a new instance
                channel.pipeline().replace("encoder", "encoder", new PacketEncoder<>(GameProtocols.CLIENTBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))));
            } else channel.pipeline().replace("encoder", "encoder", previousHandler);
        }

        if (decoder.containsKey(channel)) {
            ChannelHandler previousHandler = decoder.remove(channel);
            if (previousHandler instanceof PacketDecoder) {
                channel.pipeline().replace("decoder", "decoder", new PacketDecoder<>(GameProtocols.SERVERBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))));
            } else {
                channel.pipeline().replace("decoder", "decoder", previousHandler);
            }
        }
    }

    private void inject(Channel channel, @Nullable Player player) {
        // Replace the vanilla PacketEncoder with our own
        if (!(channel.pipeline().get("encoder") instanceof CustomPacketEncoder))
            encoder.putIfAbsent(channel, channel.pipeline().replace("encoder", "encoder", new CustomPacketEncoder(player)));

        // Replace the vanilla PacketDecoder with our own
        if (!(channel.pipeline().get("decoder") instanceof CustomPacketDecoder))
            decoder.putIfAbsent(channel, channel.pipeline().replace("decoder", "decoder", new CustomPacketDecoder(player)));
    }

    private void bind(List<ChannelFuture> channelFutures, ChannelInboundHandlerAdapter serverChannelHandler) {
        for (ChannelFuture future : channelFutures) {
            future.channel().pipeline().addFirst(serverChannelHandler);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    private static class CustomDataSerializer extends FriendlyByteBuf {
        @Nullable private final Player player;

        public CustomDataSerializer(@Nullable Player player, ByteBuf bytebuf) {
            super(bytebuf);
            this.player = player;
        }

        @NotNull
        public FriendlyByteBuf writeComponent(@NotNull Component component) {
            writeNbt(ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, PaperAdventure.asVanilla(component)).getOrThrow());
            return this;
        }

        @NotNull
        public net.minecraft.network.chat.Component readComponent() {
            return PaperAdventure.asVanilla((GlyphHandlers.transform(PaperAdventure.asAdventure(ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, readNbt(NbtAccounter.unlimitedHeap())).getOrThrow()), player, false)));
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
        public FriendlyByteBuf writeNbt(@Nullable Tag tag) {
            if (tag instanceof CompoundTag compoundTag) transform(compoundTag, GlyphHandlers.transformer(null));
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
        @Nullable private final Player player;

        private CustomPacketEncoder(@Nullable Player player) {
            super();
            this.player = player;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (player != null) {
                ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                for (PacketListener value : LISTENER_MAP.values()) {
                    if (!value.listen(serverPlayer, msg)) return;
                }
            }
            if (msg instanceof Packet<?> packet) {
                ConnectionCodec codec = CODEC_MAP.get(packet.type());
                if (codec != null) ctx.channel().attr(CODEC_ATTRIBUTE_KEY).set(codec);
            }
            super.write(ctx, msg, promise);
        }

        @Override
        public void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf byteBuf) {
            if (ctx.channel() == null) throw new RuntimeException("Channel is null");

            Attribute<ConnectionCodec> attribute = ctx.channel().attr(CODEC_ATTRIBUTE_KEY);
            ConnectionCodec codecData = attribute.get();
            int packetId = codecData.id;

            FriendlyByteBuf packetDataSerializer = new CustomDataSerializer(player, byteBuf);
            packetDataSerializer.writeVarInt(packetId);

            try {
                int integer2 = packetDataSerializer.writerIndex();
                codecData.codec.encode(packetDataSerializer, packet);
                int integer3 = packetDataSerializer.writerIndex() - integer2;
                if (integer3 > 8388608) {
                    throw new IllegalArgumentException("Packet too big (is " + integer3 + ", should be less than 8388608): " + packet);
                }
                swapProtocolIfNeeded(attribute, packet);
            } catch (Exception e) {
                if (packet.isSkippable()) throw new SkipPacketException(e);
                throw e;
            }
            swapProtocolIfNeeded(attribute, packet);
        }
    }

    private static class CustomPacketDecoder extends ByteToMessageDecoder {
        @Nullable private final Player player;

        private CustomPacketDecoder(@Nullable Player player) {
            this.player = player;
        }


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (player != null) {
                ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                for (PacketListener value : LISTENER_MAP.values()) {
                    if (!value.listen(serverPlayer, msg)) return;
                }
            }
            if (msg instanceof Packet<?> packet) {
                ConnectionCodec codec = CODEC_MAP.get(packet.type());
                if (codec != null) ctx.channel().attr(CODEC_ATTRIBUTE_KEY).set(codec);
            }
            super.channelRead(ctx, msg);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws IOException {
            final ByteBuf bufferCopy = buffer.copy();
            if (buffer.readableBytes() == 0) return;

            CustomDataSerializer dataSerializer = new CustomDataSerializer(player, buffer);
            int packetID = dataSerializer.readVarInt();
            Attribute<ConnectionCodec> attribute = ctx.channel().attr(CODEC_ATTRIBUTE_KEY);
            Packet<?> packet = attribute.get().codec().decode(dataSerializer);

            if (dataSerializer.readableBytes() > 0) {
                throw new IOException("Packet " + packetID + " " + packet + " was larger than expected, found " + dataSerializer.readableBytes() + " bytes extra whiløst reading the packet " + packetID);
            } else if (packet instanceof ServerboundChatPacket) {
                FriendlyByteBuf baseSerializer = new FriendlyByteBuf(bufferCopy);
                int basePacketID = baseSerializer.readVarInt();
                packet = attribute.get().codec.decode(baseSerializer);
            }

            out.add(packet);
            swapProtocolIfNeeded(attribute, packet);
        }
    }
    private static void swapProtocolIfNeeded(Attribute<ConnectionCodec> protocolAttribute, Packet<?> packet) {
        ConnectionCodec codec = CODEC_MAP.get(packet.type());
        ProtocolInfo<?> connectionProtocol = codec.protocol;
        if (connectionProtocol != null) {
            ConnectionCodec codecData = protocolAttribute.get();
            ProtocolInfo<?> connectionProtocol2 = codecData.protocol;
            if (connectionProtocol.id() != connectionProtocol2.id()) {
                StreamCodec<ByteBuf, Packet<?>> codecData2 = (StreamCodec<ByteBuf, Packet<?>>) connectionProtocol.codec();
                protocolAttribute.set(new ConnectionCodec(codecData2, connectionProtocol, codec.id));
            }
        }
    }

    @Override
    public boolean getSupported() {
        return true;
    }

    @Override
    public boolean isTool(@NotNull ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack).has(DataComponents.TOOL);
    }

    @Override
    public boolean isTool(@NotNull Material material) {
        return isTool(new ItemStack(material));
    }

    @Override
    public void applyMiningFatigue(@NotNull Player player) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        handle.connection.send(new ClientboundUpdateMobEffectPacket(handle.getId(), new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                -1,
                -1,
                false,
                false,
                false
        ), false));
    }
    @Override
    public void removeMiningFatigue(@NotNull Player player) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        handle.connection.send(new ClientboundRemoveMobEffectPacket(handle.getId(), MobEffects.DIG_SLOWDOWN));
    }
    private static final Map<PacketListenerType, PacketListener> LISTENER_MAP = new EnumMap<>(PacketListenerType.class);

    @Override
    public void addPacketListener(@NotNull PacketListenerType type) {
        LISTENER_MAP.computeIfAbsent(type, t -> switch (type) {
            case TITLE -> TitlePacketListener.INSTANCE;
            case INVENTORY -> InventoryPacketListener.INSTANCE;
            case EFFICIENCY -> EfficiencyPacketListener.INSTANCE;
        });
    }

    @Override
    public void removePacketListener(@NotNull PacketListenerType type) {
        LISTENER_MAP.remove(type);
    }
}