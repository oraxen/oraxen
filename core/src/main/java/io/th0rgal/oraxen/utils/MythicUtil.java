package io.th0rgal.oraxen.utils;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.adapters.item.ItemComponentBukkitItemStack;
import io.lumine.mythic.bukkit.adapters.item.NbtBukkitItemStack;
import io.lumine.mythic.bukkit.utils.numbers.RandomDouble;
import io.lumine.mythic.core.drops.Drop;
import io.lumine.mythic.core.drops.droppables.NothingDrop;
import io.lumine.mythic.core.drops.droppables.VanillaItemDrop;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MythicUtil {

    public static Drop getOraxenDrop(String line, MythicLineConfig config, @NotNull ItemStack oraxenItem, RandomDouble amount) {
        // MythicMobs 5.7.0-SNAPSHOT changed this functionality
        // This is a workaround to support both old and new moving forward

        BukkitItemStack itemStack;
        try {
            itemStack = VersionUtil.atOrAbove("1.20.5")
                    ? new ItemComponentBukkitItemStack(oraxenItem) : new NbtBukkitItemStack(oraxenItem);
        } catch (Exception e) {
            try {
                itemStack = BukkitItemStack.class.getConstructor(String.class, MythicLineConfig.class, ItemStack.class).newInstance(line, config, oraxenItem);
            } catch (Exception e2) {
                return new NothingDrop(line, config, amount.get());
            }
        }

        return new VanillaItemDrop(line, config, itemStack, amount);
    }
}
