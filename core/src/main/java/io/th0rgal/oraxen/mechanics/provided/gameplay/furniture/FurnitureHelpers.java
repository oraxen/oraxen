package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenFurniture;
import org.bukkit.Rotation;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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

    public static ItemStack furnitureItem(Entity entity) {
        return switch (entity.getType()) {
            case ARMOR_STAND -> ((ArmorStand) entity).getEquipment().getHelmet();
            case ITEM_DISPLAY -> OraxenPlugin.supportsDisplayEntities ? ((ItemDisplay) entity).getItemStack() : null;
            default -> ((ItemFrame) entity).getItem();
        };
    }

    public static void furnitureItem(Entity entity, ItemStack item) {
        if (entity instanceof ItemFrame itemFrame)
            itemFrame.setItem(item, false);
        else if (entity instanceof ArmorStand armorStand)
            armorStand.setItem(EquipmentSlot.HEAD, item);
        else if (OraxenPlugin.supportsDisplayEntities && entity instanceof ItemDisplay itemDisplay)
            itemDisplay.setItemStack(item);
    }
}
