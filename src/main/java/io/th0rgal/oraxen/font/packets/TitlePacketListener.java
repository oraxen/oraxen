package io.th0rgal.oraxen.font.packets;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.handler.NMSPacketHandler;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.protocol.utils.ProtocolComponentSerializer;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.CacheInvoker;
import io.th0rgal.oraxen.utils.PacketHelpers;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class TitlePacketListener {

    private final class TitleHandler implements NMSPacketHandler {

        private static final CacheInvoker INVOKER = CacheInvoker.get();

        private final int packetId;
        private MethodHandle nmsGetter;
        private Field nmsField;
        private ProtocolComponentSerializer nmsSerializer;
        private MethodHandle adventureGetter;
        private MethodHandle adventureSetter;
        private MethodHandle bungeeGetter;
        private MethodHandle bungeeSetter;

        TitleHandler(int packetId) {
            this.packetId = packetId;
        }

        private void initializeAdventure(MethodHandles.Lookup lookup, Field field) throws IllegalAccessException {
            adventureGetter = lookup.unreflectGetter(field);
            adventureSetter = lookup.unreflectSetter(field);
        }

        private void initializeBungee(MethodHandles.Lookup lookup, Field field) throws IllegalAccessException {
            bungeeGetter = lookup.unreflectGetter(field);
            bungeeSetter = lookup.unreflectSetter(field);
        }

        private void initializeNMS(MethodHandles.Lookup lookup, Field field) throws IllegalAccessException {
            field.setAccessible(true);
            nmsGetter = lookup.unreflectGetter(field);
            nmsField = field;
            nmsSerializer = protocolInjector.getProtocolComponentSerializer(nmsField.getType());
        }

        @Override
        public void initialize() {
            try {
                Class<?> packetClass = protocolInjector.getNMSPacketClass(PacketFlow.CLIENTBOUND, packetId);
                MethodHandles.Lookup packetLookup = MethodHandles.privateLookupIn(packetClass, MethodHandles.lookup());
                Field[] fields = packetClass.getDeclaredFields();
                for (int i = 0; i < Math.min(3, fields.length); i++) {
                    Field field = fields[i];
                    Class<?> fieldType = field.getType();
                    if (fieldType.getPackageName().startsWith("net.minecraft.")) {
                        if (Settings.DEBUG.toBool()) Logs.logInfo("Found NMS component for " + packetClass.getName() + ".");
                        initializeNMS(packetLookup, field);
                    } else if (fieldType.getSimpleName().equals("Component")) {
                        if (Settings.DEBUG.toBool()) Logs.logInfo("Found Adventure component for " + packetClass.getName() + ".");
                        initializeAdventure(packetLookup, field);
                    } else if (fieldType.getName().equals("[Lnet.md_5.bungee.api.chat.BaseComponent;")) {
                        if (Settings.DEBUG.toBool()) Logs.logInfo("Found Bungee component for " + packetClass.getName() + ".");
                        initializeBungee(packetLookup, field);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean handle(Player player, PacketFlow flow, int id, Object packet) {
            try {
                if (adventureGetter != null) {
                    Component adventureComponent = (Component) INVOKER.cache(adventureGetter).invoke(packet);
                    if (adventureComponent != null) {
                        String title = AdventureUtils.MINI_MESSAGE.serialize(adventureComponent);
                        INVOKER.cache(adventureSetter).invoke(packet, AdventureUtils.GSON_SERIALIZER.deserialize(PacketHelpers.toJson(title)));
                        return true;
                    }
                }

                if (bungeeGetter != null) {
                    BaseComponent[] bungeeComponent = (BaseComponent[]) INVOKER.cache(bungeeGetter).invoke(packet);
                    if (bungeeComponent != null) {
                        String title = PacketHelpers.readJson(ComponentSerializer.toString(bungeeComponent));
                        INVOKER.cache(bungeeSetter).invoke(packet, ComponentSerializer.parse(PacketHelpers.toJson(title)));
                        return true;
                    }
                }

                if (nmsGetter != null) {
                    Object nmsComponent = INVOKER.cache(nmsGetter).invoke(packet);
                    if (nmsComponent != null) {
                        String title = PacketHelpers.readJson(nmsSerializer.serialize(nmsComponent));
                        nmsField.set(packet, nmsSerializer.deserialize(PacketHelpers.toJson(title)));
                        return true;
                    }
                }
            } catch (Throwable e) {
                if (Settings.DEBUG.toBool()) {
                    Logs.logWarning("Error whilst modifying " + packet.getClass().getSimpleName() + "(" + id + ") packet");
                    e.printStackTrace();
                }
            }

            return true;
        }
    }

    private static final ProtocolMapping<Integer> SET_ACTION_BAR_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x41)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x40)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x43)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x42)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x46)
                    .build();

    private static final ProtocolMapping<Integer> SET_SUBTITLE_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x57)
                    .add(MinecraftVersion.MINECRAFT_1_18, 0x58)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x5B)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x59)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x5D)
                    .build();

    private static final ProtocolMapping<Integer> SET_TITLE_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x59)
                    .add(MinecraftVersion.MINECRAFT_1_18, 0x5A)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x5D)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x5B)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x5F)
                    .build();

    private final ProtocolInjector protocolInjector;
    private final int setActionBarId;
    private final int setSubtitleId;
    private final int setTitleId;

    public TitlePacketListener(ProtocolInjector protocolInjector) {
        this.protocolInjector = protocolInjector;
        setActionBarId = SET_ACTION_BAR_ID_MAPPING.get(protocolInjector.getServerVersion());
        setSubtitleId = SET_SUBTITLE_ID_MAPPING.get(protocolInjector.getServerVersion());
        setTitleId = SET_TITLE_ID_MAPPING.get(protocolInjector.getServerVersion());
    }

    public void registerListener() {
        if (Settings.FORMAT_TITLES.toBool()) {
            protocolInjector.addNMSHandler(new TitleHandler(setTitleId), PacketFlow.CLIENTBOUND, setTitleId);
        }

        if (Settings.FORMAT_SUBTITLES.toBool()) {
            protocolInjector.addNMSHandler(new TitleHandler(setSubtitleId), PacketFlow.CLIENTBOUND, setSubtitleId);
        }

        if (Settings.FORMAT_ACTION_BAR.toBool()) {
            protocolInjector.addNMSHandler(new TitleHandler(setActionBarId), PacketFlow.CLIENTBOUND, setActionBarId);
        }
    }
}
