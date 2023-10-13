package io.th0rgal.oraxen.compatibilities.provided.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.core.drops.droppables.ItemDrop;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class MythicMobsCompatibility extends CompatibilityProvider<MythicBukkit> {

    @EventHandler
    public void onMythicDropLoadEvent(MythicDropLoadEvent event) {
        if (event.getDropName().equalsIgnoreCase("oraxen")) {
            String line = event.getContainer().getLine();
            if (line.split(" ").length == 4 && OraxenItems.exists(line.split(" ")[1])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[1]).build();
                ItemDrop itemDrop = new ItemDrop(line, event.getConfig(), new BukkitItemStack(od));
                event.register(itemDrop);
            } else if (line.split(" ").length == 3 && OraxenItems.exists(line.split(" ")[2])) {
                ItemStack od = OraxenItems.getItemById(line.split(" ")[2]).build();
                ItemDrop itemDrop = new ItemDrop(line, event.getConfig(), new BukkitItemStack(od));
                event.register(itemDrop);
            }
        }
    }
}
