package io.th0rgal.oraxen.nms.v1_20_R4.furniture;

import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.*;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.text.Component;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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

    public final FurnitureType type;
    public final Integer entityId;
    public final UUID uuid;
    private final ClientboundAddEntityPacket entityPacket;
    private final ClientboundSetEntityDataPacket metadataPacket;
    private final ClientboundBundlePacket bundlePacket;

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, Entity baseEntity, FurnitureType type, Player player) {
        this.type = type;
        this.entityId = furnitureBase.entityId(type);
        this.uuid = furnitureBase.uuid(type);
        Location baseLoc = baseEntity.getLocation();
        double x = baseLoc.x(), y = baseLoc.y(), z = baseLoc.z();
        Logs.debug(baseLoc.getYaw(), correctedPlayerYaw(baseLoc.getYaw(), player));
        //Logs.debug(ChatColor.GREEN.toString() + baseLoc.getYaw() + ChatColor.WHITE + " | " + ChatColor.RED + correctedPlayerYaw(baseLoc.getYaw(), player));
        float pitch = correctedPlayerPitch(furnitureBase, baseLoc.getPitch(), player), yaw = correctedPlayerYaw(baseLoc.getYaw(), player);
        EntityType<?> entityType = type == FurnitureType.DISPLAY_ENTITY ? EntityType.ITEM_DISPLAY : type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;

        this.entityPacket = new ClientboundAddEntityPacket(
                entityId, uuid, x, y, z, pitch, yaw,
                entityType, entityData(furnitureBase), Vec3.ZERO, 0.0
        );

        this.metadataPacket = new ClientboundSetEntityDataPacket(entityId, dataValues(furnitureBase, (ItemDisplay) baseEntity));
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

    private float correctedPlayerPitch(FurnitureBaseEntity furnitureBase, float initialPitch, Player player) {
        if (type != FurnitureType.DISPLAY_ENTITY) return initialPitch;
        FurnitureMechanic mechanic = furnitureBase.mechanic();
        LimitedPlacing lp = mechanic.limitedPlacing();
        boolean isFixed = mechanic.displayEntityProperties().isFixedTransform();
        return VersionUtil.atOrAbove(player, 763) ? mechanic.hasLimitedPlacing() && isFixed ? lp.isFloor() ? -90 : lp.isRoof() ? 90 : initialPitch : initialPitch : initialPitch;
    }

    private float correctedPlayerYaw(float initialYaw, Player player) {
        if (type != FurnitureType.DISPLAY_ENTITY) return initialYaw;
        return VersionUtil.below(player, 763) ? initialYaw - 180 : initialYaw;

    }

    private int entityData(FurnitureBaseEntity furnitureBase) {
        // https://wiki.vg/Object_Data#Item_Frame
        if (type == FurnitureType.ITEM_FRAME || type == FurnitureType.GLOW_ITEM_FRAME) {
            LimitedPlacing limitedPlacing = furnitureBase.mechanic().limitedPlacing();
            if (!furnitureBase.mechanic().hasLimitedPlacing()) return Direction.UP.ordinal();
            if (limitedPlacing.isFloor() && !limitedPlacing.isWall() && furnitureBase.baseEntity().getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid())
                return Direction.UP.ordinal();
            if (limitedPlacing.isRoof()/* && facing == BlockFace.DOWN*/)
                return Direction.DOWN.ordinal();
        }

        return 0;
    }

    private List<SynchedEntityData.DataValue<?>> dataValues(FurnitureBaseEntity furnitureBase, ItemDisplay baseEntity) {
        List<SynchedEntityData.DataValue<?>> data = new ArrayList<>();
        LimitedPlacing limitedPlacing = furnitureBase.mechanic().limitedPlacing();

        data.add(dataValue(0, EntityDataSerializers.BYTE, (byte) 0x20));
        data.add(dataValue(2, EntityDataSerializers.OPTIONAL_COMPONENT, Optional.of(PaperAdventure.asVanilla(Component.empty()))));
        data.add(dataValue(3, EntityDataSerializers.BOOLEAN, false));

        if (type == FurnitureType.DISPLAY_ENTITY) {
            DisplayEntityProperties displayProp = furnitureBase.mechanic().displayEntityProperties();

            data.add(dataValue(9, EntityDataSerializers.INT, 0));
            data.add(dataValue(DISPLAY_WIDTH_ID, EntityDataSerializers.FLOAT, displayProp.displayWidth()));
            data.add(dataValue(DISPLAY_HEIGHT_ID, EntityDataSerializers.FLOAT, displayProp.displayHeight()));

            Optional.ofNullable(displayProp.viewRange()).ifPresent(viewRange -> data.add(dataValue(DISPLAY_VIEW_RANGE_ID, EntityDataSerializers.FLOAT, (float) viewRange)));
            Optional.ofNullable(displayProp.shadowRadius()).ifPresent(shadowRadius -> data.add(dataValue(DISPLAY_SHADOW_RADIUS_ID, EntityDataSerializers.FLOAT, shadowRadius)));
            Optional.ofNullable(displayProp.shadowStrength()).ifPresent(shadowStrength -> data.add(dataValue(DISPLAY_SHADOW_STRENGTH_ID, EntityDataSerializers.FLOAT, shadowStrength)));
            Optional.ofNullable(displayProp.trackingRotation()).ifPresent(tracking -> data.add(dataValue(DISPLAY_BILLBOARD_ID, EntityDataSerializers.BYTE, (byte) tracking.ordinal())));
            Optional.ofNullable(displayProp.brightness()).ifPresent(brightness -> data.add(dataValue(DISPLAY_BRIGHTNESS_ID, EntityDataSerializers.INT, (brightness.getBlockLight() << 4 | brightness.getSkyLight() << 20))));
            Optional.ofNullable(displayProp.glowColor()).ifPresent(glow -> data.add(dataValue(DISPLAY_GLOW_ID, EntityDataSerializers.INT, glow.asRGB())));

            data.add(dataValue(DISPLAY_TRANSLATION, EntityDataSerializers.VECTOR3, baseEntity.getTransformation().getTranslation()));
            data.add(dataValue(DISPLAY_SCALE_ID, EntityDataSerializers.VECTOR3, Optional.ofNullable(displayProp.scale()).orElse(baseEntity.getTransformation().getScale())));
            data.add(dataValue(DISPLAY_QUATERNION_LEFT, EntityDataSerializers.QUATERNION, baseEntity.getTransformation().getLeftRotation()));
            data.add(dataValue(DISPLAY_QUATERNION_RIGHT, EntityDataSerializers.QUATERNION, baseEntity.getTransformation().getRightRotation()));

            data.add(dataValue(ITEM_DISPLAY_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
            data.add(dataValue(ITEM_DISPLAY_TRANSFORM_ID, EntityDataSerializers.BYTE, (byte) displayProp.displayTransform().ordinal()));
        } else {
            data.add(dataValue(ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK, CraftItemStack.asNMSCopy(furnitureBase.itemStack())));
            data.add(dataValue(ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT, limitedPlacing != null && limitedPlacing.isWall() ? Rotation.NONE.ordinal() : FurnitureHelpers.yawToRotation(furnitureBase.baseEntity().getYaw()).ordinal()));
        }

        return data;
    }

    private <T> SynchedEntityData.DataValue<T> dataValue(int id, EntityDataSerializer<T> serializer, T value) {
        return new SynchedEntityData.DataValue<>(id, serializer, value);
    }
}
