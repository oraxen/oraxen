package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.mechanics.provided.gameplay.limitedplacing.LimitedPlacing;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class FurnitureHelpers {

    public static float correctedYaw(FurnitureMechanic mechanic, float yaw) {
        boolean isFixed = mechanic.displayEntityProperties().isFixedTransform();

        if (mechanic.hasLimitedPlacing() && !mechanic.limitedPlacing().isRoof()) return yaw;
        else if (isFixed) return yaw - 180;
        else return yaw;
    }

    public static float correctedPitch(FurnitureMechanic mechanic, float initialPitch) {
        LimitedPlacing lp = mechanic.limitedPlacing();
        boolean isFixed = mechanic.displayEntityProperties().isFixedTransform();
        return mechanic.hasLimitedPlacing() && isFixed ? lp.isFloor() ? -90 : lp.isRoof() ? 90 : initialPitch : initialPitch;
    }

    public static void furnitureYaw(ItemDisplay baseEntity, float yaw) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        baseEntity.setRotation(yaw, baseEntity.getLocation().getPitch());
    }

    public static float rotationToYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    public static Rotation yawToRotation(float yaw) {
        return Rotation.values()[Math.round(yaw / 45f) & 0x7];
    }

    @Nullable
    public static ItemStack furnitureItem(ItemDisplay baseEntity) {
        return FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).map(FurnitureBaseEntity::itemStack).orElse(null);
    }

    public static void furnitureItem(Entity baseEntity, ItemStack itemStack) {
        FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).ifPresent(furnitureBase -> furnitureBase.itemStack(itemStack));
    }
}
