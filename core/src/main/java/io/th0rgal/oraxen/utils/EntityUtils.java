package io.th0rgal.oraxen.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;

@SuppressWarnings({"unused", "deprecation"})
public class EntityUtils {

    public static boolean isUnderWater(Entity entity) {
        return VersionUtil.isPaperServer() ? entity.isUnderWater() : entity.isInWater();
    }

    public static boolean isFixed(ItemDisplay itemDisplay) {
        return itemDisplay.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.FIXED;
    }

    public static boolean isNone(ItemDisplay itemDisplay) {
        return itemDisplay.getItemDisplayTransform() == ItemDisplay.ItemDisplayTransform.NONE;
    }

    public static void customName(Entity entity, Component customName) {
        if (VersionUtil.isPaperServer()) entity.customName(customName);
        else entity.setCustomName(AdventureUtils.LEGACY_SERIALIZER.serialize(customName));
    }

}

