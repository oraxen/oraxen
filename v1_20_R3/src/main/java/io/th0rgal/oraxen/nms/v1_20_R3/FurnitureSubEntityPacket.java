package io.th0rgal.oraxen.nms.v1_20_R3;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

public class FurnitureSubEntityPacket {
    public Integer entityId;
    public ClientboundAddEntityPacket addEntity;
    public ClientboundSetEntityDataPacket metadata;

    public FurnitureSubEntityPacket(int entityId, ClientboundAddEntityPacket addEntity, ClientboundSetEntityDataPacket metadata) {
        this.entityId = entityId;
        this.addEntity = addEntity;
        this.metadata = metadata;
    }
}
