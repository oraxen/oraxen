package io.th0rgal.oraxen.protocol.packet;

import io.netty.buffer.ByteBuf;
import io.th0rgal.oraxen.protocol.MinecraftVersion;
import io.th0rgal.oraxen.protocol.ProtocolMapping;
import io.th0rgal.oraxen.protocol.ProtocolUtil;

public class ResourcePackRequest implements Packet {

    private final ProtocolMapping<Integer> ID_MAPPING =
            new ProtocolMapping<Integer>()
                    .add(MinecraftVersion.MINECRAFT_1_8, 0x48)
                    .add(MinecraftVersion.MINECRAFT_1_9, 0x32)
                    .add(MinecraftVersion.MINECRAFT_1_12, 0x33)
                    .add(MinecraftVersion.MINECRAFT_1_12_1, 0x34)
                    .add(MinecraftVersion.MINECRAFT_1_13, 0x37)
                    .add(MinecraftVersion.MINECRAFT_1_14, 0x39)
                    .add(MinecraftVersion.MINECRAFT_1_15, 0x3A)
                    .add(MinecraftVersion.MINECRAFT_1_16, 0x39)
                    .add(MinecraftVersion.MINECRAFT_1_16_2, 0x38)
                    .add(MinecraftVersion.MINECRAFT_1_17, 0x3C)
                    .add(MinecraftVersion.MINECRAFT_1_19, 0x3A)
                    .add(MinecraftVersion.MINECRAFT_1_19_1, 0x3D)
                    .add(MinecraftVersion.MINECRAFT_1_19_3, 0x3C)
                    .add(MinecraftVersion.MINECRAFT_1_19_4, 0x40)
                    .build();

    private final String url;
    private final String hash;
    private final boolean required;
    private final String prompt;

    public ResourcePackRequest(String url, String hash, boolean required, String prompt) {
        this.url = url;
        this.hash = hash;
        this.required = required;
        this.prompt = prompt;
    }

    @Override
    public void encode(ByteBuf buf, MinecraftVersion version) {
        ProtocolUtil.writeString(buf, url);
        ProtocolUtil.writeString(buf, hash);
        if (version.compareTo(MinecraftVersion.MINECRAFT_1_17) >= 0) {
            buf.writeBoolean(required);
            if (prompt != null) {
                buf.writeBoolean(true);
                ProtocolUtil.writeString(buf, prompt);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public int getID(MinecraftVersion version) {
        return ID_MAPPING.get(version);
    }
}
