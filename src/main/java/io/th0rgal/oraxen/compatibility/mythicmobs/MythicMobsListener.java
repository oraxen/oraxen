package io.th0rgal.oraxen.compatibility.mythicmobs;

import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitItemStack;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicDropLoadEvent;
import io.lumine.xikage.mythicmobs.drops.droppables.ItemDrop;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * @author yzl210
 * @since June 15, 2020
 */
public class MythicMobsListener implements Listener {


    public MythicMobsListener() {
    }

    public void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent mdle) {
        if (mdle.getDropName().equalsIgnoreCase("oraxen")) {
            String line = mdle.getContainer().getLine();
            if (line.split(" ").length == 4 && OraxenItems.isAnItem(line.split(" ")[1])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[1]).build();
                ItemDrop itemDrop = new ItemDrop(line, mdle.getConfig(), new BukkitItemStack(od));
                mdle.register(itemDrop);
            } else if (line.split(" ").length == 3 && OraxenItems.isAnItem(line.split(" ")[2])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[2]).build();
                ItemDrop itemDrop = new ItemDrop(line, mdle.getConfig(), new BukkitItemStack(od));
                mdle.register(itemDrop);
            }
        }
    }
}