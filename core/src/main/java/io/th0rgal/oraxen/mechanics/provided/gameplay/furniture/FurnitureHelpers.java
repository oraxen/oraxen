package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class FurnitureHelpers {

    public static float furnitureYaw(Entity baseEntity) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return baseEntity.getLocation().getYaw();

        if (baseEntity instanceof ItemFrame itemFrame) {
            if (mechanic.hasLimitedPlacing() && mechanic.limitedPlacing().isWall() && itemFrame.getFacing().getModY() == 0)
                return baseEntity.getLocation().getYaw();
            else return rotationToYaw(itemFrame.getRotation());
        } else return baseEntity.getLocation().getYaw();
    }

    public static void furnitureYaw(Entity baseEntity, float yaw) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        if (baseEntity instanceof ItemFrame itemFrame) {
            itemFrame.setRotation(yawToRotation(yaw));
        } else baseEntity.setRotation(yaw, baseEntity.getPitch());
    }

    public static float rotationToYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    public static Rotation yawToRotation(float yaw) {
        return Rotation.values()[Math.round(yaw / 45f) & 0x7];
    }

    @Nullable
    public static ItemStack furnitureItem(Entity baseEntity) {
        return FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).map(FurnitureBaseEntity::itemStack).orElse(null);
    }

    public static void furnitureItem(Entity baseEntity, ItemStack itemStack) {
        FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).ifPresent(furnitureBase -> {
            furnitureBase.itemStack(itemStack);
        });
    }
}
