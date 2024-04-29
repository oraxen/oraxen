package io.th0rgal.oraxen.nms.v1_19_R1.furniture;

import io.netty.buffer.Unpooled;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureBaseEntity;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureHelpers;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureType;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import io.th0rgal.oraxen.utils.BlockHelpers;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.UUID;

public class FurnitureBasePacket {

    private final int ITEM_FRAME_ITEM_ID = 8;
    private final int ITEM_FRAME_ROTATION_ID = 9;

    public final FurnitureType type;
    public final Integer entityId;
    private final ClientboundAddEntityPacket entityPacket;
    private final ClientboundSetEntityDataPacket metadataPacket;

    public FurnitureBasePacket(FurnitureBaseEntity furnitureBase, Entity baseEntity, FurnitureType type, Player player) {
        Location baseLoc = BlockHelpers.toCenterBlockLocation(baseEntity.getLocation());
        FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());

        this.type = type;
        this.entityId = baseEntity.getEntityId();

        EntityType<?> entityType = type == FurnitureType.ITEM_FRAME ? EntityType.ITEM_FRAME : EntityType.GLOW_ITEM_FRAME;
        this.entityPacket = new ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(),
                baseLoc.getX(), baseLoc.getY(), baseLoc.getZ(), baseLoc.getPitch(), baseLoc.getYaw(),
                entityType, entityData(furnitureBase), Vec3.ZERO, 0.0
        );

        friendlyByteBuf.writeVarInt(entityId);
        SynchedEntityData.pack(Arrays.asList(
                new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ITEM_ID, EntityDataSerializers.ITEM_STACK), CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))),
                new SynchedEntityData.DataItem<>(new EntityDataAccessor<>(ITEM_FRAME_ROTATION_ID, EntityDataSerializers.INT), FurnitureHelpers.yawToRotation(baseEntity.getLocation().getYaw()).ordinal())
        ), friendlyByteBuf);

        this.metadataPacket = new ClientboundSetEntityDataPacket(friendlyByteBuf);
    }

    public ClientboundAddEntityPacket entityPacket() {
        return this.entityPacket;
    }

    public ClientboundSetEntityDataPacket metadataPacket() {
        return this.metadataPacket;
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
}
