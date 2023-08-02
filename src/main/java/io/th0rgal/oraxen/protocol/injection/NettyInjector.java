package io.th0rgal.oraxen.protocol.injection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.th0rgal.oraxen.utils.OverlayList;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class NettyInjector {

    private final class InjectedList<T extends ChannelFuture> extends OverlayList<T> {

        private InjectedList(List<T> originalList) {
            super(originalList);
        }

        @Override
        public boolean add(T element) {
            inject(element.channel());
            return super.add(element);
        }

        @Override
        public void add(int index, T element) {
            inject(element.channel());
            super.add(index, element);
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            c.forEach(channelFuture -> inject(channelFuture.channel()));
            return super.addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            c.forEach(channelFuture -> inject(channelFuture.channel()));
            return super.addAll(index, c);
        }
    }

    private static Field findField(Class<?> cls, Predicate<Field> predicate) throws NoSuchMethodException {
        for (Field field : cls.getDeclaredFields()) {
            if (predicate.test(field)) {
                field.setAccessible(true);
                return field;
            }
        }

        Class<?> superclass = cls.getSuperclass();
        if (superclass != null) {
            return findField(superclass, predicate);
        } else {
            throw new NoSuchMethodException("in class " + cls.getName());
        }
    }

    private final List<ChannelInjector> injectors = Collections.synchronizedList(new ArrayList<>());
    private final Field openChannelsField;
    private final Object connection;
    private List<? extends ChannelFuture> openChannels;

    @SuppressWarnings("unchecked")
    public NettyInjector() {
        try {
            Server server = Bukkit.getServer();
            Field consoleField = server.getClass().getDeclaredField("console");
            consoleField.setAccessible(true);
            Object minecraftServer = consoleField.get(server);
            Field connectionField = findField(minecraftServer.getClass(),
                    field -> field.getType().getSimpleName().equals("ServerConnection"));
            connection = connectionField.get(minecraftServer);

            for (Field field : connection.getClass().getDeclaredFields()) {
                Type genericType = field.getGenericType();
                if (!(genericType instanceof ParameterizedType type)) {
                    continue;
                }

                if (type.getRawType() != List.class) {
                    continue;
                }

                Type firstParameter = type.getActualTypeArguments()[0];
                if (!firstParameter.getTypeName().endsWith("ChannelFuture")) {
                    continue;
                }

                field.setAccessible(true);
                openChannelsField = field;
                openChannels = (List<? extends ChannelFuture>) field.get(connection);
                return;
            }

            throw new InjectionException("Could not inject.");
        } catch (ReflectiveOperationException e) {
            throw new InjectionException(e);
        }
    }

    public void inject() {
        ensureNotInjected();
        openChannels = new InjectedList<>(openChannels);
        openChannels.forEach(channelFuture -> inject(channelFuture.channel()));

        try {
            openChannelsField.set(connection, openChannels);
        } catch (ReflectiveOperationException e) {
            throw new InjectionException(e);
        }
    }

    public void uninject() {
        ensureInjected();
        openChannels.forEach(channelFuture -> uninject(channelFuture.channel()));
        openChannels = ((InjectedList<? extends ChannelFuture>) openChannels).getOriginalList();

        try {
            openChannelsField.set(connection, openChannels);
        } catch (ReflectiveOperationException e) {
            throw new InjectionException(e);
        }
    }

    private void uninject(Channel channel) {
        channel.pipeline().addFirst(new ChannelInjectionHandler(injectors, ChannelInjectionHandler.Operation.UNINJECT));
    }

    private void inject(Channel channel) {
        channel.pipeline().addFirst(new ChannelInjectionHandler(injectors, ChannelInjectionHandler.Operation.INJECT));
    }


    public void addInjector(ChannelInjector injector) {
        ensureNotInjected();
        injectors.add(injector);
    }

    public void removeInjector(ChannelInjector injector) {
        ensureNotInjected();
        injectors.remove(injector);
    }

    public List<? extends ChannelFuture> getOpenChannels() {
        return openChannels;
    }

    private void ensureNotInjected() {
        if (openChannels instanceof InjectedList) {
            throw new InjectionException("Already injected.");
        }
    }

    private void ensureInjected() {
        if (!(openChannels instanceof InjectedList)) {
            throw new InjectionException("Not injected.");
        }
    }
}