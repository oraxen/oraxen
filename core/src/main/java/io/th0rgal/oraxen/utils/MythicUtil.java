package io.th0rgal.oraxen.utils;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.core.drops.Drop;
import io.th0rgal.oraxen.api.OraxenItems;

import java.lang.reflect.InvocationTargetException;

public class MythicUtil {

    public static Drop getOraxenDrop(String line, MythicLineConfig config, String itemId) {
        BukkitItemStack itemStack = new BukkitItemStack(OraxenItems.getItemById(itemId).build());

        // MythicMobs 5.6.0 SNAPSHOT changed this functionality
        // This is a workaround to support both old and new moving forward
        Drop drop;
        try {
            drop = (Drop) Class.forName("io.lumine.mythic.core.drops.droppables.VanillaItemDrop").getConstructor(String.class, MythicLineConfig.class, BukkitItemStack.class).newInstance(line, config, itemStack);
        } catch (NoClassDefFoundError e) {
            drop = new io.lumine.mythic.core.drops.droppables.ItemDrop(line, config, itemStack);
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | NoSuchMethodException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return drop;
    }
}
