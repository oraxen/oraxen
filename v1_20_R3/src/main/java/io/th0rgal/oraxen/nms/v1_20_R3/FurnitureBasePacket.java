package io.th0rgal.oraxen.nms.v1_20_R3;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureType;
import io.th0rgal.oraxen.utils.BlockHelpers;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;
    private final int ITEM_DISPLAY_ITEM_ID = 23;
    private final int ITEM_DISPLAY_TRANSFORM_ID = 24;

    public final FurnitureType type;
    public final Integer entityId;
    public final ClientboundAddEntityPacket addEntity;
    private final ClientboundSetEntityDataPacket metadata;

    public FurnitureBasePacket(FurnitureBaseEntity furniture, Entity baseEntity, FurnitureType type) {
        this.type = type;
        this.entityId = furniture.entityId(type);
        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        EntityType<?> entityType = type == FurnitureType.DISPLAY_ENTITY ? EntityType.ITEM_DISPLAY : type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;

        this.addEntity = new ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(),
                baseLoc.x(), baseLoc.y(), baseLoc.z(), baseLoc.getPitch(), baseLoc.getYaw(),
                entityType, 0, Vec3.ZERO, 0.0
        );

        this.metadata = new ClientboundSetEntityDataPacket(
                entityId, Arrays.asList(
                new SynchedEntityData.DataValue<>(type == FurnitureType.DISPLAY_ENTITY ? ITEM_DISPLAY_ITEM_ID : ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))),
                new SynchedEntityData.DataValue<>(type == FurnitureType.DISPLAY_ENTITY ? ITEM_DISPLAY_TRANSFORM_ID : ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT, (type == FurnitureType.DISPLAY_ENTITY ? furniture.mechanic().displayEntityProperties().displayTransform() : FurnitureHelpers.yawToRotation(baseEntity.getYaw())).ordinal())
        ));
    }

    public FurnitureBasePacket(FurnitureType type, int entityId, ClientboundAddEntityPacket addEntity, ClientboundSetEntityDataPacket metadata) {
        this.type = type;
        this.entityId = entityId;
        this.addEntity = addEntity;
        this.metadata = metadata;
    }

    public ClientboundAddEntityPacket entityPacket() {
        return this.addEntity;
    }

    public ClientboundSetEntityDataPacket metadataPacket() {
        return this.metadata;
    }

    public ClientboundSetEntityDataPacket metadataPacket(FurnitureBaseEntity baseEntity) {
        metadata.packedItems().add(new SynchedEntityData.DataValue<>(type == FurnitureType.DISPLAY_ENTITY ? ITEM_DISPLAY_ITEM_ID : ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(baseEntity.itemStack())));
        return metadata;
    }
}
