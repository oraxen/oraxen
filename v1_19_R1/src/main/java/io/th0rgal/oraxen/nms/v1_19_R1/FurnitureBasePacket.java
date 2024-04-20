package io.th0rgal.oraxen.nms.v1_19_R1;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureType;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

public class FurnitureBasePacket {

    public FurnitureType type;
    public Integer entityId;
    public ClientboundAddEntityPacket addEntity;
    public ClientboundSetEntityDataPacket metadata;

    public FurnitureBasePacket(FurnitureType type, int entityId, ClientboundAddEntityPacket addEntity, ClientboundSetEntityDataPacket metadata) {
        this.type = type;
        this.entityId = entityId;
        this.addEntity = addEntity;
        this.metadata = metadata;
    }
}
