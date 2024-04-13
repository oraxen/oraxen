package io.th0rgal.oraxen.nms.v1_20_R3;

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
import io.th0rgal.oraxen.nms.GlyphHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.BlockHelpers;
import io.th0rgal.oraxen.utils.VersionUtil;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
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

public class NMSHandler implements io.th0rgal.oraxen.nms.NMSHandler {

    private final Map<Channel, ChannelHandler> encoder = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Channel, ChannelHandler> decoder = Collections.synchronizedMap(new WeakHashMap<>());

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
        CompoundTag oldTag = CraftItemStack.asNMSCopy(oldItem).getOrCreateTag();
        net.minecraft.world.item.ItemStack newNmsItem = CraftItemStack.asNMSCopy(newItem);
        CompoundTag newTag = newNmsItem.getOrCreateTag();
        oldTag.getAllKeys().stream().filter(key -> !vanillaKeys.contains(key)).forEach(key -> newTag.put(key, oldTag.get(key)));
        newNmsItem.setTag(newTag);
        return CraftItemStack.asBukkitCopy(newNmsItem);
    }

    @Override
    @Nullable
    public BlockData correctBlockStates(Player player, EquipmentSlot slot, ItemStack itemStack) {
        InteractionHand hand = slot == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        BlockHitResult hitResult = getPlayerPOVHitResult(serverPlayer.level(), serverPlayer, ClipContext.Fluid.NONE);
        BlockPlaceContext placeContext = new BlockPlaceContext(new UseOnContext(serverPlayer, hand, hitResult));

        if (!(nmsStack.getItem() instanceof BlockItem blockItem)) {
            nmsStack.getItem().useOn(new UseOnContext(serverPlayer, hand, hitResult));
            if (!player.isSneaking()) serverPlayer.gameMode.useItem(serverPlayer, serverPlayer.level(), nmsStack, hand);
            return null;
        }

        // Shulker-Boxes are DirectionalPlace based unlike other directional-blocks
        if (org.bukkit.Tag.SHULKER_BOXES.isTagged(itemStack.getType())) {
            placeContext = new DirectionalPlaceContext(serverPlayer.level(), hitResult.getBlockPos(), hitResult.getDirection(), nmsStack, hitResult.getDirection().getOpposite());
        }

        BlockPos pos = hitResult.getBlockPos();
        InteractionResult result = blockItem.place(placeContext);
        if (result == InteractionResult.FAIL) return null;
        if (placeContext instanceof DirectionalPlaceContext && player.getGameMode() != org.bukkit.GameMode.CREATIVE) itemStack.setAmount(itemStack.getAmount() - 1);
        World world = player.getWorld();

        if(!player.isSneaking()) {
            BlockPos clickPos = placeContext.getClickedPos();
            Block block = world.getBlockAt(clickPos.getX(), clickPos.getY(), clickPos.getZ());
            SoundGroup sound = block.getBlockData().getSoundGroup();

            world.playSound(
                    BlockHelpers.toCenterBlockLocation(block.getLocation()), sound.getPlaceSound(),
                    SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F
            );
        }

        return world.getBlockAt(pos.getX(), pos.getY(), pos.getZ()).getBlockData();
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
                channel.pipeline().replace("encoder", "encoder", new PacketEncoder(Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL));
            } else channel.pipeline().replace("encoder", "encoder", previousHandler);
        }

        if (decoder.containsKey(channel)) {
            ChannelHandler previousHandler = decoder.remove(channel);
            if (previousHandler instanceof PacketDecoder) {
                channel.pipeline().replace("decoder", "decoder", new PacketDecoder(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL));
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

        private final AttributeKey<ConnectionProtocol.CodecData<?>> protocolDirection = Connection.ATTRIBUTE_CLIENTBOUND_PROTOCOL;

        @Override
        public void encode(ChannelHandlerContext ctx, Packet<?> packet, ByteBuf byteBuf) {
            if (ctx.channel() == null) throw new RuntimeException("Channel is null");
            Attribute<ConnectionProtocol.CodecData<?>> attribute = ctx.channel().attr(protocolDirection);
            ConnectionProtocol.CodecData<?> codecData = attribute.get();
            int packetId = codecData.packetId(packet);

            FriendlyByteBuf packetDataSerializer = new CustomDataSerializer(player, byteBuf);
            packetDataSerializer.writeVarInt(packetId);

            try {
                int integer2 = packetDataSerializer.writerIndex();
                packet.write(packetDataSerializer);
                int integer3 = packetDataSerializer.writerIndex() - integer2;
                if (integer3 > 8388608) {
                    throw new IllegalArgumentException("Packet too big (is " + integer3 + ", should be less than 8388608): " + packet);
                }
                ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
            } catch (Exception e) {
                if (packet.isSkippable()) throw new SkipPacketException(e);
                throw e;
            }
            ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
        }
    }

    private static class CustomPacketDecoder extends ByteToMessageDecoder {
        @Nullable private final Player player;

        private CustomPacketDecoder(@Nullable Player player) {
            this.player = player;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws IOException {
            final ByteBuf bufferCopy = buffer.copy();
            if (buffer.readableBytes() == 0) return;

            CustomDataSerializer dataSerializer = new CustomDataSerializer(player, buffer);
            int packetID = dataSerializer.readVarInt();
            Attribute<ConnectionProtocol.CodecData<?>> attribute = ctx.channel().attr(Connection.ATTRIBUTE_SERVERBOUND_PROTOCOL);
            Packet<?> packet = attribute.get().createPacket(packetID, dataSerializer);

            if (dataSerializer.readableBytes() > 0) {
                throw new IOException("Packet " + packetID + " " + packet + " was larger than expected, found " + dataSerializer.readableBytes() + " bytes extra whiløst reading the packet " + packetID);
            } else if (packet instanceof ServerboundChatPacket) {
                FriendlyByteBuf baseSerializer = new FriendlyByteBuf(bufferCopy);
                int basePacketID = baseSerializer.readVarInt();
                packet = attribute.get().createPacket(basePacketID, baseSerializer);
            }

            if (packet == null) throw new IOException("Bad packet id " + packetID);

            out.add(packet);
            ProtocolSwapHandler.swapProtocolIfNeeded(attribute, packet);
        }
    }

    @Override
    public boolean getSupported() {
        return true;
    }
}
