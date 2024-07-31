package io.th0rgal.oraxen.nms.v1_20_R3.furniture;

import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.DisplayEntityProperties;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;

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

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, Entity baseEntity, Player player) {
        this.entityId = furnitureBase.baseId();
        this.uuid = furnitureBase.baseUUID();
        Location baseLoc = baseEntity.getLocation();

        this.entityPacket = new ClientboundAddEntityPacket(
                entityId, uuid, baseLoc.x(), baseLoc.y(), baseLoc.z(),
                pitch(furnitureBase, baseLoc.getPitch(), player), yaw(furnitureBase, baseLoc.getYaw(), player),
                EntityType.ITEM_DISPLAY, 0, Vec3.ZERO, 0.0
        );

        this.metadataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues(furnitureBase, (ItemDisplay) baseEntity));
    }

    public ClientboundAddEntityPacket entityPacket() {
        return this.entityPacket;
    }

    public ClientboundSetEntityDataPacket metadataPacket() {
        return this.metadataPacket;
    }

    public int entityId() {
        return this.entityId;
    }

    public UUID uuid() {
        return this.uuid;
    }

    private float pitch(FurnitureBaseEntity furnitureBase, float initialPitch, Player player) {
        LimitedPlacing lp = furnitureBase.mechanic().limitedPlacing();
        boolean isFixed = furnitureBase.mechanic().displayEntityProperties().isFixedTransform();

        if (VersionUtil.atOrAbove(player, 763)) {
            return lp != null && isFixed ? lp.isFloor() ? -90 : lp.isRoof() ? 90 : initialPitch : initialPitch;
        } else return lp != null && isFixed ? lp.isFloor() ? 90 : lp.isRoof() ? -90 : initialPitch : initialPitch;
    }

    private float yaw(FurnitureBaseEntity furnitureBase, float initialYaw, Player player) {
        LimitedPlacing limitedPlacing = furnitureBase.mechanic().limitedPlacing();
        boolean isFixed = furnitureBase.mechanic().displayEntityProperties().isFixedTransform();

        if (VersionUtil.atOrAbove(player, 763) && (limitedPlacing == null || !limitedPlacing.isRoof() || !isFixed))
            return initialYaw;
        else return initialYaw - 180;

    }

    private List<SynchedEntityData.DataValue<?>> dataValues(FurnitureBaseEntity furnitureBase, ItemDisplay baseEntity) {
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();

        data.add(new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, (byte) 0x20));
        data.add(new SynchedEntityData.DataValue<>(2, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(PaperAdventure.asVanilla(Component.empty()))));
        data.add(new SynchedEntityData.DataValue<>(3, EntityDataSerializers.BOOLEAN, false));

        DisplayEntityProperties displayProp = furnitureBase.mechanic().displayEntityProperties();

        data.add(new SynchedEntityData.DataValue<>(9, EntityDataSerializers.INT, 0));
        data.add(new SynchedEntityData.DataValue<>(DISPLAY_WIDTH_ID, EntityDataSerializers.FLOAT, displayProp.displayWidth()));
        data.add(new SynchedEntityData.DataValue<>(DISPLAY_HEIGHT_ID, EntityDataSerializers.FLOAT, displayProp.displayHeight()));

        Optional.ofNullable(displayProp.viewRange()).ifPresent(viewRange -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_VIEW_RANGE_ID, EntityDataSerializers.FLOAT, (float) viewRange)));
        Optional.ofNullable(displayProp.shadowRadius()).ifPresent(shadowRadius -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_SHADOW_RADIUS_ID, EntityDataSerializers.FLOAT, shadowRadius)));
        Optional.ofNullable(displayProp.shadowStrength()).ifPresent(shadowStrength -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_SHADOW_STRENGTH_ID, EntityDataSerializers.FLOAT, shadowStrength)));
        Optional.ofNullable(displayProp.trackingRotation()).ifPresent(tracking -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_BILLBOARD_ID, EntityDataSerializers.BYTE, (byte) tracking.ordinal())));
        Optional.ofNullable(displayProp.brightness()).ifPresent(brightness -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_BRIGHTNESS_ID, EntityDataSerializers.INT, (brightness.getBlockLight() << 4 | brightness.getSkyLight() << 20))));
        Optional.ofNullable(displayProp.glowColor()).ifPresent(glow -> data.add(new SynchedEntityData.DataValue<>(DISPLAY_GLOW_ID, EntityDataSerializers.INT, glow.asRGB())));

        data.add(new SynchedEntityData.DataValue<>(DISPLAY_TRANSLATION, EntityDataSerializers.VECTOR3, baseEntity.getTransformation().getTranslation()));
        data.add(new SynchedEntityData.DataValue<>(DISPLAY_SCALE_ID, EntityDataSerializers.VECTOR3, Optional.ofNullable(displayProp.scale()).orElse(baseEntity.getTransformation().getScale())));
        data.add(new SynchedEntityData.DataValue<>(DISPLAY_QUATERNION_LEFT, EntityDataSerializers.QUATERNION, baseEntity.getTransformation().getLeftRotation()));
        data.add(new SynchedEntityData.DataValue<>(DISPLAY_QUATERNION_RIGHT, EntityDataSerializers.QUATERNION, baseEntity.getTransformation().getRightRotation()));

        data.add(new SynchedEntityData.DataValue<>(ITEM_DISPLAY_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
        data.add(new SynchedEntityData.DataValue<>(ITEM_DISPLAY_TRANSFORM_ID, EntityDataSerializers.BYTE, (byte) displayProp.displayTransform().ordinal()));

        return data;
    }

    public ClientboundBundlePacket bundlePackets() {
        return new ClientboundBundlePacket(List.of(entityPacket, metadataPacket));
    }
}
