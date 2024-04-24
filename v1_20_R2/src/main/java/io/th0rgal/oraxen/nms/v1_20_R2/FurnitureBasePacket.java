package io.th0rgal.oraxen.nms.v1_20_R2;

import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.DisplayEntityProperties;
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
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;

    private final int DISPLAY_SCALE_ID = 12;
    private final int DISPLAY_BILLBOARD_ID = 15;
    private final int DISPLAY_BRIGHTNESS_ID = 16;
    private final int DISPLAY_VIEW_RANGE_ID = 17;
    private final int DISPLAY_SHADOW_RADIUS_ID = 18;
    private final int DISPLAY_SHADOW_STRENGTH_ID = 19;
    private final int DISPLAY_WIDTH_ID = 20;
    private final int DISPLAY_HEIGHT_ID = 21;
    private final int DISPLAY_GLOW_ID = 22;

    private final int ITEM_DISPLAY_ITEM_ID = 23;
    private final int ITEM_DISPLAY_TRANSFORM_ID = 24;

    public final FurnitureType type;
    public final Integer entityId;
    private final ClientboundAddEntityPacket addEntity;
    private final ClientboundSetEntityDataPacket metadata;

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, Entity baseEntity, FurnitureType type) {
        this.type = type;
        this.entityId = baseEntity.getEntityId();
        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        EntityType<?> entityType = type == FurnitureType.DISPLAY_ENTITY ? EntityType.ITEM_DISPLAY : type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;

        this.addEntity = new ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(),
                baseLoc.x(), baseLoc.y(), baseLoc.z(), baseLoc.getPitch(), baseLoc.getYaw(),
                entityType, 0, Vec3.ZERO, 0.0
        );

        this.metadata = new ClientboundSetEntityDataPacket(entityId, dataValues(furnitureBase));
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

    private List<SynchedEntityData.DataValue<?>> dataValues(FurnitureBaseEntity furnitureBase) {
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        if (type == FurnitureType.DISPLAY_ENTITY) {
            DisplayEntityProperties displayProp = furnitureBase.mechanic().displayEntityProperties();
            data.add(new SynchedEntityData.DataValue<>(DISPLAY_WIDTH_ID, EntityDataSerializers.FLOAT, displayProp.displayWidth()));
            data.add(new SynchedEntityData.DataValue<>(DISPLAY_HEIGHT_ID, EntityDataSerializers.FLOAT, displayProp.displayHeight()));

            Optional.ofNullable(displayProp.scale()).ifPresent(scale -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_SCALE_ID, EntityDataSerializers.VECTOR3, displayProp.scale())));
            Optional.ofNullable(displayProp.viewRange()).ifPresent(viewRange  -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_VIEW_RANGE_ID, EntityDataSerializers.FLOAT, (float) viewRange)));
            Optional.ofNullable(displayProp.shadowRadius()).ifPresent(shadowRadius  -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_SHADOW_RADIUS_ID, EntityDataSerializers.FLOAT, shadowRadius)));
            Optional.ofNullable(displayProp.shadowStrength()).ifPresent(shadowStrength  -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_SHADOW_STRENGTH_ID, EntityDataSerializers.FLOAT, shadowStrength)));
            Optional.ofNullable(displayProp.trackingRotation()).ifPresent(tracking ->data.add(new SynchedEntityData.DataValue<>(DISPLAY_BILLBOARD_ID, EntityDataSerializers.BYTE, (byte) tracking.ordinal())));
            Optional.ofNullable(displayProp.brightness()).ifPresent(brightness -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_BRIGHTNESS_ID, EntityDataSerializers.INT, (brightness.getBlockLight() << 4 | brightness.getSkyLight() << 20))));
            Optional.ofNullable(displayProp.glowColor()).ifPresent(glow -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_GLOW_ID, EntityDataSerializers.INT, glow.asRGB())));

            data.add(new SynchedEntityData.DataValue<>(ITEM_DISPLAY_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
            data.add(new SynchedEntityData.DataValue<>(ITEM_DISPLAY_TRANSFORM_ID, EntityDataSerializers.BYTE, (byte) displayProp.displayTransform().ordinal()));
        } else {
            data.add(new SynchedEntityData.DataValue<>(ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
            data.add(new SynchedEntityData.DataValue<>(ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT, FurnitureHelpers.yawToRotation(furnitureBase.baseEntity().getYaw()).ordinal()));
        }

        return data;
    }
}
