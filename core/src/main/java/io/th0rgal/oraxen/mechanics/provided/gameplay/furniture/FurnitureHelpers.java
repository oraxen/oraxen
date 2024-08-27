package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Color;
import org.bukkit.Rotation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public class FurnitureHelpers {

    public static float correctedYaw(@NotNull FurnitureMechanic mechanic, float yaw) {
        boolean isFixed = mechanic.displayEntityProperties().isFixedTransform();

        if (mechanic.hasLimitedPlacing() && !mechanic.limitedPlacing().isRoof()) return yaw;
        else if (isFixed) return yaw - 180;
        else return yaw;
    }

    public static void furnitureYaw(@NotNull ItemDisplay baseEntity, float yaw) {
        FurnitureMechanic mechanic = OraxenFurniture.getFurnitureMechanic(baseEntity);
        if (mechanic == null) return;

        baseEntity.setRotation(yaw, baseEntity.getLocation().getPitch());
    }

    public static float rotationToYaw(@NotNull Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }

    @Nullable
    public static ItemStack furnitureItem(@NotNull ItemDisplay baseEntity) {
        return FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).map(FurnitureBaseEntity::itemStack).orElse(null);
    }

    public static void furnitureItem(@NotNull Entity baseEntity, @NotNull ItemStack itemStack) {
        FurnitureFactory.instance.packetManager().furnitureBaseFromBaseEntity(baseEntity).ifPresent(furnitureBase -> furnitureBase.itemStack(itemStack));
    }

    @Nullable
    public static Color furnitureDye(@NotNull ItemDisplay baseEntity) {
        return Optional.ofNullable(baseEntity.getPersistentDataContainer().get(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER)).map(Color::fromRGB).orElse(null);
    }

    public static void furnitureDye(@NotNull ItemDisplay baseEntity, @Nullable Color dyeColor) {
        if (dyeColor == null) baseEntity.getPersistentDataContainer().remove(FurnitureMechanic.FURNITURE_DYE_KEY);
        else baseEntity.getPersistentDataContainer().set(FurnitureMechanic.FURNITURE_DYE_KEY, PersistentDataType.INTEGER, dyeColor.asRGB());
    }
}
