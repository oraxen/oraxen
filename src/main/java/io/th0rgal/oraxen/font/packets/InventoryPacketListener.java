package io.th0rgal.oraxen.font.packets;

import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolInjector;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.handler.NMSPacketHandler;
import io.th0rgal.oraxen.protocol.packet.PacketFlow;
import io.th0rgal.oraxen.protocol.utils.ProtocolComponentSerializer;
import io.th0rgal.oraxen.utils.CacheInvoker;
import io.th0rgal.oraxen.utils.PacketHelpers;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class InventoryPacketListener implements NMSPacketHandler {

    private static final ProtocolMapping<Integer> OPEN_SCREEN_ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_8, 0x2D)
                    .add(MinecraftVersion.MINECRAFT_1_9, 0x13)
                    .add(MinecraftVersion.MINECRAFT_1_13, 0x14)
                    .add(MinecraftVersion.MINECRAFT_1_14, 0x2E)
                    .add(MinecraftVersion.MINECRAFT_1_15, 0x2F)
                    .add(MinecraftVersion.MINECRAFT_1_16, 0x2E)
                    .add(MinecraftVersion.MINECRAFT_1_16_2, 0x2D)
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x2E)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x2B)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x2D)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x2C)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x30)
                    .build();

    private static final CacheInvoker INVOKER = CacheInvoker.get();

    private final ProtocolInjector protocolInjector;
    private final int openScreenId;
    private ProtocolComponentSerializer componentSerializer;
    private MethodHandle componentGetter;
    private Field componentField;

    public InventoryPacketListener(ProtocolInjector protocolInjector) {
        this.protocolInjector = protocolInjector;
        openScreenId = OPEN_SCREEN_ID_MAPPING.get(protocolInjector.getServerVersion());
    }

    public void registerListener() {
        protocolInjector.addNMSHandler(this, PacketFlow.CLIENTBOUND, openScreenId);
    }

    @Override
    public void initialize() {
        try {
            Class<?> openScreenClass = protocolInjector.getNMSPacketClass(PacketFlow.CLIENTBOUND, openScreenId);
            MethodHandles.Lookup openScreenLookup = MethodHandles.privateLookupIn(openScreenClass, MethodHandles.lookup());
            Field componentField = openScreenClass.getDeclaredFields()[2];
            componentField.setAccessible(true);
            componentGetter = openScreenLookup.unreflectGetter(componentField);
            this.componentField = componentField;
            componentSerializer = protocolInjector.getProtocolComponentSerializer(componentField.getType());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handle(Player player, PacketFlow flow, int id, Object packet) {
        try {
            String json = componentSerializer.serialize(INVOKER.cache(componentGetter).invoke(packet));
            json = PacketHelpers.toJson(PacketHelpers.readJson(json));
            componentField.set(packet, componentSerializer.deserialize(json));
        } catch (Throwable ignored) {
        }

        return true;
    }
}
