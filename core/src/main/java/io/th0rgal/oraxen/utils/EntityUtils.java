package io.th0rgal.oraxen.utils;

import io.th0rgal.oraxen.OraxenPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings({"unchecked", "unused", "deprecation"})
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

    public static <T extends Entity> Collection<T> getEntitiesByClass(Chunk chunk, Class<T> clazz) {
        Collection<T> list = new ArrayList<T>();

        OraxenPlugin.get().getScheduler().runRegionTask(chunk, () -> {
            for (Entity bukkitEntity : chunk.getEntities()) {
                if (bukkitEntity == null) {
                    continue;
                }

                Class<?> bukkitClass = bukkitEntity.getClass();

                OraxenPlugin.get().getScheduler().runEntityTask(bukkitEntity, () -> {
                    if (clazz.isAssignableFrom(bukkitClass) && (!bukkitEntity.isValid())) {
                        list.add((T) bukkitEntity);
                    }
                }, null);
            }
        });

        return list;
    }

    public static Collection<Entity> getEntitiesByClasses(Chunk chunk, Class<?>... classes) {
        Collection<Entity> list = new ArrayList<Entity>();
        OraxenPlugin.get().getScheduler().runRegionTask(chunk, () -> {
            for (Entity bukkitEntity : chunk.getEntities()) {
                if (bukkitEntity == null) continue;
                Class<?> bukkitClass = bukkitEntity.getClass();
                for (Class<?> clazz : classes) {
                    if (clazz.isAssignableFrom(bukkitClass)) {
                        OraxenPlugin.get().getScheduler().runEntityTask(bukkitEntity, () -> {
                            if (bukkitEntity.isValid()) {
                                list.add(bukkitEntity);
                            }
                        }, null);
                        break;
                    }
                }
            }
        });
        return list;
    }

}

