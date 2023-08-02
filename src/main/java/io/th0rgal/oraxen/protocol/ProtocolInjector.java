package io.th0rgal.oraxen.protocol;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.AttributeKey;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.protocol.handler.HandshakeListener;
import io.th0rgal.oraxen.protocol.handler.NMSInitializer;
import io.th0rgal.oraxen.protocol.handler.NMSPacketHandler;
import io.th0rgal.oraxen.protocol.injection.ChannelInjector;
import io.th0rgal.oraxen.protocol.injection.NettyInjector;
import io.th0rgal.oraxen.protocol.packet.Packet;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.protocol.utils.NMSComponentSerializer;
import io.th0rgal.oraxen.protocol.utils.ProtocolComponentSerializer;
import io.th0rgal.oraxen.utils.CacheInvoker;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolInjector implements Listener {

    public static final AttributeKey<Player> PLAYER_ATTRIBUTE = AttributeKey.valueOf("oraxen:bukkit_player");
    private static final CacheInvoker INVOKER = CacheInvoker.get();

    private final MinecraftVersion serverVersion;
    private final NettyInjector nettyInjector;
    private final Map<String, Channel> playerChannels = new ConcurrentHashMap<>();
    private final Int2ObjectMap<Set<NMSPacketHandler>> clientboundPacketHandlers = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Set<NMSPacketHandler>> serverboundPacketHandlers = new Int2ObjectOpenHashMap<>();
    private Int2ObjectMap<Class<?>> clientboundPacketClasses;
    private Int2ObjectMap<Class<?>> serverboundPacketClasses;
    private MethodHandle getPacketIdMH;
    private Object playProtocol;
    private Object clientboundFlow;
    private Object serverboundFlow;

    public ProtocolInjector() {
        this(new NettyInjector());
    }

    public ProtocolInjector(NettyInjector nettyInjector) {
        this.nettyInjector = nettyInjector;
        serverVersion = detectServerVersion();
        nettyInjector.addInjector(new ChannelInjector() {
            @Override
            public void inject(Channel channel) {
                channel.pipeline().addAfter("splitter", "oraxen:handshake",
                        new HandshakeListener(ProtocolInjector.this));
                if (playProtocol == null) {
                    channel.pipeline().addBefore("splitter", "oraxen:nms_initializer",
                            new NMSInitializer(ProtocolInjector.this));
                }
            }

            private void uninjectHandler(Channel channel, String name) {
                try {
                    channel.pipeline().remove(name);
                } catch (NoSuchElementException ignored) {
                }
            }

            @Override
            public void uninject(Channel channel) {
                channel.attr(PLAYER_ATTRIBUTE).set(null);
                uninjectHandler(channel, "oraxen:nms_initializer");
                uninjectHandler(channel, "oraxen:handshake");
                uninjectHandler(channel, "oraxen:login");
                uninjectHandler(channel, "oraxen:encoder");
                uninjectHandler(channel, "oraxen:nms_handler");
                uninjectHandler(channel, "oraxen:disconnect");
                playerChannels.clear();
            }
        });
    }

    private MinecraftVersion detectServerVersion() {
        try {
            Bukkit.class.getMethod("getUnsafe");
            Method getProtocolVersion = Class.forName("org.bukkit.UnsafeValues").getDeclaredMethod("getProtocolVersion");
            return MinecraftVersion.fromVersionNumber((int) getProtocolVersion.invoke(Bukkit.getUnsafe()));
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
            Object worldVersion = null;
            for (Method method : sharedConstants.getDeclaredMethods()) {
                if (!method.getReturnType().getSimpleName().equals("WorldVersion")) {
                    continue;
                }

                if (method.getParameterTypes().length != 0) {
                    continue;
                }

                method.setAccessible(true);
                worldVersion = method.invoke(null);
                break;
            }

            if (worldVersion != null) {
                for (Method method : worldVersion.getClass().getDeclaredMethods()) {
                    if (method.getReturnType() != int.class) {
                        continue;
                    }

                    if (method.getParameterTypes().length != 0) {
                        continue;
                    }

                    method.setAccessible(true);
                    int protocolVersion = (int) method.invoke(worldVersion);
                    return MinecraftVersion.fromVersionNumber(protocolVersion);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException("Could not detect server protocol version.");
    }

    public void inject() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        nettyInjector.inject();
    }

    public void uninject() {
        HandlerList.unregisterAll(this);
        clientboundPacketHandlers.clear();
        serverboundPacketHandlers.clear();
        nettyInjector.uninject();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Channel channel = playerChannels.get(event.getPlayer().getName());

        if (channel.pipeline().get("compress") != null) {
            channel.attr(PLAYER_ATTRIBUTE).set(event.getPlayer());
            ChannelHandler handler = channel.pipeline().get("oraxen:encoder");
            channel.pipeline().remove(handler);
            channel.pipeline().addAfter("compress", "oraxen:encoder", handler);
        }
    }

    public void sendPacket(Player player, Packet packet) {
        if (player == null) {
            return;
        }

        sendPacket(player.getName(), packet);
    }

    public void sendPacket(String player, Packet packet) {
        Channel channel = playerChannels.get(player);
        if (channel == null) {
            return;
        }

        ProtocolUtil.writePacket(channel, packet);
    }

    @SuppressWarnings("unchecked")
    public void initializeProtocol(Class<?> connectionProtocolClass) {
        playProtocol = connectionProtocolClass.getEnumConstants()[1];

        Class<?> flowClass = null;
        for (Field field : connectionProtocolClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType type)) {
                continue;
            }

            if (type.getRawType() != Map.class) {
                continue;
            }

            Type firstParameter = type.getActualTypeArguments()[0];
            if (!(firstParameter instanceof Class<?> parameterClass)) {
                continue;
            }

            if (!parameterClass.isEnum()) {
                continue;
            }

            flowClass = parameterClass;
            break;
        }

        if (flowClass == null) {
            throw new IllegalStateException("Could not find PacketFlow class.");
        }

        Object[] flows = flowClass.getEnumConstants();
        serverboundFlow = flows[0];
        clientboundFlow = flows[1];

        Method getPacketIdMethod = null;

        for (Method method : connectionProtocolClass.getDeclaredMethods()) {
            if (method.getReturnType() != Integer.class && method.getReturnType() != int.class) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 2 || parameterTypes[0] != flowClass) {
                continue;
            }

            getPacketIdMethod = method;
            break;
        }

        if (getPacketIdMethod == null) {
            throw new IllegalStateException("Could not find getPacketId method.");
        }

        try {
            getPacketIdMH = MethodHandles.lookup().unreflect(getPacketIdMethod);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        Method packetsByIdsMethod = null;

        for (Method method : connectionProtocolClass.getDeclaredMethods()) {
            if (method.getReturnType() != Int2ObjectMap.class) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1 || parameterTypes[0] != flowClass) {
                continue;
            }

            method.setAccessible(true);
            packetsByIdsMethod = method;
            break;
        }

        if (packetsByIdsMethod == null) {
            throw new IllegalStateException("Could not find getPacketsByIds method.");
        }

        try {
            clientboundPacketClasses = (Int2ObjectMap<Class<?>>) packetsByIdsMethod.invoke(playProtocol, clientboundFlow);
            serverboundPacketClasses = (Int2ObjectMap<Class<?>>) packetsByIdsMethod.invoke(playProtocol, serverboundFlow);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public ProtocolComponentSerializer getProtocolComponentSerializer(Class<?> componentClass) {
        return NMSComponentSerializer.get(componentClass);
    }

    public Class<?> getNMSPacketClass(PacketFlow flow, int id) {
        return (flow == PacketFlow.CLIENTBOUND ? clientboundPacketClasses : serverboundPacketClasses).get(id);
    }

    public int getNMSPacketID(PacketFlow flow, Object packet) throws Throwable {
        Object nmsFlow = flow == PacketFlow.CLIENTBOUND ? clientboundFlow : serverboundFlow;
        return (int) INVOKER.cache(getPacketIdMH).invoke(playProtocol, nmsFlow, packet);
    }

    public void addNMSHandler(NMSPacketHandler handler, PacketFlow flow, int... ids) {
        var nmsPacketHandlers = flow == PacketFlow.CLIENTBOUND ? clientboundPacketHandlers : serverboundPacketHandlers;
        for (int id : ids) {
            nmsPacketHandlers.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(handler);
        }
    }

    public void removeNMSHandler(NMSPacketHandler handler, PacketFlow flow, int... ids) {
        var nmsPacketHandlers = flow == PacketFlow.CLIENTBOUND ? clientboundPacketHandlers : serverboundPacketHandlers;
        for (int id : ids) {
            Set<NMSPacketHandler> handlers = nmsPacketHandlers.get(id);
            if (handlers == null) {
                continue;
            }

            if (handlers.remove(handler) && handlers.isEmpty()) {
                nmsPacketHandlers.remove(id);
            }
        }
    }

    public Set<NMSPacketHandler> getNMSHandlers(PacketFlow flow, int id) {
        return (flow == PacketFlow.CLIENTBOUND ? clientboundPacketHandlers : serverboundPacketHandlers).get(id);
    }

    public Int2ObjectMap<Set<NMSPacketHandler>> getAllNMSHandlers(PacketFlow flow) {
        return flow == PacketFlow.CLIENTBOUND ? clientboundPacketHandlers : serverboundPacketHandlers;
    }

    public NettyInjector getNettyInjector() {
        return nettyInjector;
    }

    public Map<String, Channel> getPlayerChannels() {
        return playerChannels;
    }

    public MinecraftVersion getServerVersion() {
        return serverVersion;
    }

    public Object getPlayProtocol() {
        return playProtocol;
    }

    public Object getClientboundFlow() {
        return clientboundFlow;
    }

    public Object getServerboundFlow() {
        return serverboundFlow;
    }
}
