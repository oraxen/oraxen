package io.th0rgal.oraxen.nms.v1_20_R3;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

public class FurnitureInteractionHitboxPacket {
    public Integer entityId;
    public ClientboundAddEntityPacket addEntity;
    public ClientboundSetEntityDataPacket metadata;

    public FurnitureInteractionHitboxPacket(int entityId, ClientboundAddEntityPacket addEntity, ClientboundSetEntityDataPacket metadata) {
        this.entityId = entityId;
        this.addEntity = addEntity;
        this.metadata = metadata;
    }
}
