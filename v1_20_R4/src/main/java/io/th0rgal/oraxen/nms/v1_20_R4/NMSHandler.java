 package io.th0rgal.oraxen.nms.v1_20_R4;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
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

 @SuppressWarnings("unused")
public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    FurniturePacketManager furniturePacketManager = new FurniturePacketManager();


    private static final Map<PacketType<?>, ConnectionCodec> CODEC_MAP = new HashMap<>();
    private static final Map<Integer, List<ConnectionCodec>> INT_MAP = new HashMap<>();

    private record ConnectionCodec(StreamCodec<FriendlyByteBuf, Packet<?>> codec, ProtocolInfo<?> protocol, int id) {
    }

    private record NamedPacketType(String parent, String name, PacketType<?> type) {

    }
    private record NamedClass(String name, Class<?> aClass) {
    }
    private static class PacketList {
        private final List<NamedPacketType> clientbounds = new ArrayList<>();
        private final List<NamedPacketType> serverbounds = new ArrayList<>();
        private final Map<PacketType<?>, String> typeStringMap = new HashMap<>();
        private PacketList(NamedClass... classes) {
            for (NamedClass aClass : classes) {
                for (Field field : aClass.aClass.getFields()) {
                    try {
                        PacketType<?> type = (PacketType<?>) field.get(null);
                        typeStringMap.put(type, field.getName());
                        switch (type.flow()) {
                            case CLIENTBOUND -> clientbounds.add(new NamedPacketType(aClass.name, field.getName(), type));
                            case SERVERBOUND -> serverbounds.add(new NamedPacketType(aClass.name, field.getName(), type));
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
                new NamedClass("common", CommonPacketTypes.class),
                new NamedClass("configuration", ConfigurationPacketTypes.class),
                new NamedClass("cookie", CookiePacketTypes.class),
                new NamedClass("game", GamePacketTypes.class),
                new NamedClass("handshake", HandshakePacketTypes.class),
                new NamedClass("login", LoginPacketTypes.class),
                new NamedClass("ping", PingPacketTypes.class),
                new NamedClass("status", StatusPacketTypes.class)
        );

        for (PacketProtocol packetProtocol : List.of(
                new PacketProtocol(GameProtocols.CLIENTBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY)), GameProtocols.SERVERBOUND.bind(RegistryFriendlyByteBuf.decorator(RegistryAccess.EMPTY))),
                new PacketProtocol(StatusProtocols.CLIENTBOUND, StatusProtocols.SERVERBOUND),
                new PacketProtocol(LoginProtocols.CLIENTBOUND, LoginProtocols.SERVERBOUND),
                new PacketProtocol(ConfigurationProtocols.CLIENTBOUND, ConfigurationProtocols.SERVERBOUND),
                new PacketProtocol(null, HandshakeProtocols.SERVERBOUND)
        )) {
            for (NamedPacketType clientbound : list.clientbounds) {
                if (packetProtocol.clientboundInfo == null) break;
                IdDispatchCodec<ByteBuf, ?, ?> codec = (IdDispatchCodec<ByteBuf, ? ,?>) packetProtocol.clientboundInfo.codec();
                try {
                    Object2IntMap<?> object2IntMap = (Object2IntMap<?>) getToId.get(codec);
                    List<?> objects = (List<?>) getById.get(codec);
                    for (Object object : objects) {
                        if (getType.get(object).equals(clientbound.type)) {
                            ConnectionCodec codec1 = new ConnectionCodec(searchPacketClass(clientbound.parent, true, clientbound.name), packetProtocol.clientboundInfo, object2IntMap.getInt(clientbound));
                            CODEC_MAP.put(clientbound.type, codec1);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (NamedPacketType serverbound : list.serverbounds) {
                if (packetProtocol.serverboundInfo == null) break;
                IdDispatchCodec<ByteBuf, ?, ?> codec = (IdDispatchCodec<ByteBuf, ? ,?>) packetProtocol.serverboundInfo.codec();
                try {
                    Object2IntMap<?> object2IntMap = (Object2IntMap<?>) getToId.get(codec);
                    List<?> objects = (List<?>) getById.get(codec);
                    for (Object object : objects) {
                        if (getType.get(object).equals(serverbound.type)) {
                            StringBuilder classNameBuilder = new StringBuilder();
                            ConnectionCodec codec1 = new ConnectionCodec(searchPacketClass(serverbound.parent, false, serverbound.name), packetProtocol.serverboundInfo, object2IntMap.getInt(serverbound));
                            CODEC_MAP.put(serverbound.type, codec1);
                            INT_MAP.computeIfAbsent(codec1.id, t -> new ArrayList<>()).add(codec1);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    private static StreamCodec<FriendlyByteBuf, Packet<?>> searchPacketClass(String parent, boolean isClientbound, String name) {
        StringBuilder sb = new StringBuilder();
        Class<?> clazz;
        int i = 0;
        String[] split = name.split("_");
        for (String s : split) {
            i++;
            sb.append(capitalize(s));
            clazz = findClass(parent, isClientbound, sb.toString());
            if (clazz != null) {
                if (i < split.length) {
                    for (int t = i; t < split.length; t++) {
                        for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
                            if (declaredClass.getName().equals(split[t])) {
                                clazz = declaredClass;
                                break;
                            }
                        }
                    }
                }
                return findCodec(clazz);
            }
        }
        throw new RuntimeException();
    }


    private static StreamCodec<FriendlyByteBuf, Packet<?>> findCodec(Class<?> clazz) {
        try {
            return (StreamCodec<FriendlyByteBuf, Packet<?>>) Arrays.stream(clazz.getFields()).filter(f -> StreamCodec.class.isAssignableFrom(f.getType())).findFirst().map(f -> {
                try {
                    return f.get(null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).orElse(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static Class<?> findClass(String parent, boolean isClientbound, String name) {
        Class<?> clazz;
        String bound = isClientbound ? "Clientbound" : "Serverbound";
        String loc = "net.minecraft.network.protocol." + parent + ".";
        clazz = findClass(loc + bound + name + "Packet");
        if (clazz == null) clazz = findClass(loc  + name + "Packet");
        if (clazz == null) clazz = findClass(loc + bound + name);
        if (clazz == null) clazz = findClass(loc + name);
        return clazz;
    }
    private static Class<?> findClass(String clazz) {
        try {
            return Class.forName(clazz);
        } catch (Exception e) {
            return null;
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
            Field channelFutureField = ServerConnectionListener.class.getDeclaredField("channels");
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
        CustomPacketHandler handler = new CustomPacketHandler(player);
        // Replace the vanilla PacketEncoder with our own
        if (!(channel.pipeline().get("encoder") instanceof CustomPacketHandler.CustomPacketEncoder))
            encoder.putIfAbsent(channel, channel.pipeline().replace("encoder", "encoder", handler.encoder));

        // Replace the vanilla PacketDecoder with our own
        if (!(channel.pipeline().get("decoder") instanceof CustomPacketHandler.CustomPacketDecoder))
            decoder.putIfAbsent(channel, channel.pipeline().replace("decoder", "decoder", handler.decoder));
    }

    private void bind(List<ChannelFuture> channelFutures, ChannelInboundHandlerAdapter serverChannelHandler) {
        for (ChannelFuture future : channelFutures) {
            future.channel().pipeline().addFirst(serverChannelHandler);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            inject(player);
        }
    }

    private static class CustomDataSerializer extends RegistryFriendlyByteBuf {
        @Nullable private final Player player;

        public CustomDataSerializer(@Nullable Player player, ByteBuf bytebuf) {
            super(bytebuf, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
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

    private static class CustomPacketHandler {
        @Nullable private final Player player;

        private volatile ConnectionCodec encodeCodec;

        private final CustomPacketEncoder encoder = new CustomPacketEncoder();
        private final CustomPacketDecoder decoder = new CustomPacketDecoder();

        private CustomPacketHandler(@Nullable Player player) {
            this.player = player;
        }

        private class CustomPacketEncoder extends MessageToByteEncoder<Packet<?>> {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (player != null) {
                    ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                    for (PacketListener value : LISTENER_MAP.values()) {
                        if (!value.listen(serverPlayer, msg)) return;
                    }
                }
                synchronized (CustomPacketHandler.this) {
                    if (msg instanceof Packet<?> packet) {
                        ConnectionCodec codec = CODEC_MAP.get(packet.type());
                        if (codec != null) encodeCodec = codec;
                    }
                    super.write(ctx, msg, promise);
                }
            }

            @Override
            public void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf byteBuf) {
                if (ctx.channel() == null) throw new RuntimeException("Channel is null");

                ConnectionCodec codecData = encodeCodec;
                int packetId = codecData.id;
                if (packetId < 0) return;

                FriendlyByteBuf packetDataSerializer = new CustomDataSerializer(player, byteBuf);
                packetDataSerializer.writeVarInt(packetId);

                try {
                    int integer2 = packetDataSerializer.writerIndex();
                    codecData.codec.encode(packetDataSerializer, packet);
                    int integer3 = packetDataSerializer.writerIndex() - integer2;
                    if (integer3 > 8388608) {
                        throw new IllegalArgumentException("Packet too big (is " + integer3 + ", should be less than 8388608): " + packet);
                    }
                } catch (Exception e) {
                    if (packet.isSkippable()) throw new SkipPacketException(e);
                    throw e;
                }
            }
        }

        private class CustomPacketDecoder extends ByteToMessageDecoder {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (player != null) {
                    ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
                    for (PacketListener value : LISTENER_MAP.values()) {
                        if (!value.listen(serverPlayer, msg)) return;
                    }
                }
                super.channelRead(ctx, msg);
            }

            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws IOException {
                final ByteBuf bufferCopy = buffer.copy();
                if (buffer.copy().readableBytes() == 0) return;

                CustomDataSerializer dataSerializer = new CustomDataSerializer(player, buffer.copy());
                int packetID = dataSerializer.readVarInt();

                List<ConnectionCodec> codecs = INT_MAP.get(packetID);
                if (codecs == null) return;
                for (ConnectionCodec codec : codecs) {
                    try {
                        Packet<?> packet = codec.codec.decode(dataSerializer);

                        if (packet instanceof ServerboundChatPacket) {
                            FriendlyByteBuf baseSerializer = new FriendlyByteBuf(bufferCopy);
                            int basePacketID = baseSerializer.readVarInt();
                            packet = codec.codec.decode(baseSerializer);
                        }

                        out.add(packet);
                    } catch (Exception ignored) {}
                }
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
