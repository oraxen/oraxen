package io.th0rgal.oraxen.nms.v1_19_R1;

import io.netty.buffer.Unpooled;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureType;
import io.th0rgal.oraxen.utils.BlockHelpers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;

    public final FurnitureType type;
    public final Integer entityId;
    private final ClientboundAddEntityPacket addEntity;
    private final ClientboundSetEntityDataPacket metadata;

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, Entity baseEntity, FurnitureType type) {
        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());

        this.type = type;
        this.entityId = baseEntity.getEntityId();

        EntityType<?> entityType = type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;
        this.addEntity = new ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(),
                baseLoc.getX(), baseLoc.getY(), baseLoc.getZ(), baseLoc.getPitch(), baseLoc.getYaw(),
                entityType, 0, Vec3.ZERO, 0.0
        );

        friendlyByteBuf.writeVarInt(entityId);
        SynchedEntityData.pack(Arrays.asList(
                new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK), CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))),
                new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT), FurnitureHelpers.yawToRotation(baseEntity.getLocation().getYaw()).ordinal())
        ), friendlyByteBuf);

        this.metadata = new ClientboundSetEntityDataPacket(friendlyByteBuf);
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
}
