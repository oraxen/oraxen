package io.th0rgal.oraxen.nms.v1_19_R1.protocol;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class PacketListenerBuilder {
    private final List<PacketEntry<?>> packetEntries = new ArrayList<>();

    public <T extends Packet<?>> PacketListenerBuilder add(Class<T> clazz, BiFunction<ServerPlayer, T, Boolean> function) {
        packetEntries.add(new PacketEntry<>(clazz, function));
        return this;
    }

    private record PacketEntry<T extends Packet<?>>(Class<T> clazz, BiFunction<ServerPlayer, T, Boolean> function) {
        public boolean handle(ServerPlayer player, Object object) {
            return !clazz.isAssignableFrom(object.getClass()) || function.apply(player, clazz.cast(object));
        }
    }

    public PacketListener build() {
        return (player, packet) -> {
            boolean result = true;
            for (PacketEntry<?> packetEntry : packetEntries) {
                if (!packetEntry.handle(player, packet)) {
                    result = false;
                }
            }
            return result;
        };
    }
}
