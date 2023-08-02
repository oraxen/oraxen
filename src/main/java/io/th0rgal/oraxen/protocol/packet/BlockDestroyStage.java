package io.th0rgal.oraxen.protocol.packet;

import io.netty.buffer.ByteBuf;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.ProtocolUtil;
import org.bukkit.Location;

public class BlockDestroyStage implements Packet {

    private static final ProtocolMapping<Integer> ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_8, 0x25)
                    .add(MinecraftVersion.MINECRAFT_1_9, 0x08)
                    .add(MinecraftVersion.MINECRAFT_1_15, 0x09)
                    .add(MinecraftVersion.MINECRAFT_1_16, 0x08)
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x09)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x06)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x07)
                    .build();

    private final int entity;
    private final Location location;
    private final int stage;

    public BlockDestroyStage(int entity, Location location, int stage) {
        this.entity = entity;
        this.location = location;
        this.stage = stage;
    }

    @Override
    public void encode(ByteBuf buf, MinecraftVersion version) {
        ProtocolUtil.writeVarInt(buf, entity);
        buf.writeLong(((location.getBlockX() & 0x3FFFFFFL) << 38)
                | ((location.getBlockZ() & 0x3FFFFFFL) << 12)
                | (location.getBlockY() & 0xFFF));
        buf.writeByte(stage);
    }

    @Override
    public int getID(MinecraftVersion version) {
        return ID_MAPPING.get(version);
    }
}
