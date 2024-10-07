package io.th0rgal.oraxen.nms.v1_21_R1.furniture;

import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureProperties;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int DISPLAY_TRANSLATION = 11;
    private final int DISPLAY_SCALE_ID = 12;
    private final int DISPLAY_QUATERNION_LEFT = 13;
    private final int DISPLAY_QUATERNION_RIGHT = 14;
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

    public final Integer entityId;
    public final UUID uuid;
    private final ClientboundAddEntityPacket entityPacket;
    private final ClientboundSetEntityDataPacket metadataPacket;
    private final ClientboundBundlePacket bundlePacket;

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, ItemDisplay baseEntity) {
        this.entityId = furnitureBase.baseId();
        this.uuid = furnitureBase.baseUUID();
        Location baseLoc = baseEntity.getLocation();
        double x = baseLoc.x(), y = baseLoc.y(), z = baseLoc.z();

        this.entityPacket = new ClientboundAddEntityPacket(
                entityId, uuid, x, y, z, baseLoc.getPitch(), baseLoc.getYaw(),
                EntityType.ITEM_DISPLAY, 1, Vec3.ZERO, 0.0
        );

        this.metadataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues(furnitureBase, baseEntity));
        this.bundlePacket = new ClientboundBundlePacket(List.of(entityPacket, metadataPacket));
    }

    public ClientboundAddEntityPacket entityPacket() {
        return this.entityPacket;
    }

    public ClientboundSetEntityDataPacket metadataPacket() {
        return this.metadataPacket;
    }

    public ClientboundBundlePacket bundlePacket() {
        return this.bundlePacket;
    }

    public int entityId() {
        return this.entityId;
    }

    public UUID uuid() {
        return this.uuid;
    }

    private List<SynchedEntityData.DataValue<?>> dataValues(FurnitureBaseEntity furnitureBase, ItemDisplay baseEntity) {
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        FurnitureProperties properties = furnitureBase.mechanic().properties();
        Transformation transformation = baseEntity.getTransformation();

        data.add(dataValue(0, EntityDataSerializers.BYTE, (byte) 0x20));
        data.add(dataValue(2, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(PaperAdventure.asVanilla(Component.empty()))));
        data.add(dataValue(3, EntityDataSerializers.BOOLEAN, false));

        data.add(dataValue(9, EntityDataSerializers.INT, 0));
        data.add(dataValue(DISPLAY_WIDTH_ID, EntityDataSerializers.FLOAT, properties.displayWidth()));
        data.add(dataValue(DISPLAY_HEIGHT_ID, EntityDataSerializers.FLOAT, properties.displayHeight()));

        properties.viewRange().ifPresent(viewRange -> data.add(dataValue(DISPLAY_VIEW_RANGE_ID, EntityDataSerializers.FLOAT, (float) viewRange)));
        properties.shadowRadius().ifPresent(shadowRadius -> data.add(dataValue(DISPLAY_SHADOW_RADIUS_ID, EntityDataSerializers.FLOAT, shadowRadius)));
        properties.shadowStrength().ifPresent(shadowStrength -> data.add(dataValue(DISPLAY_SHADOW_STRENGTH_ID, EntityDataSerializers.FLOAT, shadowStrength)));
        properties.trackingRotation().ifPresent(tracking -> data.add(dataValue(DISPLAY_BILLBOARD_ID, EntityDataSerializers.BYTE, (byte) tracking.ordinal())));
        properties.brightness().ifPresent(brightness -> data.add(dataValue(DISPLAY_BRIGHTNESS_ID, EntityDataSerializers.INT, (brightness.getBlockLight() << 4 | brightness.getSkyLight() << 20))));
        properties.glowColor().ifPresent(glow -> data.add(dataValue(DISPLAY_GLOW_ID, EntityDataSerializers.INT, glow.asRGB())));

        data.add(dataValue(DISPLAY_TRANSLATION, EntityDataSerializers.VECTOR3, Optional.of(transformation.getTranslation()).filter(v -> !v.equals(new Vector3f())).orElse(properties.translation())));
        data.add(dataValue(DISPLAY_SCALE_ID, EntityDataSerializers.VECTOR3, Optional.of(transformation.getScale()).filter(v -> !v.equals(new Vector3f(1,1,1))).orElse(properties.scale())));
        data.add(dataValue(DISPLAY_QUATERNION_LEFT, EntityDataSerializers.QUATERNION, Optional.of(transformation.getLeftRotation()).filter(q -> !q.equals(new Quaternionf())).orElse(properties.leftRotation())));
        data.add(dataValue(DISPLAY_QUATERNION_RIGHT, EntityDataSerializers.QUATERNION, Optional.of(transformation.getRightRotation()).filter(q -> !q.equals(new Quaternionf())).orElse(properties.rightRotation())));

        data.add(dataValue(ITEM_DISPLAY_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
        data.add(dataValue(ITEM_DISPLAY_TRANSFORM_ID, EntityDataSerializers.BYTE, (byte) properties.displayTransform().ordinal()));

        return data;
    }

    private <T> SynchedEntityData.DataValue<T> dataValue(int id, EntityDataSerializer<T> serializer, T value) {
        return new SynchedEntityData.DataValue<>(id, serializer, value);
    }
}
